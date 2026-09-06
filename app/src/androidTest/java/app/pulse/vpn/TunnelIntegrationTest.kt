package app.pulse.vpn

import android.net.VpnService
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.pulse.vpn.core.AdvancedOptions
import app.pulse.vpn.core.SettingsManager
import io.nekohasekai.sfa.constant.Status
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Isolated emulator fixture. An unroutable test destination can answer only when
 * Android's TUN and sing-box send it through our loopback HTTP CONNECT proxy.
 * No provider, credentials or external VPN service participates in this test.
 */
class TunnelIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun actualTunRoutesSwitchAndSettingsReconnect() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("appops set app.pulse.vpn ACTIVATE_VPN allow").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        assertNull("VPN test permission missing", VpnService.prepare(compose.activity))
        lateinit var model: PulseViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[PulseViewModel::class.java] }
        val oldOptions = SettingsManager.advanced
        val oldRouting = SettingsManager.routingMode
        val oldDns = SettingsManager.dnsMode
        val oldMode = SettingsManager.perAppProxyMode
        val oldInclude = SettingsManager.perAppProxyIncludeList.toSet()
        val oldExclude = SettingsManager.perAppProxyExcludeList.toSet()
        LoopbackProxy("route-a").use { first ->
            LoopbackProxy("route-b").use { second ->
                try {
                    compose.runOnIdle {
                        model.setLiveEffects(false)
                        model.resetAdvanced()
                        model.setPerAppMode(SettingsManager.Keys.PER_APP_PROXY_DISABLED)
                        model.setRoutingMode("global")
                        model.setDnsMode("local")
                    }
                    compose.waitUntil(5_000) { model.state.value.routingMode == "global" && model.state.value.dnsMode == "local" }
                    val completed = model.state.value.importCompleted
                    compose.runOnIdle { model.import("""
                        {"profile-title":"Isolated tunnel test","log":{"disabled":true},"outbounds":[
                        {"type":"http","tag":"Route A","server":"127.0.0.1","server_port":${first.port}},
                        {"type":"http","tag":"Route B","server":"127.0.0.1","server_port":${second.port}}]}
                    """.trimIndent()) }
                    compose.waitUntil(15_000) { model.state.value.importCompleted > completed }
                    compose.runOnIdle { model.startVpn() }
                    awaitStarted(model)
                    assertEquals("route-a", requestThroughTun())
                    compose.runOnIdle { model.selectServer(model.state.value.servers.first { it.tag == "Route B" }) }
                    compose.waitUntil(8_000) { model.state.value.servers.any { it.tag == "Route B" && it.selected } }
                    assertEquals("route-b", requestThroughTun())
                    compose.runOnIdle { model.setOption("mtu", "1400") }
                    compose.waitUntil(5_000) { model.state.value.settingsPending }
                    compose.runOnIdle { model.reconnect() }
                    compose.waitUntil(15_000) { model.state.value.vpnStatus == Status.Started && !model.state.value.settingsPending }
                    assertEquals("route-b", requestThroughTun())
                    // Include-only mode must keep selected app traffic usable.
                    compose.runOnIdle {
                        model.setPerAppMode(SettingsManager.Keys.PER_APP_PROXY_INCLUDE)
                        if ("app.pulse.vpn" !in model.state.value.selectedApps) model.toggleApp("app.pulse.vpn")
                        model.reconnect()
                    }
                    compose.waitUntil(15_000) { model.state.value.vpnStatus == Status.Started && !model.state.value.settingsPending }
                    assertEquals("route-b", requestThroughTun())
                    compose.runOnIdle { model.stopVpn() }
                    compose.waitUntil(10_000) { model.state.value.vpnStatus == Status.Stopped }
                    assertEquals(0L, model.state.value.traffic.downloadPerSecond)
                    // Stop while the start job is still entering the native core.
                    compose.runOnIdle { model.startVpn() }
                    compose.waitUntil(5_000) { model.state.value.vpnStatus in listOf(Status.Starting, Status.Started) }
                    compose.runOnIdle { model.stopVpn() }
                    compose.waitUntil(10_000) { model.state.value.vpnStatus == Status.Stopped }
                } finally {
                    compose.runOnIdle { model.stopVpn() }
                    compose.waitUntil(10_000) { model.state.value.vpnStatus == Status.Stopped }
                    SettingsManager.advanced = oldOptions
                    SettingsManager.routingMode = oldRouting
                    SettingsManager.dnsMode = oldDns
                    SettingsManager.setPerAppList(SettingsManager.Keys.PER_APP_PROXY_INCLUDE, oldInclude)
                    SettingsManager.setPerAppList(SettingsManager.Keys.PER_APP_PROXY_EXCLUDE, oldExclude)
                    SettingsManager.perAppProxyMode = oldMode
                }
            }
        }
    }

    private fun awaitStarted(model: PulseViewModel) {
        compose.waitUntil(15_000) { model.state.value.vpnStatus == Status.Started || model.state.value.connectionError != null }
        assertNull("VPN startup failed", model.state.value.connectionError)
        assertEquals(Status.Started, model.state.value.vpnStatus)
        // Android needs a moment to publish UID routing after establish().
        Thread.sleep(300)
    }

    private fun requestThroughTun(): String = Socket().use { socket ->
        socket.soTimeout = 6000
        socket.connect(InetSocketAddress("198.18.0.20", 80), 6000)
        socket.getOutputStream().write("GET /pulse-test HTTP/1.1\r\nHost: 198.18.0.20\r\nConnection: close\r\n\r\n".toByteArray())
        val response = socket.getInputStream().bufferedReader().readText()
        check(response.startsWith("HTTP/1.1 200")) { "No fixture response through VPN" }
        response.substringAfter("\r\n\r\n")
    }

    private class LoopbackProxy(private val marker: String) : AutoCloseable {
        private val listener = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        private val workers = Executors.newFixedThreadPool(4)
        val port: Int get() = listener.localPort
        private val acceptor = Thread {
            while (!listener.isClosed) {
                val socket = runCatching { listener.accept() }.getOrNull() ?: break
                workers.submit {
                    socket.use {
                        runCatching {
                            it.soTimeout = 6000
                            val input = it.getInputStream()
                            fun header(): String {
                                val value = StringBuilder()
                                while (value.length < 8192 && !value.endsWith("\r\n\r\n")) {
                                    val next = input.read()
                                    check(next >= 0) { "Incomplete fixture header" }
                                    value.append(next.toChar())
                                }
                                return value.toString()
                            }
                            val target = header()
                            if (!target.startsWith("CONNECT 198.18.0.20:80 ")) return@runCatching
                            it.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                            check(header().startsWith("GET /pulse-test "))
                            it.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: ${marker.length}\r\nConnection: close\r\n\r\n$marker".toByteArray())
                        }
                    }
                }
            }
        }.apply { isDaemon = true; start() }
        override fun close() { listener.close(); workers.shutdownNow(); acceptor.join(2000) }
    }
}
