package app.pulse.vpn.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Happ-compatible metadata, kept separate from the configuration sent to libbox. */
internal object SubscriptionMetadata {
    val keys = setOf("profile-title", "announce", "support-url", "profile-web-page-url", "subscription-userinfo", "profile-update-interval")

    fun fromBody(body: String): Map<String, String> {
        val text = unwrap(body)
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull()
        val fields = root?.entries?.mapNotNull { (key, value) ->
            if (key.lowercase() in keys) (value as? JsonPrimitive)?.contentOrNull?.let { key.lowercase() to it } else null
        }?.toMap().orEmpty()
        val comments = text.lineSequence().mapNotNull { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith('#')) return@mapNotNull null
            val pair = trimmed.removePrefix("#").trim().split(':', limit = 2)
            val key = pair.first().trim().lowercase()
            if (pair.size == 2 && key in keys) key to pair[1].trim() else null
        }.toMap()
        return fields + comments
    }

    fun unwrap(body: String): String {
        val text = body.trim().trimStart('\uFEFF')
        return decodeBase64(text)?.takeIf { decoded ->
            decoded.contains("://") || decoded.trimStart().startsWith('{') || decoded.trimStart().startsWith('#')
        } ?: text
    }

    fun text(value: String?, limit: Int = 4096): String? {
        val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        val decodedUrl = runCatching { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrDefault(raw)
        val prefixed = decodedUrl.startsWith("base64:", true) || decodedUrl.startsWith("base64,", true)
        val decoded = if (prefixed) decodeBase64(decodedUrl.substring(7)) ?: return null else decodedUrl
        return decoded.replace("\\n", "\n").replace("\r\n", "\n")
            .filter { it == '\n' || it == '\t' || !it.isISOControl() }.trim().take(limit).takeIf(String::isNotBlank)
    }

    fun link(value: String?): String? = text(value)?.let { raw ->
        val normalized = if (raw.startsWith('@')) "https://t.me/${raw.drop(1)}" else raw
        runCatching { URI(normalized) }.getOrNull()?.takeIf {
            it.scheme?.lowercase() in setOf("http", "https") && !it.host.isNullOrBlank() && it.userInfo == null
        }?.toASCIIString()
    }

    private fun decodeBase64(value: String): String? = runCatching {
        val clean = value.filterNot(Char::isWhitespace).replace('-', '+').replace('_', '/')
        val bytes = Base64.getDecoder().decode(clean)
        StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    }.getOrNull()
}
