package app.pulse.vpn

import android.graphics.Bitmap
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
        val unavailable = SubscriptionImporter().import("""{"outbounds":[{"type":"vless","tag":"Device limit reached","server":"error.example.invalid","server_port":443}]}""")
        Libbox.checkConfig(unavailable.config)
        listOf("rules", "global", "direct").forEach { mode ->
            listOf("local", "cloudflare", "google", "quad9", "adguard").forEach { dns ->
                Libbox.checkConfig(RuntimeSettings.apply(source, "Test", mode, dns).toString())
            }
        }
        app.pulse.vpn.core.OptionCatalog.all.filter { it.reconnect }.forEach { spec ->
            (if (spec.toggle) listOf("true", "false") else spec.choices.map { it.first }).forEach { value ->
                val options = app.pulse.vpn.core.AdvancedOptions().with(spec.key, value)
                Libbox.checkConfig(RuntimeSettings.apply(source, "Test", "rules", "cloudflare", options).toString())
            }
        }
    }

    private fun snapshot(name: String) {
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        // UIAutomation captures the display, so allow its compositor to present the frame.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(400)
        val image = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun importsDescriptionAndNavigatesEveryTheme() {
        lateinit var model: PulseViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[PulseViewModel::class.java]; model.setLiveEffects(false) }
        compose.onNodeWithText("Добавить подписку").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("not-a-profile")
        compose.onNodeWithText("Импортировать").performClick()
        compose.waitUntil(15_000) { model.state.value.importError != null }
        compose.onNode(hasSetTextAction()).assertTextContains("not-a-profile").performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("""
            #profile-title: Pulse Demo — very long subscription name for accessibility and compact layout review
            #announce: Welcome to Pulse. Your subscription details stay here.
            #support-url: https://t.me/pulse_demo
            #profile-web-page-url: https://example.invalid
            #subscription-userinfo: upload=0; download=1073741824; total=10737418240; expire=1893456000
            vless://11111111-1111-4111-8111-111111111111@fi.example.invalid:443?security=tls#Finland
            vless://11111111-1111-4111-8111-111111111111@nl.example.invalid:443?security=tls#Netherlands
        """.trimIndent())
        compose.onNodeWithText("Импортировать").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ВАША ПОДПИСКА").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { model.clearMessage() }
        compose.onNodeWithText("Обновить").assertIsDisplayed()
        compose.onNodeWithText("Поддержка").assertIsDisplayed()
        snapshot("01-home-pulse")
        compose.onNodeWithText("Welcome to Pulse. Your subscription details stay here.").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("О подписке").assertIsDisplayed()
        snapshot("02-provider-description")
        compose.onNodeWithText("Понятно").performClick()
        snapshot("03-subscription-card")
        compose.onNodeWithContentDescription("Свернуть подписку").performScrollTo().performClick()
        compose.onNodeWithText("Welcome to Pulse. Your subscription details stay here.").assertDoesNotExist()
        compose.onNodeWithText("Поддержка").assertDoesNotExist()
        compose.onNodeWithText("Срок подписки").assertDoesNotExist()
        compose.onNodeWithText("СЕРВЕРЫ").assertDoesNotExist()
        snapshot("03a-collapsed-subscription")
        compose.onNodeWithContentDescription("Развернуть подписку").performClick()
        compose.onNodeWithText("Поддержка").assertExists()
        compose.onNodeWithText("СЕРВЕРЫ").assertExists()
        compose.onNodeWithContentDescription("Сменить сервер").performScrollTo().performClick()
        compose.onNodeWithText("Быстрый выбор").assertIsDisplayed()
        snapshot("03b-quick-server-picker")
        compose.onNodeWithTag("quick-server:Netherlands").performClick()
        compose.waitUntil(5_000) { model.state.value.servers.any { it.tag == "Netherlands" && it.selected } }
        compose.onNodeWithText("Настройки").performClick()
        snapshot("04-settings-pulse")
        compose.onNodeWithText("DNS и сеть").performScrollTo().performClick()
        compose.onNodeWithText("Кэш DNS").performScrollTo().assertIsDisplayed()
        snapshot("04b-dns-settings")
        compose.onNodeWithText("Поиск настроек").performScrollTo()
        compose.onNodeWithText("Поиск настроек").performTextInput("Цветовая тема")
        listOf("Ocean", "Ember", "Midnight", "Mono", "Из подписки", "Pulse").forEachIndexed { index, theme ->
            compose.onNodeWithText(theme, useUnmergedTree = true).performScrollTo().performClick()
            snapshot("theme-$index")
        }
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.onNodeWithText("Поиск настроек").performTextInput("Тёмное оформление")
        compose.onNode(hasText("Тёмное оформление") and isToggleable()).performScrollTo().performClick()
        snapshot("05-light-settings")
        compose.onNodeWithText("Главная").performClick()
        snapshot("06-light-home")
        compose.onNodeWithText("Маршруты").performClick()
        compose.onNodeWithText("Finland").assertIsDisplayed()
        snapshot("07-routes")
        compose.onNodeWithContentDescription("В избранное: Finland").performClick()
        compose.onNodeWithText("Избранные").performClick()
        compose.onNodeWithContentDescription("В избранном: Finland").assertIsDisplayed()
        compose.onNodeWithText("Настройки").performClick()
        compose.onNodeWithText("Поиск настроек").performTextInput("MTU")
        compose.onNodeWithText("Размер пакета MTU").performClick()
        compose.onNodeWithText("1400").performClick()
        compose.onNodeWithText("1400").assertIsDisplayed()
        snapshot("08-advanced-search")
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.runOnIdle { ViewModelProvider(compose.activity)[PulseViewModel::class.java].connectionFailed("Тестовое сообщение: проверьте интернет или смените сервер.") }
        compose.runOnIdle { ViewModelProvider(compose.activity)[PulseViewModel::class.java].navigate(Screen.HOME) }
        compose.onNodeWithText("Повторить").performScrollTo().assertIsDisplayed()
        snapshot("09-recovery")
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) { automation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() } }
        try {
            shell("wm size 720x1280")
            shell("wm density 320")
            shell("settings put system font_scale 1.5")
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            android.os.SystemClock.sleep(800)
            snapshot("10-small-large-font-home")
            compose.runOnIdle { ViewModelProvider(compose.activity)[PulseViewModel::class.java].navigate(Screen.SETTINGS) }
            compose.onNodeWithText("Поиск настроек").assertIsDisplayed()
            snapshot("11-small-large-font-settings")
        } finally {
            shell("settings put system font_scale 1.0")
            shell("wm size reset")
            shell("wm density reset")
        }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[PulseViewModel::class.java]
            model.setDarkTheme(true)
            model.import("""
                #profile-title: Pulse Demo — лимит устройств
                #support-url: https://t.me/pulse_demo
                #profile-web-page-url: https://example.invalid
                {"outbounds":[{"type":"vless","tag":"❌ Лимит устройств: удалите старое устройство","server":"error.example.invalid","server_port":443}]}
            """.trimIndent())
        }
        compose.waitUntil(15_000) { model.state.value.selectedProfile?.issue?.code == "device_limit" && !model.state.value.importing }
        compose.runOnIdle { model.clearMessage(); model.startVpn() }
        org.junit.Assert.assertEquals(io.nekohasekai.sfa.constant.Status.Stopped, model.state.value.vpnStatus)
        compose.runOnIdle { model.clearMessage() }
        compose.onNodeWithText("Лимит устройств").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("❌ Лимит устройств: удалите старое устройство").assertIsDisplayed()
        compose.onNodeWithText("Подключить").assertDoesNotExist()
        compose.onNodeWithText("СЕРВЕРЫ").assertDoesNotExist()
        snapshot("12-unavailable-subscription")
        compose.onNodeWithContentDescription("Свернуть подписку").performScrollTo().performClick()
        compose.onNodeWithText("❌ Лимит устройств: удалите старое устройство").assertDoesNotExist()
        compose.onNodeWithText("Лимит устройств").assertIsDisplayed()
        snapshot("13-collapsed-unavailable-subscription")
    }
}
