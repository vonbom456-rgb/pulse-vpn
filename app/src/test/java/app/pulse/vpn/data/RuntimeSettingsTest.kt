package app.pulse.vpn.data

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class RuntimeSettingsTest {
    private val base = Json.parseToJsonElement("""{"outbounds":[{"tag":"Proxy","type":"selector","outbounds":["A","B"],"default":"A"},{"type":"direct","tag":"direct"}],"route":{"final":"A","rules":[{"domain":["example.com"],"outbound":"direct"},{"protocol":"dns","action":"hijack-dns"}]},"dns":{"servers":[{"type":"local","tag":"local"}]}}""").jsonObject

    @Test fun selectedServerBecomesActualDefaultWithoutMutatingSource() {
        val runtime = RuntimeSettings.apply(base, "B", "rules", "local")
        assertEquals("B", runtime["outbounds"]!!.jsonArray.first().jsonObject["default"]!!.jsonPrimitive.content)
        assertEquals("Proxy", runtime["route"]!!.jsonObject["final"]!!.jsonPrimitive.content)
        assertEquals("A", base["outbounds"]!!.jsonArray.first().jsonObject["default"]!!.jsonPrimitive.content)
        assertEquals(base["dns"], runtime["dns"])
        assertEquals(base["route"]!!.jsonObject["rules"]!!.jsonArray, runtime["route"]!!.jsonObject["rules"]!!.jsonArray.drop(1))
    }

    @Test fun allTrafficAndDirectOverrideProviderRoutesButRetainDnsHijack() {
        listOf("global" to "Proxy", "direct" to "direct").forEach { (mode, final) ->
            val route = RuntimeSettings.apply(base, "B", mode, "local")["route"]!!.jsonObject
            assertEquals(final, route["final"]!!.jsonPrimitive.content)
            assertEquals(2, route["rules"]!!.jsonArray.size)
            assertEquals(53, route["rules"]!!.jsonArray.first().jsonObject["port"]!!.jsonPrimitive.int)
            assertEquals("hijack-dns", route["rules"]!!.jsonArray.first().jsonObject["action"]!!.jsonPrimitive.content)
        }
    }

    @Test fun secureDnsUsesIpAndSeparatePathWithoutBootstrapLoop() {
        listOf("google" to "8.8.8.8", "cloudflare" to "1.1.1.1").forEach { (mode, ip) ->
            val runtime = RuntimeSettings.apply(base, "B", "rules", mode)
            val dns = runtime["dns"]!!.jsonObject
            val server = dns["servers"]!!.jsonArray.last().jsonObject
            assertEquals(ip, server["server"]!!.jsonPrimitive.content)
            assertEquals("/dns-query", server["path"]!!.jsonPrimitive.content)
            assertEquals(server["tag"], dns["final"])
            assertEquals(server["tag"], runtime["route"]!!.jsonObject["default_domain_resolver"])
            assertEquals(base["dns"]!!.jsonObject["servers"]!!.jsonArray.first(), dns["servers"]!!.jsonArray.first())
        }
    }
}
