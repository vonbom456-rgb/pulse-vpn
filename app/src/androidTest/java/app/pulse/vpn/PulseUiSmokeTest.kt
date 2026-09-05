package app.pulse.vpn

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File
import app.pulse.vpn.data.RuntimeSettings
import app.pulse.vpn.data.SubscriptionImporter
import io.nekohasekai.libbox.Libbox
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Uses only synthetic metadata and example.invalid hosts; no subscription secrets. */
class PulseUiSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun nativeCoreAcceptsRoutingAndDnsConfigurations() = runBlocking {
        val imported = SubscriptionImporter().import("vless://11111111-1111-4111-8111-111111111111@example.invalid:443?security=tls#Test")
        val source = Json.parseToJsonElement(imported.config).jsonObject
        listOf("rules", "global", "direct").forEach { mode ->
            listOf("local", "cloudflare", "google").forEach { dns ->
                Libbox.checkConfig(RuntimeSettings.apply(source, "Test", mode, dns).toString())
            }
        }
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun importsDescriptionAndNavigatesEveryTheme() {
        compose.runOnIdle { ViewModelProvider(compose.activity)[PulseViewModel::class.java].setLiveEffects(false) }
        compose.onNodeWithText("Добавить подписку").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("""
            #profile-title: Pulse Demo
            #announce: Welcome to Pulse. Your subscription details stay here.
            #support-url: https://t.me/pulse_demo
            #profile-web-page-url: https://example.invalid
            #subscription-userinfo: upload=0; download=1073741824; total=10737418240; expire=1893456000
            vless://11111111-1111-4111-8111-111111111111@fi.example.invalid:443?security=tls#Finland
            vless://11111111-1111-4111-8111-111111111111@nl.example.invalid:443?security=tls#Netherlands
        """.trimIndent())
        compose.onNodeWithText("Импортировать").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ВАША ПОДПИСКА").fetchSemanticsNodes().isNotEmpty() }
        snapshot("01-home-pulse")
        compose.onNodeWithText("Welcome to Pulse. Your subscription details stay here.").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Сообщение провайдера").assertIsDisplayed()
        snapshot("02-provider-description")
        compose.onNodeWithText("Понятно").performClick()
        snapshot("03-subscription-card")
        compose.onNodeWithText("Настройки").performClick()
        snapshot("04-settings-pulse")
        listOf("Ocean", "Ember", "Midnight", "Mono", "Из подписки", "Pulse").forEachIndexed { index, theme ->
            compose.onNodeWithText(theme, useUnmergedTree = true).performScrollTo().performClick()
            snapshot("theme-$index")
        }
        compose.onNodeWithText("Тёмное оформление").performScrollTo().performClick()
        snapshot("05-light-settings")
        compose.onNodeWithText("Главная").performClick()
        snapshot("06-light-home")
        compose.onNodeWithText("Маршруты").performClick()
        compose.onNodeWithText("Finland").assertIsDisplayed()
        snapshot("07-routes")
    }
}
