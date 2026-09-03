package app.pulse.vpn.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SubscriptionImporterTest {
    private val importer = SubscriptionImporter()

    @Test
    fun importsVlessRealityLink() = runBlocking {
        val result = importer.import(
            "vless://11111111-1111-4111-8111-111111111111@example.com:443?security=reality&sni=example.com&fp=chrome&pbk=public&type=tcp#Finland",
        )
        val root = Json.parseToJsonElement(result.config).jsonObject
        val outbounds = root["outbounds"]!!.jsonArray
        assertTrue(outbounds.any { it.jsonObject["tag"]?.jsonPrimitive?.content == "Finland" })
        assertTrue(root["inbounds"]!!.jsonArray.any { it.jsonObject["type"]?.jsonPrimitive?.content == "tun" })
    }

    @Test
    fun importsUnpaddedBase64Subscription() = runBlocking {
        val source = "trojan://secret@example.org:443?security=tls&sni=example.org#NL"
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(source.toByteArray())
        val result = importer.import(encoded)
        val root = Json.parseToJsonElement(result.config).jsonObject
        assertTrue(root["outbounds"]!!.jsonArray.any { it.jsonObject["type"]?.jsonPrimitive?.content == "trojan" })
    }

    @Test
    fun preservesFullSingBoxConfigAndAddsTun() = runBlocking {
        val source = """{"outbounds":[{"type":"direct","tag":"direct"}]}"""
        val result = importer.import(source)
        val root = Json.parseToJsonElement(result.config).jsonObject
        assertEquals("tun", root["inbounds"]!!.jsonArray.first().jsonObject["type"]!!.jsonPrimitive.content)
    }

    @Test
    fun sendsRemnawaveDeviceHeadersAndHappAgentForAutoRedirect() = runBlocking {
        var captured: okhttp3.Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    "vless://11111111-1111-4111-8111-111111111111@example.com:443?security=tls#Finland"
                        .toResponseBody("text/plain".toMediaType()),
                )
                .build()
        }.build()
        val importer = SubscriptionImporter(
            identityProvider = {
                SubscriptionIdentity(
                    hwid = "1234567890abcdef1234567890abcdef",
                    deviceOs = "Android",
                    osVersion = "15",
                    deviceModel = "Pulse Test Phone",
                )
            },
            client = client,
        )

        importer.import("https://provider.example/sub/token/redirect/auto")

        val request = captured
        assertNotNull(request)
        assertEquals("1234567890abcdef1234567890abcdef", request!!.header("X-HWID"))
        assertEquals("Android", request!!.header("X-Device-OS"))
        assertEquals("15", request!!.header("X-Ver-OS"))
        assertEquals("Pulse Test Phone", request!!.header("X-Device-Model"))
        assertTrue(request!!.header("User-Agent")!!.startsWith("Happ/"))
    }

    @Test
    fun rejectsProviderErrorNodeInsteadOfCreatingBrokenProfile() = runBlocking {
        val result = runCatching {
            importer.import(
                "vless://11111111-1111-4111-8111-111111111111@error.cdn-global.pro:443?security=tls#%E2%9D%8C%20Missing%20device%20HWID",
            )
        }
        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull()?.message.orEmpty().contains("VLESS"))
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Провайдер"))
    }
}
