package app.pulse.vpn.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class SubscriptionMetadataTest {
    private val server = "vless://11111111-1111-4111-8111-111111111111@example.com:443#Test"
    private fun encoded(value: String) = "base64:" + Base64.getEncoder().encodeToString(value.toByteArray())
    private suspend fun import(body: String, headers: Map<String, String> = emptyMap()): ImportedProfile {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .apply { headers.forEach { (key, value) -> header(key, value) } }
                .body(body.toResponseBody()).build()
        }.build()
        return SubscriptionImporter(client = client).import("https://provider.example/sub/test/redirect/auto")
    }

    @Test fun importsHappAnnouncementAndContactsFromHeaders() = runBlocking {
        val description = "Добро пожаловать в Pulse!\nПоддержка работает каждый день."
        val result = import(server, mapOf(
            "announce" to encoded(description),
            "support-url" to encoded("https://t.me/pulse_support"),
            "profile-web-page-url" to "https://provider.example/account",
        ))
        assertEquals(description, result.providerDescription)
        assertEquals("https://t.me/pulse_support", result.providerTelegram)
        assertEquals("https://t.me/pulse_support", result.providerSupportUrl)
        assertEquals("https://provider.example/account", result.providerWebsite)
    }

    @Test fun bodyOverridesHeadersAndMergesMissingFields() = runBlocking {
        val result = import("#announce: ${encoded("Описание из тела")}\n#profile-title: My subscription\n#subscription-userinfo: total=0; expire=1788885540000\n$server", mapOf(
            "announce" to encoded("Описание из заголовка"), "profile-title" to "Old title", "support-url" to "https://provider.example/help",
        ))
        assertEquals("Описание из тела", result.providerDescription)
        assertEquals("My subscription", result.name)
        assertEquals(1788885540L, result.userInfo.expire)
        assertEquals("https://provider.example/help", result.providerSupportUrl)
        assertNull(result.providerTelegram)
    }

    @Test fun importsMetadataInsideBase64Subscription() = runBlocking {
        val body = "#announce: ${encoded("Первая строка\nВторая строка")}\n#support-url: @pulse_support\n$server"
        val result = import(Base64.getEncoder().encodeToString(body.toByteArray()))
        assertEquals("Первая строка\nВторая строка", result.providerDescription)
        assertEquals("https://t.me/pulse_support", result.providerSupportUrl)
    }

    @Test fun removesDisplayFieldsFromRuntimeAndKeepsThemeHint() = runBlocking {
        val result = import("""{"announce":"Hello","theme":"ocean","profile-title":"Ocean","outbounds":[{"type":"direct","tag":"direct"}]}""")
        assertEquals("Hello", result.providerDescription)
        assertEquals("ocean", result.themeHint)
        val root = Json.parseToJsonElement(result.config).jsonObject
        assertFalse(root.containsKey("announce"))
        assertFalse(root.containsKey("theme"))
        assertFalse(root.containsKey("profile-title"))
    }

    @Test fun malformedMetadataDoesNotBreakValidSubscription() = runBlocking {
        val result = import(server, mapOf("announce" to "base64:invalid!", "support-url" to "javascript:alert(1)", "profile-web-page-url" to "file:///private"))
        assertNull(result.providerDescription)
        assertNull(result.providerSupportUrl)
        assertNull(result.providerWebsite)
        assertTrue(result.config.contains("vless"))
    }

    @Test fun preservesMultilineAndLiteralPlusInAnnouncement() {
        assertEquals("A+B\nC", SubscriptionMetadata.text("A+B%0AC"))
        assertNull(SubscriptionMetadata.text("base64:/w=="))
    }

    @Test fun plainTitleIsNotMistakenForBase64() = runBlocking {
        assertEquals("Test", import(server, mapOf("profile-title" to "Test")).name)
    }

    @Test fun acceptsCommentMetadataBeforeFullJson() = runBlocking {
        val result = import("#announce: Welcome\n{\"outbounds\":[{\"type\":\"direct\",\"tag\":\"direct\"}]}")
        assertEquals("Welcome", result.providerDescription)
    }
}
