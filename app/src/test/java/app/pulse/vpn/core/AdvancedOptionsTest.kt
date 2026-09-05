package app.pulse.vpn.core

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdvancedOptionsTest(private val key: String, private val value: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}={1}")
        fun cases(): List<Array<String>> = OptionCatalog.all.flatMap { spec ->
            (if (spec.toggle) listOf("true", "false") else spec.choices.map { it.first }).map { arrayOf(spec.key, it) }
        }
    }
    @Test fun validChoiceSurvivesSerialization() {
        val changed = AdvancedOptions().with(key, value)
        val restored = Json.decodeFromString<AdvancedOptions>(Json.encodeToString(AdvancedOptions.serializer(), changed))
        assertEquals(value, restored.text(key))
    }
    @Test fun invalidStoredChoiceFallsBackToDefault() {
        assertEquals(OptionCatalog.byKey.getValue(key).default, AdvancedOptions(mapOf(key to "invalid-value")).text(key))
    }
    @Test fun rejectsInvalidInputWithoutChangingOtherOptions() {
        val original = AdvancedOptions().with(key, value)
        assertThrows(IllegalArgumentException::class.java) { original.with(key, "invalid-value") }
        assertEquals(value, original.text(key))
    }
}

