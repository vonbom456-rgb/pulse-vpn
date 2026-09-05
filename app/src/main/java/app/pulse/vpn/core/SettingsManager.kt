package app.pulse.vpn.core

import com.tencent.mmkv.MMKV
import io.nekohasekai.sfa.bg.VPNService
import org.json.JSONArray

/** Настройки читаются и UI, и отдельным VPN-процессом. */
object SettingsManager {
    const val MMKV_ID = "pulse_settings"
    private val storage by lazy { MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE) }

    val dynamicNotification get() = storage.decodeBool(Keys.DYNAMIC_NOTIFICATION, true)
    val disableMemoryLimit get() = storage.decodeBool(Keys.DISABLE_MEMORY_LIMIT, false)
    var startedByUser: Boolean
        get() = storage.decodeBool(Keys.STARTED_BY_USER, false)
        set(value) { storage.encode(Keys.STARTED_BY_USER, value); storage.sync() }
    var autoConnect: Boolean
        get() = storage.decodeBool(Keys.AUTO_CONNECT, false)
        set(value) { storage.encode(Keys.AUTO_CONNECT, value); storage.sync() }
    var refreshOnOpen: Boolean
        get() = storage.decodeBool(Keys.REFRESH_ON_OPEN, true)
        set(value) { storage.encode(Keys.REFRESH_ON_OPEN, value); storage.sync() }
    var autoFastest: Boolean
        get() = storage.decodeBool(Keys.AUTO_FASTEST, true)
        set(value) { storage.encode(Keys.AUTO_FASTEST, value); storage.sync() }
    var systemProxyEnabled: Boolean
        get() = storage.decodeBool(Keys.SYSTEM_PROXY_ENABLED, true)
        set(value) { storage.encode(Keys.SYSTEM_PROXY_ENABLED, value); storage.sync() }
    var autoRedirect: Boolean
        get() = storage.decodeBool(Keys.AUTO_REDIRECT, false)
        set(value) { storage.encode(Keys.AUTO_REDIRECT, value); storage.sync() }
    var darkTheme: Boolean
        get() = storage.decodeBool(Keys.DARK_THEME, true)
        set(value) { storage.encode(Keys.DARK_THEME, value); storage.sync() }
    var liveEffects: Boolean
        get() = storage.decodeBool(Keys.LIVE_EFFECTS, true)
        set(value) { storage.encode(Keys.LIVE_EFFECTS, value); storage.sync() }
    /** Visual accent. `profile` follows a theme hint embedded by a provider, `pulse` is the reset. */
    var accentTheme: String
        get() = storage.decodeString(Keys.ACCENT_THEME, "profile") ?: "profile"
        set(value) { storage.encode(Keys.ACCENT_THEME, value); storage.sync() }
    var routingMode: String
        get() = storage.decodeString(Keys.ROUTING_MODE, "rules") ?: "rules"
        set(value) { storage.encode(Keys.ROUTING_MODE, value); storage.sync() }
    var dnsMode: String
        get() = storage.decodeString(Keys.DNS_MODE, "local") ?: "local"
        set(value) { storage.encode(Keys.DNS_MODE, value); storage.sync() }
    var perAppProxyMode: Int
        get() = storage.decodeInt(Keys.PER_APP_PROXY_MODE, Keys.PER_APP_PROXY_DISABLED)
        set(value) { storage.encode(Keys.PER_APP_PROXY_MODE, value.coerceIn(0, 2)); storage.sync() }

    val perAppProxyExcludeList get() = readList(Keys.PER_APP_PROXY_EXCLUDE_LIST)
    val perAppProxyIncludeList get() = readList(Keys.PER_APP_PROXY_INCLUDE_LIST)

    fun setPerAppList(mode: Int, packages: Set<String>) {
        perAppProxyMode = mode
        val key = if (mode == Keys.PER_APP_PROXY_INCLUDE) Keys.PER_APP_PROXY_INCLUDE_LIST else Keys.PER_APP_PROXY_EXCLUDE_LIST
        storage.encode(key, JSONArray(packages.toList()).toString())
        storage.sync()
    }

    fun serviceClass(): Class<*> = VPNService::class.java

    private fun readList(key: String): List<String> {
        val array = runCatching { JSONArray(storage.decodeString(key, "[]")) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }

    object Keys {
        const val DYNAMIC_NOTIFICATION = "dynamic_notification"
        const val DISABLE_MEMORY_LIMIT = "disable_memory_limit"
        const val STARTED_BY_USER = "started_by_user"
        const val AUTO_CONNECT = "auto_connect"
        const val REFRESH_ON_OPEN = "refresh_on_open"
        const val AUTO_FASTEST = "auto_fastest"
        const val SYSTEM_PROXY_ENABLED = "system_proxy_enabled"
        const val AUTO_REDIRECT = "auto_redirect"
        const val DARK_THEME = "dark_theme"
        const val LIVE_EFFECTS = "live_effects"
        const val ACCENT_THEME = "accent_theme"
        const val ROUTING_MODE = "routing_mode"
        const val DNS_MODE = "dns_mode"
        const val PER_APP_PROXY_MODE = "per_app_proxy_mode"
        const val PER_APP_PROXY_DISABLED = 0
        const val PER_APP_PROXY_INCLUDE = 1
        const val PER_APP_PROXY_EXCLUDE = 2
        const val PER_APP_PROXY_EXCLUDE_LIST = "per_app_proxy_exclude_list"
        const val PER_APP_PROXY_INCLUDE_LIST = "per_app_proxy_include_list"
    }
}
