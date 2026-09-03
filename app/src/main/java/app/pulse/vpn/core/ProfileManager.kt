package app.pulse.vpn.core

import com.tencent.mmkv.MMKV
import java.io.File

/** Минимальный многопроцессный мост между UI и VPN-процессом. */
object ProfileManager {
    const val MMKV_ID = "pulse_runtime_profile"
    private const val KEY_NAME = "name"
    private const val KEY_DIRECTORY = "directory"

    private val storage by lazy { MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE) }

    data class RuntimeProfile(val name: String)

    fun select(name: String, configFile: File) {
        require(configFile.isFile && configFile.length() > 0) { "Пустая конфигурация" }
        storage.encode(KEY_NAME, name)
        storage.encode(KEY_DIRECTORY, configFile.parentFile!!.absolutePath)
        storage.sync()
    }

    fun clear() {
        storage.removeValuesForKeys(arrayOf(KEY_NAME, KEY_DIRECTORY))
        storage.sync()
    }

    fun getSelectedProfile(): RuntimeProfile? {
        val name = storage.decodeString(KEY_NAME)?.takeIf { it.isNotBlank() } ?: return null
        return RuntimeProfile(name)
    }

    fun getUsingConfig(): File {
        val directory = storage.decodeString(KEY_DIRECTORY).orEmpty()
        return if (directory.isBlank()) File("/non-existent") else File(directory, "using_config.json")
    }
}
