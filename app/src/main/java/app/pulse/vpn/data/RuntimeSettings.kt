package app.pulse.vpn.data

import kotlinx.serialization.json.*

/** Applies app choices to a copy; the stored provider configuration stays reusable. */
internal object RuntimeSettings {
    fun apply(base: JsonObject, selected: String?, mode: String, dnsMode: String, options: app.pulse.vpn.core.AdvancedOptions = app.pulse.vpn.core.AdvancedOptions()): JsonObject {
        val values = base.toMutableMap()
        val outbounds = (base["outbounds"] as? JsonArray).orEmpty().map { element ->
            val item = element as? JsonObject ?: return@map element
            if (item.text("type") != "selector" || item.text("tag") != "Proxy" || selected == null) return@map item
            val refs = (item["outbounds"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            JsonObject(item + mapOf("default" to JsonPrimitive(selected), "outbounds" to JsonArray((refs + selected).distinct().map(::JsonPrimitive))))
        }
        values["outbounds"] = JsonArray(outbounds)
        if (outbounds.none { (it as? JsonObject)?.text("tag") == "direct" }) {
            values["outbounds"] = JsonArray(outbounds + buildJsonObject { put("type", "direct"); put("tag", "direct") })
        }
        values["inbounds"] = JsonArray((base["inbounds"] as? JsonArray).orEmpty().map { element ->
            val inbound = element as? JsonObject ?: return@map element
            if (inbound.text("type") != "tun") return@map inbound
            JsonObject(inbound.toMutableMap().apply {
                if (options.number("mtu") > 0) put("mtu", JsonPrimitive(options.number("mtu")))
                if (options.text("stack") != "profile") put("stack", JsonPrimitive(options.text("stack")))
            })
        })
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
            val address = when (dnsMode) { "google" -> "8.8.8.8"; "quad9" -> "9.9.9.9"; "adguard" -> "94.140.14.14"; else -> "1.1.1.1" }
            val hostname = when (dnsMode) { "google" -> "dns.google"; "quad9" -> "dns.quad9.net"; "adguard" -> "dns.adguard-dns.com"; else -> "cloudflare-dns.com" }
            val custom = buildJsonObject {
                put("type", "https"); put("tag", tag)
                put("server", address)
                put("server_port", 443); put("path", "/dns-query")
                put("tls", buildJsonObject { put("enabled", true); put("server_name", hostname) })
            }
            // Keep named resolvers referenced explicitly by provider outbounds.
            dns["servers"] = JsonArray(servers + custom)
            dns["final"] = JsonPrimitive(tag)
            dns.remove("rules")
            values["dns"] = JsonObject(dns)
            route["default_domain_resolver"] = JsonPrimitive(tag)
        }
        route["auto_detect_interface"] = JsonPrimitive(true)
        val dns = (values["dns"] as? JsonObject).orEmpty().toMutableMap()
        dns["disable_cache"] = JsonPrimitive(!options.bool("dns_cache"))
        dns["independent_cache"] = JsonPrimitive(options.bool("dns_independent"))
        if (options.text("dns_strategy") != "profile") dns["strategy"] = JsonPrimitive(options.text("dns_strategy"))
        values["dns"] = JsonObject(dns)
        // URI subscriptions often have no DNS rule. Capture system DNS requests so
        // they actually use the configured resolver instead of being sent to a VPN node.
        val rules = (route["rules"] as? JsonArray).orEmpty()
        val hijack = buildJsonObject { put("port", 53); put("action", "hijack-dns") }
        val customRules = buildList {
            add(hijack)
            if (options.bool("block_quic")) add(buildJsonObject { put("network", "udp"); put("port", 443); put("action", "reject") })
            if (options.bool("bypass_lan")) add(buildJsonObject { put("ip_is_private", true); put("action", "route"); put("outbound", "direct") })
        }
        route["rules"] = JsonArray(customRules + rules.filterNot { it == hijack })
        values["route"] = JsonObject(route)
        return JsonObject(values)
    }

    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
}
