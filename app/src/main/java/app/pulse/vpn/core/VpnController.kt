package app.pulse.vpn.core

import android.content.Context
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OutboundGroup
import io.nekohasekai.libbox.StatusMessage
import io.nekohasekai.sfa.bg.BoxService
import io.nekohasekai.sfa.bg.ServiceConnection
import io.nekohasekai.sfa.constant.Alert
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.utils.CommandClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrafficSnapshot(
    val uploadPerSecond: Long = 0,
    val downloadPerSecond: Long = 0,
    val uploadTotal: Long = 0,
    val downloadTotal: Long = 0,
)

class VpnController(context: Context) : ServiceConnection.Callback, CommandClient.Handler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connection = ServiceConnection(context.applicationContext, this)
    private val commandClient = CommandClient(scope, listOf(CommandClient.ConnectionType.Status, CommandClient.ConnectionType.Groups), this)
    private val _status = MutableStateFlow(Status.Stopped)
    private val _traffic = MutableStateFlow(TrafficSnapshot())
    private val _delays = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _error = MutableStateFlow<String?>(null)

    val status: StateFlow<Status> = _status.asStateFlow()
    val traffic: StateFlow<TrafficSnapshot> = _traffic.asStateFlow()
    val delays: StateFlow<Map<String, Int>> = _delays.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    init { connection.connect() }

    fun start() {
        _error.value = null
        BoxService.start()
    }

    fun stop() = BoxService.stop()

    fun select(groupTag: String, outboundTag: String) = runCatching {
        Libbox.newStandaloneCommandClient().selectOutbound(groupTag, outboundTag)
        commandClient.connect()
    }.onFailure { _error.value = "Не удалось переключить маршрут" }

    fun urlTest(groupTag: String) = runCatching {
        Libbox.newStandaloneCommandClient().urlTest(groupTag)
        commandClient.connect()
    }.onFailure { _error.value = "Тест задержки недоступен" }

    override fun onServiceStatusChanged(status: Status) {
        _status.value = status
        if (status == Status.Started) commandClient.connect() else commandClient.disconnect()
    }

    override fun onServiceAlert(type: Alert, message: String?) {
        _status.value = Status.Stopped
        _error.value = when (type) {
            Alert.EmptyConfiguration -> "Сначала добавьте и выберите профиль"
            Alert.RequestLocationPermission -> "Для этой конфигурации требуется доступ к геолокации"
            else -> "VPN не запустился: ${message ?: type.name}"
        }
    }

    override fun updateStatus(status: StatusMessage) {
        _traffic.value = TrafficSnapshot(status.uplink, status.downlink, status.uplinkTotal, status.downlinkTotal)
    }

    override fun updateGroups(newGroups: MutableList<OutboundGroup>) {
        val values = mutableMapOf<String, Int>()
        newGroups.forEach { group ->
            val items = group.items
            while (items.hasNext()) {
                val item = items.next()
                if (item.urlTestDelay > 0) values[item.tag] = item.urlTestDelay
            }
        }
        _delays.value = values
    }

    fun close() {
        commandClient.disconnect()
        connection.disconnect()
        scope.cancel()
    }
}
