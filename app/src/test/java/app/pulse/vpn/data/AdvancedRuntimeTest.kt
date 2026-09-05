package app.pulse.vpn.data

import app.pulse.vpn.core.AdvancedOptions
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AdvancedRuntimeTest {
    private val base = Json.parseToJsonElement("""{"inbounds":[{"type":"tun","tag":"tun","mtu":9000,"stack":"mixed"},{"type":"mixed","tag":"http"}],"outbounds":[{"type":"selector","tag":"Proxy","outbounds":["A"],"default":"A"},{"type":"direct","tag":"A"}],"route":{"rules":[{"domain_suffix":["example.com"],"outbound":"A"}]} }""").jsonObject
    private fun apply(key: String, value: String) = RuntimeSettings.apply(base, "A", "global", "cloudflare", AdvancedOptions().with(key, value))
    @Test fun mtuOnlyChangesTun() {
        val inbounds = apply("mtu", "1400")["inbounds"]!!.jsonArray
        assertEquals(1400, inbounds.first().jsonObject["mtu"]!!.jsonPrimitive.int)
        assertFalse(inbounds.last().jsonObject.containsKey("mtu"))
        assertEquals(9000, base["inbounds"]!!.jsonArray.first().jsonObject["mtu"]!!.jsonPrimitive.int)
    }
    @Test fun stackOnlyChangesTun() {
        val inbounds = apply("stack", "gvisor")["inbounds"]!!.jsonArray
        assertEquals("gvisor", inbounds.first().jsonObject["stack"]!!.jsonPrimitive.content)
        assertFalse(inbounds.last().jsonObject.containsKey("stack"))
    }
    @Test fun profileMtuIsPreserved() { assertEquals(base["inbounds"], apply("mtu", "0")["inbounds"]) }
    @Test fun directTargetIsCreatedWhenMissing() {
        assertTrue(apply("bypass_lan", "true")["outbounds"]!!.jsonArray.any { it.jsonObject["tag"]?.jsonPrimitive?.content == "direct" })
    }
    @Test fun privateRuleFollowsDnsHijack() {
        val rules = apply("bypass_lan", "true")["route"]!!.jsonObject["rules"]!!.jsonArray
        assertEquals("hijack-dns", rules.first().jsonObject["action"]!!.jsonPrimitive.content)
        assertEquals(true, rules[1].jsonObject["ip_is_private"]!!.jsonPrimitive.boolean)
    }
    @Test fun quicBlockDoesNotBlockTcp() {
        val rule = apply("block_quic", "true")["route"]!!.jsonObject["rules"]!!.jsonArray[1].jsonObject
        assertEquals("udp", rule["network"]!!.jsonPrimitive.content)
        assertEquals(443, rule["port"]!!.jsonPrimitive.int)
        assertEquals("reject", rule["action"]!!.jsonPrimitive.content)
    }
    @Test fun dnsCacheCanBeDisabled() { assertTrue(apply("dns_cache", "false")["dns"]!!.jsonObject["disable_cache"]!!.jsonPrimitive.boolean) }
    @Test fun independentCacheCanBeDisabled() { assertFalse(apply("dns_independent", "false")["dns"]!!.jsonObject["independent_cache"]!!.jsonPrimitive.boolean) }
    @Test fun strategyIsApplied() { assertEquals("ipv4_only", apply("dns_strategy", "ipv4_only")["dns"]!!.jsonObject["strategy"]!!.jsonPrimitive.content) }
    @Test fun runtimeChangesNeverMutateSource() {
        val before = base.toString()
        listOf("mtu" to "1500", "stack" to "system", "bypass_lan" to "true", "block_quic" to "true").forEach { (key, value) -> apply(key, value) }
        assertEquals(before, base.toString())
    }
}

