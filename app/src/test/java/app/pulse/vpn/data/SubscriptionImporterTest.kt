package app.pulse.vpn.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
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
}
