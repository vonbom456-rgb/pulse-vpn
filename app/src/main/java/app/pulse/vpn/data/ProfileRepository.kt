package app.pulse.vpn.data

import android.content.Context
import app.pulse.vpn.core.ProfileManager
import app.pulse.vpn.core.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.decodeFromString
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
            val info = imported.userInfo
            val profile = VpnProfile(
                id = id,
                name = imported.name,
                sourceUrl = imported.sourceUrl,
                updatedAt = System.currentTimeMillis(),
                uploadBytes = info.upload,
                downloadBytes = info.download,
                totalBytes = info.total,
                expireAt = info.expire,
                themeHint = imported.themeHint,
                providerDescription = imported.providerDescription,
                providerTelegram = imported.providerTelegram,
                providerWebsite = imported.providerWebsite,
            )
            // Keep the provider response untouched as the source. Runtime settings are
            // applied to using_config.json and can therefore be changed without losing
            // the provider's routing or DNS rules on the next refresh.
            File(directory, SOURCE_CONFIG).writeText(imported.config)
            File(directory, "profile.json").writeText(json.encodeToString(profile))
            writeEffectiveConfig(profile)
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
                val updated = profile.copy(
                    name = imported.name, updatedAt = System.currentTimeMillis(),
                    uploadBytes = imported.userInfo.upload, downloadBytes = imported.userInfo.download,
                    totalBytes = imported.userInfo.total, expireAt = imported.userInfo.expire,
                    themeHint = imported.themeHint,
                    providerDescription = imported.providerDescription,
                    providerTelegram = imported.providerTelegram,
                    providerWebsite = imported.providerWebsite,
                )
                File(directory, SOURCE_CONFIG).writeText(imported.config)
                File(directory, "profile.json").writeText(json.encodeToString(updated))
                writeEffectiveConfig(updated)
                if (selectedId() == updated.id) select(updated)
                ImportResult.Success(updated)
            }.getOrElse { ImportResult.Error(it.message ?: "Ошибка обновления") }
        }
    }

    suspend fun profiles(): List<VpnProfile> = withContext(Dispatchers.IO) {
        root.listFiles().orEmpty().mapNotNull { directory ->
            runCatching {
                val file = File(directory, "profile.json")
                val loaded = json.decodeFromString<VpnProfile>(file.readText())
                val fixed = loaded.copy(expireAt = normalizeEpoch(loaded.expireAt))
                if (fixed.expireAt != loaded.expireAt) file.writeText(json.encodeToString(fixed))
                fixed
            }.getOrNull()
        }.sortedByDescending(VpnProfile::updatedAt)
    }

    fun selectedId(): String? = preferences.getString("selected_id", null)

    fun pingHistory(profile: VpnProfile): Map<String, List<Int>> = runCatching {
        val raw = preferences.getString(pingHistoryKey(profile), null) ?: return emptyMap()
        json.decodeFromString<Map<String, List<Int>>>(raw).mapValues { (_, values) -> values.takeLast(MAX_PING_SAMPLES) }
    }.getOrDefault(emptyMap())

    fun savePingHistory(profile: VpnProfile, history: Map<String, List<Int>>) {
        val stable: Map<String, List<Int>> = history
            .filterKeys(String::isNotBlank)
            .mapValues { (_, values) -> values.takeLast(MAX_PING_SAMPLES) }
        preferences.edit().putString(pingHistoryKey(profile), json.encodeToString(stable)).apply()
    }

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
        preferences.edit().remove(pingHistoryKey(profile)).apply()
    }

    suspend fun servers(profile: VpnProfile?): List<VpnServer> = withContext(Dispatchers.IO) {
        if (profile == null) return@withContext emptyList()
        val config = runCatching { json.parseToJsonElement(File(File(root, profile.id), "using_config.json").readText()).jsonObject }.getOrNull()
            ?: return@withContext emptyList()
        val selected = preferences.getString(selectedServerKey(profile), null)
        val routes = config["outbounds"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val type = item.value("type")
            if (type in setOf("direct", "block", "dns", "selector", "urltest")) return@mapNotNull null
            val tag = item.value("tag").ifBlank { type.uppercase() }
            val address = item.value("server").ifBlank { null }
            // Provider INFO outbound is metadata/contact, never a VPN route.
            if (tag.contains("info", ignoreCase = true) || address?.contains("info.", ignoreCase = true) == true) return@mapNotNull null
            if (
                tag.startsWith("❌") ||
                address?.startsWith("error.", ignoreCase = true) == true ||
                tag.contains("отсутствуют данные", ignoreCase = true)
            ) return@mapNotNull null
            VpnServer(tag, type, address, item["server_port"]?.jsonPrimitive?.intOrNull, false)
        }
        // Older builds could persist the provider's INFO outbound as the selected route.
        // Repair that state on read so it can never become a visible route or runtime default.
        val configuredDefault = config["outbounds"]?.jsonArray.orEmpty()
            .filterIsInstance<JsonObject>()
            .firstOrNull { it.value("type") == "selector" && it.value("tag").equals("Proxy", ignoreCase = true) }
            ?.get("default")?.jsonPrimitive?.contentOrNull
        val preferred = selected?.takeIf { value -> routes.any { it.tag == value } }
            ?: configuredDefault?.takeIf { value -> routes.any { it.tag == value } }
            ?: routes.firstOrNull()?.tag
        if (preferred != null && preferred != selected) {
            preferences.edit().putString(selectedServerKey(profile), preferred).apply()
            runCatching {
                val file = runtimeConfigFile(profile)
                val rootObject = json.parseToJsonElement(file.readText()).jsonObject.toMutableMap()
                rootObject["outbounds"] = JsonArray(rootObject["outbounds"]?.jsonArray.orEmpty().map { element ->
                    val item = (element as? JsonObject)?.toMutableMap() ?: return@map element
                    if (item["type"]?.jsonPrimitive?.contentOrNull == "selector") item["default"] = JsonPrimitive(preferred)
                    JsonObject(item)
                })
                file.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(rootObject)))
                if (selectedId() == profile.id) ProfileManager.select(profile.name, file)
            }
        }
        routes.map { it.copy(selected = it.tag == preferred) }
    }

    suspend fun selectServer(profile: VpnProfile, server: VpnServer) = withContext(Dispatchers.IO) {
        if (server.isInfoMetadata()) return@withContext
        val file = runtimeConfigFile(profile)
        val config = runCatching { json.parseToJsonElement(file.readText()).jsonObject }.getOrNull() ?: return@withContext
        if (config["outbounds"]?.jsonArray.orEmpty().none { (it as? JsonObject)?.value("tag") == server.tag }) return@withContext
        preferences.edit().putString(selectedServerKey(profile), server.tag).apply()
        writeEffectiveConfig(profile)
    }

    suspend fun applyRoutingSettings(profile: VpnProfile) = withContext(Dispatchers.IO) {
        writeEffectiveConfig(profile)
    }

    fun installedApps(): List<AppEntry> = context.packageManager.getInstalledApplications(0)
        .asSequence().filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .map { AppEntry(it.packageName, context.packageManager.getApplicationLabel(it).toString()) }
        .sortedBy { it.label.lowercase() }.toList()

    private fun JsonObject.value(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonObject.isProviderInfo(): Boolean = value("tag").contains("info", ignoreCase = true) || value("server").contains("info.", ignoreCase = true)
    private fun JsonObject.isProviderError(): Boolean = value("tag").startsWith("❌") || value("server").startsWith("error.", ignoreCase = true)
    private fun JsonObject.isRouteServer(): Boolean = value("tag").isNotBlank() &&
        value("type") !in setOf("direct", "block", "dns", "selector", "urltest") &&
        !isProviderInfo() && !isProviderError()

    private fun writeEffectiveConfig(profile: VpnProfile) {
        val base = readBaseConfig(profile) ?: return
        val sourceItems = base["outbounds"]?.jsonArray.orEmpty().filterIsInstance<JsonObject>()
        val routeTags = sourceItems.filter { it.isRouteServer() }.mapNotNull { it.value("tag").takeIf(String::isNotBlank) }
        val validTargets = sourceItems
            .filterNot { it.isProviderError() }
            .filter { it.value("type") != "dns" && !it.isProviderInfo() }
            .mapNotNull { it.value("tag").takeIf(String::isNotBlank) }
            .toSet()
        val outbounds = sourceItems.filterNot { it.isProviderError() }.map { item ->
            if (item.value("type") !in setOf("selector", "urltest")) return@map item
            val refs = (item["outbounds"] as? JsonArray ?: JsonArray(emptyList()))
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .filter(validTargets::contains)
                .distinct()
                .let { current ->
                    if (current.isNotEmpty() || !item.value("tag").equals("Proxy", ignoreCase = true)) current
                    else routeTags
                }
            val values = item.toMutableMap()
            values["outbounds"] = JsonArray(refs.map(::JsonPrimitive))
            if (item.value("type") == "selector") {
                val currentDefault = item["default"]?.jsonPrimitive?.contentOrNull
                refs.firstOrNull()?.let { values["default"] = JsonPrimitive(currentDefault?.takeIf(refs::contains) ?: it) }
                    ?: values.remove("default")
            }
            JsonObject(values)
        }.toMutableList()
        if (routeTags.isNotEmpty() && outbounds.none {
                val item = it as? JsonObject ?: return@none false
                item.value("type") == "selector" && item.value("tag").equals("Proxy", ignoreCase = true)
            }) {
            outbounds.add(0, JsonObject(mapOf(
                "type" to JsonPrimitive("selector"),
                "tag" to JsonPrimitive("Proxy"),
                "outbounds" to JsonArray(routeTags.map(::JsonPrimitive)),
                "default" to JsonPrimitive(routeTags.first()),
            )))
        }
        val validRouteTargets = outbounds.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            item.takeIf { it.value("type") != "dns" && !it.isProviderInfo() && !it.isProviderError() }
                ?.value("tag")?.takeIf(String::isNotBlank)
        }.toSet()
        val selectedSetting = preferences.getString(selectedServerKey(profile), null)
        val selected = selectedSetting?.takeIf { it in routeTags }
            ?: routeTags.firstOrNull()
        if (selected != null && selected != selectedSetting) preferences.edit().putString(selectedServerKey(profile), selected).apply()
        val values = base.toMutableMap()
        values["outbounds"] = JsonArray(outbounds)
        val route = ((base["route"] as? JsonObject)?.toMutableMap() ?: mutableMapOf())
        val safeFallback = when {
            "Proxy" in validRouteTargets -> "Proxy"
            selected != null && selected in validRouteTargets -> selected
            "direct" in validRouteTargets -> "direct"
            else -> validRouteTargets.firstOrNull()
        }
        val baseFinal = (route["final"] as? JsonPrimitive)?.contentOrNull
        when (SettingsManager.routingMode) {
            "global" -> route["final"] = JsonPrimitive(selected ?: safeFallback ?: "direct")
            "direct" -> route["final"] = JsonPrimitive("direct")
            else -> route["final"] = JsonPrimitive(baseFinal?.takeIf { it in validRouteTargets } ?: safeFallback ?: "direct")
        }
        route["auto_detect_interface"] = JsonPrimitive(true)
        values["route"] = JsonObject(route)
        if (SettingsManager.dnsMode != "local") {
            val endpoint = if (SettingsManager.dnsMode == "google") "https://dns.google/dns-query" else "https://1.1.1.1/dns-query"
            values["dns"] = JsonObject(mapOf(
                "servers" to JsonArray(listOf(JsonObject(mapOf(
                    "type" to JsonPrimitive("https"),
                    "tag" to JsonPrimitive("secure-dns"),
                    "server" to JsonPrimitive(endpoint),
                )))),
            ))
        }
        val file = runtimeConfigFile(profile)
        file.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(values)))
        if (selectedId() == profile.id) ProfileManager.select(profile.name, file)
    }

    private fun readBaseConfig(profile: VpnProfile): JsonObject? {
        val source = sourceConfigFile(profile)
        val file = if (source.exists()) source else runtimeConfigFile(profile)
        val parsed = runCatching { json.parseToJsonElement(file.readText()).jsonObject }.getOrNull()
        if (parsed != null && !source.exists()) source.writeText(json.encodeToString(JsonObject.serializer(), parsed))
        return parsed
    }

    private fun runtimeConfigFile(profile: VpnProfile) = File(File(root, profile.id), RUNTIME_CONFIG)
    private fun sourceConfigFile(profile: VpnProfile) = File(File(root, profile.id), SOURCE_CONFIG)
    private fun selectedServerKey(profile: VpnProfile) = "selected_server_${profile.id}"
    private fun pingHistoryKey(profile: VpnProfile) = "ping_history_${profile.id}"
    private fun normalizeEpoch(value: Long?): Long? = value?.let { if (it > 100_000_000_000L) it / 1000L else it }

    private companion object {
        const val RUNTIME_CONFIG = "using_config.json"
        const val SOURCE_CONFIG = "source_config.json"
        const val MAX_PING_SAMPLES = 20
    }

    data class AppEntry(val packageName: String, val label: String)
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
