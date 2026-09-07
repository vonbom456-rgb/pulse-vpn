package app.pulse.vpn.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

data class SubscriptionIdentity(
    val hwid: String = "",
    val deviceOs: String = "Android",
    val osVersion: String = "",
    val deviceModel: String = "",
)

class SubscriptionIdentityProvider(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("pulse_device_identity", Context.MODE_PRIVATE)

    fun current(): SubscriptionIdentity {
        val seed = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: preferences.getString(KEY_FALLBACK_ID, null)
            ?: UUID.randomUUID().toString().replace("-", "").also {
                preferences.edit().putString(KEY_FALLBACK_ID, it).apply()
            }
        return SubscriptionIdentity(
            hwid = sha256(seed).take(32),
            osVersion = ascii(Build.VERSION.RELEASE, "Android"),
            deviceModel = ascii(
                listOf(Build.MANUFACTURER, Build.MODEL)
                    .filter(String::isNotBlank)
                    .joinToString(" ")
                    .trim(),
                "Android device",
            ).take(64),
        )
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun ascii(value: String, fallback: String): String =
        value.filter { it.code in 32..126 }.ifBlank { fallback }

    private companion object {
        const val KEY_FALLBACK_ID = "fallback_hwid_seed"
    }
}
