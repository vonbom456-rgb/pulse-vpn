package app.pulse.vpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pulse.vpn.core.SettingsManager
import app.pulse.vpn.core.TrafficSnapshot
import app.pulse.vpn.core.VpnController
import app.pulse.vpn.data.ImportResult
import app.pulse.vpn.data.ProfileRepository
import app.pulse.vpn.data.VpnProfile
import app.pulse.vpn.data.VpnServer
import app.pulse.vpn.data.isInfoMetadata
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.InetAddress
import java.net.Socket

enum class Screen { HOME, ROUTES, STATS, SETTINGS, PROFILES, APPS }

data class PulseUiState(
    val screen: Screen = Screen.HOME,
    val profiles: List<VpnProfile> = emptyList(),
    val selectedProfile: VpnProfile? = null,
    val servers: List<VpnServer> = emptyList(),
    val pingHistory: Map<String, List<Int>> = emptyMap(),
    val vpnStatus: Status = Status.Stopped,
    val traffic: TrafficSnapshot = TrafficSnapshot(),
    val importing: Boolean = false,
    val testingServers: Boolean = false,
    val pingCompleted: Int = 0,
    val pingTotal: Int = 0,
    val message: String? = null,
    val darkTheme: Boolean = SettingsManager.darkTheme,
    val liveEffects: Boolean = SettingsManager.liveEffects,
    val accentTheme: String = SettingsManager.accentTheme,
    val autoConnect: Boolean = SettingsManager.autoConnect,
    val refreshOnOpen: Boolean = SettingsManager.refreshOnOpen,
    val autoFastest: Boolean = SettingsManager.autoFastest,
    val routingMode: String = SettingsManager.routingMode,
    val dnsMode: String = SettingsManager.dnsMode,
    val perAppMode: Int = SettingsManager.perAppProxyMode,
    val selectedApps: Set<String> = when (SettingsManager.perAppProxyMode) {
        SettingsManager.Keys.PER_APP_PROXY_INCLUDE -> SettingsManager.perAppProxyIncludeList.toSet()
        else -> SettingsManager.perAppProxyExcludeList.toSet()
    },
    val apps: List<ProfileRepository.AppEntry> = emptyList(),
)

class PulseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)
    private val vpn = VpnController(application)
    private val _state = MutableStateFlow(PulseUiState())
    val state: StateFlow<PulseUiState> = _state.asStateFlow()

    init {
        // Сначала показываем сохранённые серверы, обновление идёт в фоне.
        viewModelScope.launch {
            reloadInternal()
            val selected = _state.value.selectedProfile
            if (SettingsManager.refreshOnOpen && selected?.sourceUrl != null) updateProfile(selected).join()
        }
        viewModelScope.launch { vpn.status.collect { value -> _state.update { it.copy(vpnStatus = value) } } }
        viewModelScope.launch { vpn.traffic.collect { value -> _state.update { it.copy(traffic = value) } } }
        viewModelScope.launch { vpn.delays.collect { values ->
            _state.update { current -> current.copy(servers = current.servers.map { it.copy(delayMs = values[it.tag] ?: it.delayMs) }) }
        } }
        viewModelScope.launch { vpn.error.collect { it?.let(::showMessage) } }
    }

    fun navigate(screen: Screen) {
        _state.update { it.copy(screen = screen, message = null) }
        if (screen == Screen.APPS && _state.value.apps.isEmpty()) viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { repository.installedApps() }
            _state.update { it.copy(apps = apps) }
        }
    }

    fun reload(refreshRemote: Boolean = false) = viewModelScope.launch { reloadInternal(refreshRemote) }

    private suspend fun reloadInternal(refreshRemote: Boolean = false) {
        var profiles = repository.profiles()
        var selected = profiles.firstOrNull { it.id == repository.selectedId() }
        if (refreshRemote && selected?.sourceUrl != null) {
            val current = selected
            val refreshed = repository.update(current)
            if (refreshed is ImportResult.Success) {
                profiles = repository.profiles()
                selected = profiles.firstOrNull { it.id == refreshed.profile.id }
            }
        }
        if (selected != null) {
            // Rebuild the runtime config from the provider source so refreshes do not
            // silently drop routing/DNS choices made in Settings.
            repository.applyRoutingSettings(selected)
            repository.select(selected)
        }
        val history = selected?.let(repository::pingHistory).orEmpty()
        val servers = repository.servers(selected).map { server -> server.copy(delayMs = history[server.tag]?.lastOrNull()?.takeIf { it >= 0 }) }
        _state.update {
            it.copy(
                profiles = profiles,
                selectedProfile = selected,
                servers = servers,
                pingHistory = history,
            )
        }
    }

    fun import(input: String) = viewModelScope.launch {
        if (_state.value.importing) return@launch
        // Сразу показываем состояние работы, чтобы сетевой импорт не выглядел зависшим.
        _state.update { it.copy(importing = true, message = "Импортируем подписку…") }
        try { when (val result = repository.importProfile(input)) {
            is ImportResult.Success -> {
                val count = repository.servers(result.profile).size
                reloadInternal()
                _state.update {
                    it.copy(
                        importing = false,
                        screen = Screen.HOME,
                        message = "Подписка добавлена · $count серверов",
                    )
                }
            }
            is ImportResult.Error -> _state.update { it.copy(importing = false, message = result.message) }
        } } finally { _state.update { it.copy(importing = false) } }
    }

    fun updateProfile(profile: VpnProfile) = viewModelScope.launch {
        if (_state.value.importing) return@launch
        _state.update { it.copy(importing = true) }
        try {
            when (val result = repository.update(profile)) {
                is ImportResult.Success -> { reloadInternal(); showMessage("Подписка обновлена") }
                is ImportResult.Error -> showMessage(result.message)
            }
        } finally {
            _state.update { it.copy(importing = false) }
        }
    }

    fun selectProfile(profile: VpnProfile) = viewModelScope.launch {
        if (_state.value.vpnStatus != Status.Stopped) vpn.stop()
        repository.select(profile)
        repository.applyRoutingSettings(profile)
        val servers = repository.servers(profile)
        _state.update {
            it.copy(
                selectedProfile = profile,
                servers = servers,
                pingHistory = repository.pingHistory(profile),
                screen = Screen.HOME,
            )
        }
    }

    fun deleteProfile(profile: VpnProfile) = viewModelScope.launch {
        if (_state.value.selectedProfile?.id == profile.id) vpn.stop()
        repository.delete(profile)
        reload()
    }

    fun selectServer(server: VpnServer) = viewModelScope.launch {
        if (server.isInfoMetadata()) return@launch
        val profile = _state.value.selectedProfile ?: return@launch
        repository.selectServer(profile, server)
        _state.update { current -> current.copy(servers = current.servers.map { it.copy(selected = it.tag == server.tag) }) }
        if (_state.value.vpnStatus == Status.Started) vpn.select("Proxy", server.tag)
        showMessage("Маршрут: ${server.tag}")
    }

    fun testServers() = viewModelScope.launch {
        if (_state.value.testingServers || _state.value.importing) return@launch
        val owner = _state.value.selectedProfile ?: return@launch
        val snapshot = _state.value.servers.filterNot(VpnServer::isInfoMetadata)
        if (snapshot.isEmpty()) return@launch
        _state.update { it.copy(testingServers = true, pingCompleted = 0, pingTotal = snapshot.size) }
        try {
            val measured = if (_state.value.vpnStatus == Status.Started) {
                val previous = vpn.delays.value
                withTimeoutOrNull(15_000) {
                    vpn.urlTest("Proxy")
                    vpn.delays.first { it.isNotEmpty() && it != previous }
                }?.let { values -> snapshot.map { it.copy(delayMs = values[it.tag]) } }
                    ?: run { showMessage("Проверка завершена без новых результатов. Попробуйте ещё раз."); return@launch }
            } else {
                val limit = Semaphore(6)
                coroutineScope {
                    snapshot.map { server ->
                        async(Dispatchers.IO) {
                            limit.withPermit {
                                val latency = if (server.address != null && server.port != null) {
                                    val addresses = runCatching { InetAddress.getAllByName(server.address).take(4) }.getOrDefault(emptyList())
                                    suspend fun probe(): Int? = coroutineScope {
                                        addresses.map { address ->
                                            async(Dispatchers.IO) {
                                                runCatching {
                                                    val started = System.nanoTime()
                                                    Socket().use { it.connect(InetSocketAddress(address, server.port), 1200) }
                                                    ((System.nanoTime() - started) / 1_000_000).toInt()
                                                }.getOrNull()
                                            }
                                        }.awaitAll().filterNotNull().minOrNull()
                                    }
                                    probe() ?: run { delay(120); probe() }
                                } else null
                                _state.update { it.copy(pingCompleted = (it.pingCompleted + 1).coerceAtMost(it.pingTotal)) }
                                server.copy(delayMs = latency)
                            }
                        }
                    }.awaitAll()
                }
            }
            // A profile can change while network probes are running.
            if (_state.value.selectedProfile?.id != owner.id) return@launch
            val sorted = measured.sortedWith(compareBy<VpnServer> { it.delayMs == null }.thenBy { it.delayMs ?: Int.MAX_VALUE })
            val fastest = sorted.firstOrNull { it.delayMs != null }
            val autoSelect = SettingsManager.autoFastest && fastest != null
            if (autoSelect) {
                repository.selectServer(owner, fastest!!)
                if (_state.value.vpnStatus == Status.Started) vpn.select("Proxy", fastest.tag)
            }
            val history = _state.value.pingHistory.toMutableMap()
            sorted.forEach { server -> history[server.tag] = (history[server.tag].orEmpty() + (server.delayMs ?: -1)).takeLast(20) }
            withContext(Dispatchers.IO) { repository.savePingHistory(owner, history) }
            _state.update { current ->
                if (current.selectedProfile?.id != owner.id) current else current.copy(
                    servers = sorted.map { it.copy(selected = it.tag == (if (autoSelect) fastest?.tag else current.servers.firstOrNull(VpnServer::selected)?.tag)) },
                    pingHistory = history,
                    pingCompleted = sorted.size,
                    message = "Доступно ${sorted.count { it.delayMs != null }} из ${sorted.size} серверов",
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            showMessage("Не удалось завершить проверку. Попробуйте ещё раз.")
        } finally {
            _state.update { it.copy(testingServers = false) }
        }
    }

    fun startVpn() {
        if (_state.value.selectedProfile == null) return showMessage("Сначала добавьте подписку")
        vpn.start()
    }

    fun stopVpn() = vpn.stop()

    fun setDarkTheme(value: Boolean) {
        SettingsManager.darkTheme = value
        _state.update { it.copy(darkTheme = value) }
    }

    fun setLiveEffects(value: Boolean) {
        SettingsManager.liveEffects = value
        _state.update { it.copy(liveEffects = value) }
    }

    fun setAccentTheme(value: String) {
        SettingsManager.accentTheme = value
        _state.update { it.copy(accentTheme = value) }
    }

    fun setAutoConnect(value: Boolean) {
        SettingsManager.autoConnect = value
        _state.update { it.copy(autoConnect = value) }
    }

    fun setRefreshOnOpen(value: Boolean) {
        SettingsManager.refreshOnOpen = value
        _state.update { it.copy(refreshOnOpen = value) }
    }

    fun setAutoFastest(value: Boolean) {
        SettingsManager.autoFastest = value
        _state.update { it.copy(autoFastest = value) }
    }

    fun refreshSubscriptions() = viewModelScope.launch {
        if (_state.value.importing) return@launch
        val remote = _state.value.profiles.filter { it.sourceUrl != null }
        if (remote.isEmpty()) return@launch showMessage("Нет удалённых подписок для обновления")
        _state.update { it.copy(importing = true) }
        var updated = 0
        try {
            remote.forEach { profile ->
                when (repository.update(profile)) {
                    is ImportResult.Success -> updated++
                    is ImportResult.Error -> Unit
                }
            }
            reloadInternal()
            showMessage("Обновлено подписок: $updated из ${remote.size}")
        } finally {
            _state.update { it.copy(importing = false) }
        }
    }

    fun setRoutingMode(value: String) = viewModelScope.launch {
        SettingsManager.routingMode = value
        _state.value.selectedProfile?.let { repository.applyRoutingSettings(it) }
        if (_state.value.vpnStatus == Status.Started) { vpn.stop(); showMessage("Переподключитесь, чтобы применить маршрутизацию") }
        _state.update { it.copy(routingMode = value) }
    }

    fun setDnsMode(value: String) = viewModelScope.launch {
        SettingsManager.dnsMode = value
        _state.value.selectedProfile?.let { repository.applyRoutingSettings(it) }
        if (_state.value.vpnStatus == Status.Started) vpn.stop()
        _state.update { it.copy(dnsMode = value) }
        showMessage("DNS сохранён. Переподключитесь для применения")
    }

    fun setPerAppMode(value: Int) {
        SettingsManager.perAppProxyMode = value
        val saved = when (value) {
            SettingsManager.Keys.PER_APP_PROXY_INCLUDE -> SettingsManager.perAppProxyIncludeList
            else -> SettingsManager.perAppProxyExcludeList
        }.toSet()
        _state.update { it.copy(perAppMode = value, selectedApps = saved) }
    }

    fun toggleApp(packageName: String) {
        if (_state.value.perAppMode == SettingsManager.Keys.PER_APP_PROXY_DISABLED) return
        val selected = _state.value.selectedApps.toMutableSet().apply { if (!add(packageName)) remove(packageName) }
        SettingsManager.setPerAppList(_state.value.perAppMode, selected)
        _state.update { it.copy(selectedApps = selected) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
    private fun showMessage(value: String) {
        // INFO/provider metadata is never a route. Do not surface stale selections from
        // older profile files as a route notification.
        if (value.contains("INFO", ignoreCase = true) && value.startsWith("Маршрут")) return
        _state.update { it.copy(message = value) }
    }
    fun coreVersion(): String = runCatching { Libbox.version() }.getOrDefault("sing-box")

    override fun onCleared() {
        vpn.close()
        super.onCleared()
    }
}
