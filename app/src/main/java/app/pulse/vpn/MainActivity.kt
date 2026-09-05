package app.pulse.vpn

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pulse.vpn.ui.PulseApp
import app.pulse.vpn.ui.PulseTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PulseViewModel>()
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.startVpn()
        else viewModel.connectionFailed("Разрешение VPN не выдано. Нажмите «Повторить» и подтвердите запрос Android.")
    }
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf(String::isNotBlank)?.let(viewModel::import)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            androidx.compose.runtime.SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.darkTheme
                    isAppearanceLightNavigationBars = !state.darkTheme
                }
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
            }
            LaunchedEffect(state.options, state.vpnStatus) {
                val secure = android.view.WindowManager.LayoutParams.FLAG_SECURE
                val awake = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (state.options.bool("secure_screen")) window.addFlags(secure) else window.clearFlags(secure)
                if (state.options.bool("keep_screen") && state.vpnStatus == io.nekohasekai.sfa.constant.Status.Started) window.addFlags(awake) else window.clearFlags(awake)
            }
            LaunchedEffect(state.darkTheme) {
                val bars = if (state.darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            val activeAccent = if (state.accentTheme == "profile") {
                state.selectedProfile?.themeHint ?: "pulse"
            } else state.accentTheme
            PulseTheme(dark = state.darkTheme, accent = activeAccent) {
                PulseApp(
                    viewModel = viewModel,
                    requestConnect = {
                        val intent = VpnService.prepare(this)
                        if (intent == null) viewModel.startVpn() else vpnPermission.launch(intent)
                    },
                    scanQr = {
                        scanner.launch(ScanOptions().apply {
                            setPrompt("Наведите камеру на QR подписки")
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                        })
                    },
                    openVpnSettings = {
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_VPN_SETTINGS))
                    },
                )
            }
        }
    }
}
