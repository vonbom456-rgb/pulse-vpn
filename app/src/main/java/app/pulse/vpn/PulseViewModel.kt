package app.pulse.vpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pulse.vpn.core.SettingsManager
import app.pulse.vpn.core.TrafficSnapshot
import app.pulse.vpn.core.VpnController
import app.pulse.vpn.data.ImportResult
import app.pulse.vpn.data.ImportSummary
import app.pulse.vpn.data.ProfileRepository
import app.pulse.vpn.data.VpnProfile
import app.pulse.vpn.data.VpnServer
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class Screen { HOME, ROUTES, STATS, SETTINGS, PROFILES, APPS }

data class PulseUiState(
    val screen: Screen = Screen.HOME,
    val profiles: List<VpnProfile> = emptyList(),
    val selectedProfile: VpnProfile? = null,
    val servers: List<VpnServer> = emptyList(),
    val vpnStatus: Status = Status.Stopped,
    val traffic: TrafficSnapshot = TrafficSnapshot(),
    val importing: Boolean = false,
    val message: String? = null,
    val darkTheme: Boolean = SettingsManager.darkTheme,
    val autoConnect: Boolean = SettingsManager.autoConnect,
    val refreshOnOpen: Boolean = SettingsManager.refreshOnOpen,
    val routingMode: String = SettingsManager.routingMode,
    val dnsMode: String = SettingsManager.dnsMode,
    val perAppMode: Int = SettingsManager.perAppProxyMode,
    val selectedApps: Set<String> = when (SettingsManager.perAppProxyMode) {
        SettingsManager.Keys.PER_APP_PROXY_INCLUDE -> SettingsManager.perAppProxyIncludeList.toSet()
        else -> SettingsManager.perAppProxyExcludeList.toSet()
    },
    val apps: List<ProfileRepository.AppEntry> = emptyList(),
    val importSummary: ImportSummary? = null,
)

class PulseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)
    private val vpn = VpnController(application)
    private val _state = MutableStateFlow(PulseUiState())
    val state: StateFlow<PulseUiState> = _state.asStateFlow()

    init {
        reload(refreshRemote = SettingsManager.refreshOnOpen)
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

    fun reload(refreshRemote: Boolean = false) = viewModelScope.launch {
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
        if (selected != null) repository.select(selected)
        val servers = repository.servers(selected)
        _state.update { it.copy(profiles = profiles, selectedProfile = selected, servers = servers) }
    }

    fun import(input: String) = viewModelScope.launch {
        _state.update { it.copy(importing = true, message = null) }
        when (val result = repository.importProfile(input)) {
            is ImportResult.Success -> {
                val count = repository.servers(result.profile).size
                reload()
                _state.update {
                    it.copy(
                        importing = false,
                        screen = Screen.HOME,
                        message = null,
                        importSummary = ImportSummary(
                            result.profile,
                            count,
                            if (result.profile.sourceUrl != null) "Удалённая подписка" else "Локальная конфигурация",
                        ),
                    )
                }
            }
            is ImportResult.Error -> _state.update { it.copy(importing = false, message = result.message) }
        }
    }

    fun updateProfile(profile: VpnProfile) = viewModelScope.launch {
        _state.update { it.copy(importing = true) }
        when (val result = repository.update(profile)) {
            is ImportResult.Success -> { reload(); showMessage("Подписка обновлена") }
            is ImportResult.Error -> showMessage(result.message)
        }
        _state.update { it.copy(importing = false) }
    }

    fun selectProfile(profile: VpnProfile) = viewModelScope.launch {
        if (_state.value.vpnStatus == Status.Started) vpn.stop()
        repository.select(profile)
        val servers = repository.servers(profile)
        _state.update { it.copy(selectedProfile = profile, servers = servers, screen = Screen.HOME) }
    }

    fun deleteProfile(profile: VpnProfile) = viewModelScope.launch {
        if (_state.value.selectedProfile?.id == profile.id) vpn.stop()
        repository.delete(profile)
        reload()
    }

    fun selectServer(server: VpnServer) = viewModelScope.launch {
        val profile = _state.value.selectedProfile ?: return@launch
        repository.selectServer(profile, server)
        _state.update { current -> current.copy(servers = current.servers.map { it.copy(selected = it.tag == server.tag) }) }
        if (_state.value.vpnStatus == Status.Started) vpn.select("Proxy", server.tag)
        showMessage("Маршрут: ${server.tag}")
    }

    fun testServers() = viewModelScope.launch {
        if (_state.value.vpnStatus == Status.Started) {
            vpn.urlTest("Proxy")
            return@launch
        }
        val measured = withContext(Dispatchers.IO) {
            _state.value.servers.map { server ->
                val delay = if (server.address != null && server.port != null) runCatching {
                    val started = System.nanoTime()
                    Socket().use { it.connect(InetSocketAddress(server.address, server.port), 1600) }
                    ((System.nanoTime() - started) / 1_000_000).toInt()
                }.getOrNull() else null
                server.copy(delayMs = delay)
            }
        }
        _state.update { it.copy(servers = measured) }
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

    fun setAutoConnect(value: Boolean) {
        SettingsManager.autoConnect = value
        _state.update { it.copy(autoConnect = value) }
    }

    fun setRefreshOnOpen(value: Boolean) {
        SettingsManager.refreshOnOpen = value
        _state.update { it.copy(refreshOnOpen = value) }
    }

    fun refreshSubscriptions() = viewModelScope.launch {
        val remote = _state.value.profiles.filter { it.sourceUrl != null }
        if (remote.isEmpty()) return@launch showMessage("Нет удалённых подписок для обновления")
        _state.update { it.copy(importing = true) }
        remote.forEach { profile ->
            when (repository.update(profile)) {
                is ImportResult.Success -> Unit
                is ImportResult.Error -> Unit
            }
        }
        reload()
        _state.update { it.copy(importing = false) }
        showMessage("Подписки обновлены")
    }

    fun clearImportSummary() = _state.update { it.copy(importSummary = null) }

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
        SettingsManager.setPerAppList(value, _state.value.selectedApps)
        _state.update { it.copy(perAppMode = value) }
    }

    fun toggleApp(packageName: String) {
        val selected = _state.value.selectedApps.toMutableSet().apply { if (!add(packageName)) remove(packageName) }
        SettingsManager.setPerAppList(_state.value.perAppMode, selected)
        _state.update { it.copy(selectedApps = selected) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
    private fun showMessage(value: String) = _state.update { it.copy(message = value) }
    fun coreVersion(): String = runCatching { Libbox.version() }.getOrDefault("sing-box")

    override fun onCleared() {
        vpn.close()
        super.onCleared()
    }
}
