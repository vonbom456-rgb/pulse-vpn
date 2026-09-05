package app.pulse.vpn.data

import kotlinx.serialization.json.*

/** Applies app choices to a copy; the stored provider configuration stays reusable. */
internal object RuntimeSettings {
    fun apply(base: JsonObject, selected: String?, mode: String, dnsMode: String): JsonObject {
        val values = base.toMutableMap()
        val outbounds = (base["outbounds"] as? JsonArray).orEmpty().map { element ->
            val item = element as? JsonObject ?: return@map element
            if (item.text("type") != "selector" || item.text("tag") != "Proxy" || selected == null) return@map item
            val refs = (item["outbounds"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            JsonObject(item + mapOf("default" to JsonPrimitive(selected), "outbounds" to JsonArray((refs + selected).distinct().map(::JsonPrimitive))))
        }
        values["outbounds"] = JsonArray(outbounds)
        val route = (base["route"] as? JsonObject).orEmpty().toMutableMap()
        val hasSelector = outbounds.any { (it as? JsonObject)?.text("tag") == "Proxy" }
        route["final"] = JsonPrimitive(if (mode == "direct") "direct" else if (hasSelector) "Proxy" else selected ?: "direct")
        if (mode in setOf("global", "direct")) {
            // Route rules take precedence over final. Keep DNS/sniff actions only.
            route["rules"] = JsonArray((route["rules"] as? JsonArray).orEmpty().filter {
                (it as? JsonObject)?.text("action") in setOf("hijack-dns", "sniff", "resolve")
            })
        }
        if (dnsMode != "local") {
            val dns = (base["dns"] as? JsonObject).orEmpty().toMutableMap()
            val servers = (dns["servers"] as? JsonArray).orEmpty()
            val existingTags = servers.mapNotNull { (it as? JsonObject)?.text("tag") }.toSet()
            val tag = generateSequence("pulse-dns") { "$it-1" }.first { it !in existingTags }
            val google = dnsMode == "google"
            val custom = buildJsonObject {
                put("type", "https"); put("tag", tag)
                put("server", if (google) "8.8.8.8" else "1.1.1.1")
                put("server_port", 443); put("path", "/dns-query")
                put("tls", buildJsonObject { put("enabled", true); put("server_name", if (google) "dns.google" else "cloudflare-dns.com") })
            }
            // Keep named resolvers referenced explicitly by provider outbounds.
            dns["servers"] = JsonArray(servers + custom)
            dns["final"] = JsonPrimitive(tag)
            dns.remove("rules")
            values["dns"] = JsonObject(dns)
            route["default_domain_resolver"] = JsonPrimitive(tag)
        }
        route["auto_detect_interface"] = JsonPrimitive(true)
        // URI subscriptions often have no DNS rule. Capture system DNS requests so
        // they actually use the configured resolver instead of being sent to a VPN node.
        val rules = (route["rules"] as? JsonArray).orEmpty()
        val hijack = buildJsonObject { put("port", 53); put("action", "hijack-dns") }
        route["rules"] = JsonArray(listOf(hijack) + rules.filterNot { it == hijack })
        values["route"] = JsonObject(route)
        return JsonObject(values)
    }

    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
}
