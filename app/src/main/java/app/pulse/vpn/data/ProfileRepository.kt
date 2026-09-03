package app.pulse.vpn.data

import android.content.Context
import app.pulse.vpn.core.ProfileManager
import app.pulse.vpn.core.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID

class ProfileRepository(
    private val context: Context,
    importer: SubscriptionImporter? = null,
) {
    private val importer = importer ?: SubscriptionImporter(SubscriptionIdentityProvider(context)::current)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val root = File(context.filesDir, "profiles").apply { mkdirs() }
    private val preferences = context.getSharedPreferences("pulse_profiles", Context.MODE_PRIVATE)

    suspend fun importProfile(input: String): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val imported = importer.import(input)
            val id = UUID.randomUUID().toString()
            val directory = File(root, id).apply { mkdirs() }
            val configFile = File(directory, "using_config.json")
            configFile.writeText(imported.config)
            val info = imported.userInfo
            val profile = VpnProfile(id, imported.name, imported.sourceUrl, System.currentTimeMillis(), info.upload, info.download, info.total, info.expire)
            File(directory, "profile.json").writeText(json.encodeToString(profile))
            select(profile)
            ImportResult.Success(profile)
        }.getOrElse { ImportResult.Error(it.message ?: "Не удалось импортировать профиль") }
    }

    suspend fun update(profile: VpnProfile): ImportResult {
        val url = profile.sourceUrl ?: return ImportResult.Error("У локального профиля нет ссылки обновления")
        return withContext(Dispatchers.IO) {
            runCatching {
                val imported = importer.import(url)
                val directory = File(root, profile.id)
                File(directory, "using_config.json").writeText(imported.config)
                val updated = profile.copy(
                    name = imported.name, updatedAt = System.currentTimeMillis(),
                    uploadBytes = imported.userInfo.upload, downloadBytes = imported.userInfo.download,
                    totalBytes = imported.userInfo.total, expireAt = imported.userInfo.expire,
                )
                File(directory, "profile.json").writeText(json.encodeToString(updated))
                if (selectedId() == updated.id) select(updated)
                ImportResult.Success(updated)
            }.getOrElse { ImportResult.Error(it.message ?: "Ошибка обновления") }
        }
    }

    suspend fun profiles(): List<VpnProfile> = withContext(Dispatchers.IO) {
        root.listFiles().orEmpty().mapNotNull { directory ->
            runCatching { json.decodeFromString<VpnProfile>(File(directory, "profile.json").readText()) }.getOrNull()
        }.sortedByDescending(VpnProfile::updatedAt)
    }

    fun selectedId(): String? = preferences.getString("selected_id", null)

    fun select(profile: VpnProfile) {
        val config = File(File(root, profile.id), "using_config.json")
        ProfileManager.select(profile.name, config)
        preferences.edit().putString("selected_id", profile.id).apply()
    }

    suspend fun delete(profile: VpnProfile) = withContext(Dispatchers.IO) {
        File(root, profile.id).deleteRecursively()
        if (selectedId() == profile.id) {
            preferences.edit().remove("selected_id").apply()
            ProfileManager.clear()
        }
    }

    suspend fun servers(profile: VpnProfile?): List<VpnServer> = withContext(Dispatchers.IO) {
        if (profile == null) return@withContext emptyList()
        val config = runCatching { json.parseToJsonElement(File(File(root, profile.id), "using_config.json").readText()).jsonObject }.getOrNull()
            ?: return@withContext emptyList()
        val selected = preferences.getString("selected_server_${profile.id}", null)
        config["outbounds"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val type = item.value("type")
            if (type in setOf("direct", "block", "dns", "selector", "urltest")) return@mapNotNull null
            val tag = item.value("tag").ifBlank { type.uppercase() }
            val address = item.value("server").ifBlank { null }
            if (
                tag.startsWith("❌") ||
                address?.startsWith("error.", ignoreCase = true) == true ||
                tag.contains("отсутствуют данные", ignoreCase = true)
            ) return@mapNotNull null
            VpnServer(tag, type, address, item["server_port"]?.jsonPrimitive?.intOrNull, tag == selected)
        }
    }

    suspend fun selectServer(profile: VpnProfile, server: VpnServer) = withContext(Dispatchers.IO) {
        preferences.edit().putString("selected_server_${profile.id}", server.tag).apply()
        val file = File(File(root, profile.id), "using_config.json")
        val rootObject = json.parseToJsonElement(file.readText()).jsonObject.toMutableMap()
        val outbounds = rootObject["outbounds"]?.jsonArray?.map { element ->
            val item = (element as? JsonObject)?.toMutableMap() ?: return@map element
            if (item["type"]?.jsonPrimitive?.contentOrNull in setOf("selector", "urltest")) item["default"] = JsonPrimitive(server.tag)
            JsonObject(item)
        }.orEmpty()
        rootObject["outbounds"] = JsonArray(outbounds)
        file.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(rootObject)))
        if (selectedId() == profile.id) ProfileManager.select(profile.name, file)
    }

    suspend fun applyRoutingSettings(profile: VpnProfile) = withContext(Dispatchers.IO) {
        val file = File(File(root, profile.id), "using_config.json")
        val rootObject = json.parseToJsonElement(file.readText()).jsonObject.toMutableMap()
        val route = ((rootObject["route"] as? JsonObject)?.toMutableMap() ?: mutableMapOf())
        val selected = preferences.getString("selected_server_${profile.id}", null)
        when (SettingsManager.routingMode) {
            "global" -> route["final"] = JsonPrimitive(selected ?: "Proxy")
            "direct" -> route["final"] = JsonPrimitive("direct")
            else -> route["final"] = JsonPrimitive("Proxy")
        }
        rootObject["route"] = JsonObject(route)
        if (SettingsManager.dnsMode != "local") {
            val endpoint = if (SettingsManager.dnsMode == "google") "https://dns.google/dns-query" else "https://1.1.1.1/dns-query"
            rootObject["dns"] = JsonObject(mapOf(
                "servers" to JsonArray(listOf(JsonObject(mapOf(
                    "type" to JsonPrimitive("https"),
                    "tag" to JsonPrimitive("secure-dns"),
                    "server" to JsonPrimitive(endpoint),
                )))),
            ))
        }
        file.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(rootObject)))
        ProfileManager.select(profile.name, file)
    }

    fun installedApps(): List<AppEntry> = context.packageManager.getInstalledApplications(0)
        .asSequence().filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .map { AppEntry(it.packageName, context.packageManager.getApplicationLabel(it).toString()) }
        .sortedBy { it.label.lowercase() }.toList()

    private fun JsonObject.value(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    data class AppEntry(val packageName: String, val label: String)
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
