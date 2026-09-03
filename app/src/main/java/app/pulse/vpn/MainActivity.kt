package app.pulse.vpn

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.pulse.vpn.ui.PulseApp
import app.pulse.vpn.ui.PulseTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PulseViewModel>()
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.startVpn()
    }
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf(String::isNotBlank)?.let(viewModel::import)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsState()
            PulseTheme(dark = state.darkTheme) {
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
