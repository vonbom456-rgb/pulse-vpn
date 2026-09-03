package app.pulse.vpn

import android.app.Application
import app.pulse.vpn.core.PluginManager
import com.tencent.mmkv.MMKV

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        PluginManager.init(this)
    }
}
