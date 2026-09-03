package app.pulse.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import org.yaml.snakeyaml.Yaml
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit

class SubscriptionImporter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val schemes = setOf("vless", "vmess", "trojan", "ss", "hysteria2", "hy2", "tuic")

    suspend fun import(input: String): ImportedProfile = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Вставьте ссылку или конфигурацию" }
        val remote = trimmed.startsWith("https://") || trimmed.startsWith("http://")
        val response = if (remote) fetch(trimmed) else Fetched(trimmed, null, SubscriptionUserInfo())
        val normalized = normalize(response.body)
        ImportedProfile(
            name = response.name ?: guessName(trimmed, normalized),
            config = normalized,
            sourceUrl = trimmed.takeIf { remote },
            userInfo = response.userInfo,
        )
    }

    private fun fetch(url: String): Fetched {
        val request = Request.Builder().url(url)
            .header("User-Agent", "PulseVPN/0.3 sing-box Android")
            .header("Accept", "application/json,text/plain,*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Провайдер вернул HTTP ${response.code}")
            val body = response.body.string().trimStart('\uFEFF')
            if (body.isBlank()) error("Подписка вернула пустой ответ")
            val disposition = response.header("content-disposition").orEmpty()
            val profileTitle = response.header("profile-title")
                ?: Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.get(1)?.let(::decode)
            return Fetched(body, profileTitle, parseUserInfo(response.header("subscription-userinfo")))
        }
    }

    private fun normalize(raw: String): String {
        val text = raw.trim().trimStart('\uFEFF')
        parseSingBox(text)?.let { return ensureRuntimeConfig(it) }

        val decoded = decodeBase64(text)
        if (decoded != null && decoded != text) {
            parseSingBox(decoded)?.let { return ensureRuntimeConfig(it) }
            parseUriLines(decoded)?.let { return buildConfig(it) }
        }

        parseUriLines(text)?.let { return buildConfig(it) }
        parseClash(text)?.let { return buildConfig(it) }
        error("Формат не распознан. Поддерживаются sing-box JSON, VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC и Clash YAML")
    }

    private fun parseSingBox(text: String): JsonObject? = runCatching {
        val root = json.parseToJsonElement(text).jsonObject
        root.takeIf { it["outbounds"] is JsonArray }
    }.getOrNull()

    private fun parseUriLines(text: String): List<JsonObject>? {
        val links = text.lineSequence().map(String::trim).filter(String::isNotBlank)
            .filter { line -> schemes.any { line.startsWith("$it://", true) } }.toList()
        if (links.isEmpty()) return null
        val parsed = links.mapNotNull { runCatching { parseUri(it) }.getOrNull() }
        return parsed.takeIf(List<JsonObject>::isNotEmpty)
    }

    private fun parseUri(value: String): JsonObject {
        val scheme = value.substringBefore(":").lowercase()
        if (scheme == "vmess") return parseVmess(value)
        if (scheme == "ss") return parseShadowsocks(value)
        val uri = URI(value)
        val query = parseQuery(uri.rawQuery)
        val tag = decode(uri.rawFragment ?: "${scheme.uppercase()} ${uri.host.orEmpty()}")
        val port = if (uri.port > 0) uri.port else error("В ссылке $scheme не указан порт")
        return buildJsonObject {
            put("type", if (scheme == "hy2") "hysteria2" else scheme)
            put("tag", tag.ifBlank { scheme.uppercase() })
            put("server", uri.host ?: error("В ссылке $scheme не указан сервер"))
            put("server_port", port)
            when (scheme) {
                "vless" -> {
                    put("uuid", decode(uri.rawUserInfo.orEmpty()))
                    query["flow"]?.takeIf(String::isNotBlank)?.let { put("flow", it) }
                }
                "trojan", "hysteria2", "hy2" -> put("password", decode(uri.rawUserInfo.orEmpty()))
                "tuic" -> {
                    val credentials = decode(uri.rawUserInfo.orEmpty()).split(":", limit = 2)
                    put("uuid", credentials.firstOrNull().orEmpty())
                    put("password", credentials.getOrNull(1).orEmpty())
                }
            }
            tls(query)?.let { put("tls", it) }
            transport(query)?.let { put("transport", it) }
            if (scheme == "hysteria2" || scheme == "hy2") {
                query["obfs"]?.let { obfs -> put("obfs", buildJsonObject { put("type", obfs); query["obfs-password"]?.let { put("password", it) } }) }
            }
        }
    }

    private fun parseVmess(value: String): JsonObject {
        val body = decodeBase64(value.substringAfter("vmess://")) ?: error("Некорректный VMess")
        val source = json.parseToJsonElement(body).jsonObject
        val query = mapOf(
            "security" to source.string("tls"), "sni" to source.string("sni"), "fp" to source.string("fp"),
            "type" to source.string("net"), "host" to source.string("host"), "path" to source.string("path"),
        ).filterValues(String::isNotBlank)
        return buildJsonObject {
            put("type", "vmess")
            put("tag", source.string("ps").ifBlank { "VMess" })
            put("server", source.string("add"))
            put("server_port", source.string("port").toIntOrNull() ?: source["port"]?.jsonPrimitive?.intOrNull ?: 443)
            put("uuid", source.string("id"))
            put("security", source.string("scy").ifBlank { "auto" })
            tls(query)?.let { put("tls", it) }
            transport(query)?.let { put("transport", it) }
        }
    }

    private fun parseShadowsocks(value: String): JsonObject {
        var body = value.substringAfter("ss://")
        val tag = decode(body.substringAfter('#', "Shadowsocks"))
        body = body.substringBefore('#').substringBefore('?')
        val decodedWhole = decodeBase64(body)
        val authority = if (decodedWhole?.contains('@') == true) decodedWhole else body
        val userPart = authority.substringBefore('@')
        val serverPart = authority.substringAfter('@', "")
        val credentials = (decodeBase64(userPart) ?: userPart).split(":", limit = 2)
        val host = serverPart.substringBeforeLast(':')
        val port = serverPart.substringAfterLast(':').toIntOrNull() ?: error("Некорректный порт Shadowsocks")
        return buildJsonObject {
            put("type", "shadowsocks"); put("tag", tag); put("server", host); put("server_port", port)
            put("method", credentials.first()); put("password", credentials.getOrElse(1) { "" })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseClash(text: String): List<JsonObject>? = runCatching {
        val root = Yaml().load<Map<String, Any?>>(text)
        val proxies = root["proxies"] as? List<Map<String, Any?>> ?: return null
        proxies.mapNotNull(::clashProxy).takeIf(List<JsonObject>::isNotEmpty)
    }.getOrNull()

    private fun clashProxy(p: Map<String, Any?>): JsonObject? {
        val type = p["type"]?.toString()?.lowercase() ?: return null
        if (type !in setOf("vless", "vmess", "trojan", "ss", "hysteria2", "tuic")) return null
        return buildJsonObject {
            put("type", type); put("tag", p["name"]?.toString() ?: type.uppercase())
            put("server", p["server"]?.toString() ?: return null)
            put("server_port", p["port"]?.toString()?.toIntOrNull() ?: return null)
            when (type) {
                "vless", "vmess" -> put("uuid", p["uuid"]?.toString().orEmpty())
                "trojan", "hysteria2" -> put("password", p["password"]?.toString().orEmpty())
                "ss" -> { put("method", p["cipher"]?.toString().orEmpty()); put("password", p["password"]?.toString().orEmpty()) }
                "tuic" -> { put("uuid", p["uuid"]?.toString().orEmpty()); put("password", p["password"]?.toString().orEmpty()) }
            }
            val tlsEnabled = p["tls"] == true || type in setOf("trojan", "hysteria2", "tuic")
            if (tlsEnabled) put("tls", buildJsonObject {
                put("enabled", true); p["servername"]?.toString()?.let { put("server_name", it) }
                if (p["skip-cert-verify"] == true) put("insecure", true)
            })
            val network = p["network"]?.toString()
            if (network == "ws") put("transport", buildJsonObject { put("type", "ws") })
        }
    }

    private fun ensureRuntimeConfig(root: JsonObject): String {
        val map = root.toMutableMap()
        val outbounds = (root["outbounds"] as? JsonArray)?.toMutableList() ?: mutableListOf()
        val proxyTags = outbounds.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val type = item.string("type")
            item.string("tag").takeIf { it.isNotBlank() && type !in setOf("direct", "block", "dns", "selector", "urltest") }
        }
        if (outbounds.none { (it as? JsonObject)?.string("type") == "direct" }) {
            outbounds += buildJsonObject { put("type", "direct"); put("tag", "direct") }
        }
        if (proxyTags.isNotEmpty() && outbounds.none { (it as? JsonObject)?.string("type") in setOf("selector", "urltest") }) {
            outbounds.add(0, buildJsonObject {
                put("type", "selector"); put("tag", "Proxy")
                put("outbounds", JsonArray(proxyTags.map(::JsonPrimitive))); put("default", proxyTags.first())
            })
        }
        map["outbounds"] = JsonArray(outbounds)

        val inbounds = (root["inbounds"] as? JsonArray)?.toMutableList() ?: mutableListOf()
        if (inbounds.none { (it as? JsonObject)?.string("type") == "tun" }) inbounds.add(0, tunInbound())
        map["inbounds"] = JsonArray(inbounds)

        if (map["dns"] !is JsonObject) map["dns"] = buildJsonObject {
            put("servers", buildJsonArray { add(buildJsonObject { put("type", "local"); put("tag", "local") }) })
        }
        val route = ((map["route"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()).apply {
            put("auto_detect_interface", JsonPrimitive(true))
            if (get("final") == null && proxyTags.isNotEmpty()) put("final", JsonPrimitive("Proxy"))
        }
        map["route"] = JsonObject(route)
        return json.encodeToString(JsonObject.serializer(), JsonObject(map))
    }

    private fun buildConfig(proxies: List<JsonObject>): String = ensureRuntimeConfig(buildJsonObject {
        put("log", buildJsonObject { put("level", "info"); put("timestamp", true) })
        put("outbounds", JsonArray(proxies))
    })

    private fun tunInbound() = buildJsonObject {
        put("type", "tun"); put("tag", "tun-in")
        put("address", buildJsonArray { add(JsonPrimitive("172.19.0.1/30")); add(JsonPrimitive("fdfe:dcba:9876::1/126")) })
        put("auto_route", true); put("strict_route", true)
    }

    private fun tls(q: Map<String, String>): JsonObject? {
        val enabled = q["security"] in setOf("tls", "reality") || q["tls"] == "1"
        if (!enabled) return null
        return buildJsonObject {
            put("enabled", true)
            q["sni"]?.let { put("server_name", it) }
            if (q["allowInsecure"] == "1" || q["insecure"] == "1") put("insecure", true)
            q["fp"]?.let { put("utls", buildJsonObject { put("enabled", true); put("fingerprint", it) }) }
            if (q["security"] == "reality") put("reality", buildJsonObject {
                put("enabled", true); q["pbk"]?.let { put("public_key", it) }; q["sid"]?.let { put("short_id", it) }
            })
        }
    }

    private fun transport(q: Map<String, String>): JsonObject? = when (q["type"]?.lowercase()) {
        "ws" -> buildJsonObject {
            put("type", "ws"); q["path"]?.let { put("path", it) }
            q["host"]?.let { put("headers", buildJsonObject { put("Host", it) }) }
        }
        "grpc" -> buildJsonObject { put("type", "grpc"); q["serviceName"]?.let { put("service_name", it) } }
        "http", "h2" -> buildJsonObject { put("type", "http"); q["path"]?.let { put("path", it) }; q["host"]?.let { host -> put("host", buildJsonArray { add(JsonPrimitive(host)) }) } }
        else -> null
    }

    private fun decodeBase64(value: String): String? {
        val compact = value.filterNot(Char::isWhitespace)
        if (compact.length < 8) return null
        val padded = compact + "=".repeat((4 - compact.length % 4) % 4)
        return sequenceOf(Base64.getDecoder(), Base64.getUrlDecoder()).mapNotNull { decoder ->
            runCatching { String(decoder.decode(padded), StandardCharsets.UTF_8) }.getOrNull()
        }.firstOrNull { it.any(Char::isLetterOrDigit) }
    }

    private fun parseQuery(raw: String?): Map<String, String> = raw.orEmpty().split('&').mapNotNull {
        if (it.isBlank()) null else decode(it.substringBefore('=')) to decode(it.substringAfter('=', ""))
    }.toMap()

    private fun parseUserInfo(value: String?): SubscriptionUserInfo {
        val values = value.orEmpty().split(';').mapNotNull {
            val key = it.substringBefore('=').trim(); val number = it.substringAfter('=', "").trim().toLongOrNull()
            number?.let { n -> key to n }
        }.toMap()
        return SubscriptionUserInfo(values["upload"], values["download"], values["total"], values["expire"])
    }

    private fun guessName(input: String, config: String): String = runCatching {
        if (input.startsWith("http")) URI(input).host.removePrefix("www.") else {
            val first = json.parseToJsonElement(config).jsonObject["outbounds"]!!.jsonArray.first().jsonObject
            first.string("tag").ifBlank { "Pulse profile" }
        }
    }.getOrDefault("Pulse profile")

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun decode(value: String) = runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)
    private data class Fetched(val body: String, val name: String?, val userInfo: SubscriptionUserInfo)
}
