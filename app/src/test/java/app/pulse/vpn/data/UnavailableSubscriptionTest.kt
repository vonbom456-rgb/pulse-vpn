package app.pulse.vpn.data

import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class UnavailableSubscriptionTest {
    private val url = "https://example.invalid/sub/private-test-token"
    private val healthy = """{"type":"vless","tag":"Finland","server":"fi.example.invalid","server_port":443,"uuid":"11111111-1111-4111-8111-111111111111"}"""
    private val denial = """{"type":"vless","tag":"❌ Лимит устройств: удалите старое устройство","server":"error.example.invalid","server_port":443}"""

    private fun http(code: Int = 200, body: String = "", headers: Map<String, String> = emptyMap()) =
        SubscriptionImporter(client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("Fixture").body(body.toResponseBody())
                .apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        }.build())

    private fun assertBlocked(result: ImportedProfile, code: String) {
        assertEquals(code, result.issue?.code)
        assertTrue(result.issue!!.blocksConnection)
        val config = Json.parseToJsonElement(result.config).jsonObject
        assertTrue(config["outbounds"]!!.jsonArray.all { it.jsonObject["type"]!!.jsonPrimitive.content == "direct" })
        assertEquals("reject", config["route"]!!.jsonObject["rules"]!!.jsonArray.first().jsonObject["action"]!!.jsonPrimitive.content)
        assertFalse(result.config.contains("error.example.invalid"))
    }

    @Test fun errorOnlyServersBecomePersistentStatus() = runBlocking {
        val result = http(body = """{"outbounds":[$denial]}""").import(url)
        assertBlocked(result, "device_limit")
        assertEquals(url, result.sourceUrl)
        assertTrue(result.issue!!.message.contains("удалите старое устройство"))
    }

    @Test fun errorAlongsideHealthyServersIsAnAdvisoryNotARoute() = runBlocking {
        val result = http(body = """{"outbounds":[$denial,$healthy]}""").import(url)
        assertFalse(result.issue!!.blocksConnection)
        val config = Json.parseToJsonElement(result.config).jsonObject
        val selector = config["outbounds"]!!.jsonArray.first { it.jsonObject["type"]!!.jsonPrimitive.content == "selector" }.jsonObject
        assertEquals(listOf("Finland"), selector["outbounds"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertFalse(result.config.contains("error.example.invalid"))
    }

    @Test fun base64ErrorLinkIsSaved() = runBlocking {
        val uri = "vless://11111111-1111-4111-8111-111111111111@error.example.invalid:443#Device%20limit%20reached"
        assertBlocked(http(body = Base64.getEncoder().encodeToString(uri.toByteArray())).import(url), "device_limit")
    }

    @Test fun deviceHeaderWinsEvenIfBodyContainsWorkingServers() = runBlocking {
        for (flag in listOf("true", "1")) for (code in listOf(200, 403)) {
            val result = http(code, """{"outbounds":[$healthy]}""", mapOf(
                "x-hwid-max-devices-reached" to flag,
                "profile-title" to "base64:" + Base64.getEncoder().encodeToString("Моя подписка".toByteArray()),
                "announce" to "base64:" + Base64.getEncoder().encodeToString("Помощь с подпиской".toByteArray()),
                "support-url" to "https://t.me/pulse_demo",
                "subscription-userinfo" to "total=1000; expire=1893456000",
            )).import(url)
            assertBlocked(result, "device_limit")
            assertEquals("Моя подписка", result.name)
            assertEquals("Помощь с подпиской", result.providerDescription)
            assertEquals("https://t.me/pulse_demo", result.providerSupportUrl)
            assertEquals(1000L, result.userInfo.total)
        }
    }

    @Test fun unsupportedDeviceHeaderIsSaved() = runBlocking {
        assertBlocked(http(403, headers = mapOf("x-hwid-not-supported" to "true")).import(url), "device_id")
    }

    @Test fun accessFailuresAreSavedAndNotConnectable() = runBlocking {
        for (code in listOf(401, 403, 404, 410)) assertBlocked(http(code).import(url), "access_denied")
    }

    @Test fun serverFailureIsSavedAsRetryable() = runBlocking {
        val result = http(503, "<html>unavailable</html>").import(url)
        assertBlocked(result, "http")
        assertTrue(result.issue!!.retryable)
        assertFalse(result.issue.message.contains("<html>"))
    }

    @Test fun networkFailureKeepsLinkButNotSecretExceptionText() = runBlocking {
        val importer = SubscriptionImporter(client = OkHttpClient.Builder().addInterceptor {
            throw IOException("Private URL: $url")
        }.build())
        val result = importer.import(url)
        assertBlocked(result, "network")
        assertEquals(url, result.sourceUrl)
        assertFalse(result.issue!!.message.contains("private-test-token"))
    }

    @Test fun htmlIsSavedAsRetryableFormatIssue() = runBlocking {
        assertBlocked(http(body = "<html>Login first</html>").import(url), "format")
    }

    @Test fun invalidLocalInputRemainsAnImportError() = runBlocking {
        assertTrue(runCatching { SubscriptionImporter().import("not-a-profile") }.isFailure)
    }

    @Test fun jsonErrorMessageIsDisplayedAndSensitiveLinksAreRedacted() = runBlocking {
        val result = http(403, """{"message":"Device limit reached. See https://example.invalid/secret"}""").import(url)
        assertBlocked(result, "device_limit")
        assertTrue(result.issue!!.message.contains("Device limit reached"))
        assertFalse(result.issue.message.contains("/secret"))
    }

    @Test fun plainServiceMessageIsSaved() = runBlocking {
        assertBlocked(http(body = "Слишком много устройств").import(url), "device_limit")
    }

    @Test fun expiredPlaceholderIsNotAServer() = runBlocking {
        val result = http(body = """{"outbounds":[{"type":"vless","tag":"Subscription expired","server":"127.0.0.1","server_port":443}]}""").import(url)
        assertBlocked(result, "expired")
    }

    @Test fun emptySubscriptionIsNotADirectOnlyVpn() = runBlocking {
        assertBlocked(http(body = """{"outbounds":[]}""").import(url), "no_servers")
    }

    @Test fun healthyProfileDoesNotHaveAnIssue() = runBlocking {
        assertNull(http(body = """{"outbounds":[$healthy]}""").import(url).issue)
    }

    @Test fun previouslySavedProfilesRemainReadable() {
        val old = Json.decodeFromString<VpnProfile>("""{"id":"legacy","name":"Saved","updatedAt":42}""")
        assertNull(old.issue)
    }
}

