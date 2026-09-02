package app.pulse.vpn.pulse_vpn

import android.os.Bundle
import com.clashsing.flutter_sing_box.FlutterSingBoxPlugin
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // flutter_sing_box 1.1.5 иногда не получает ActivityAware callback.
        FlutterSingBoxPlugin.attachHostActivity(this)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        FlutterSingBoxPlugin.detachHostActivity(this)
        super.onDestroy()
    }
}
