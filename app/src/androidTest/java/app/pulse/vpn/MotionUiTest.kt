package app.pulse.vpn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.pulse.vpn.ui.PulseScreenHost
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MotionUiTest {
    // CI disables system animations; explicitly enable deterministic Compose motion here.
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 1f
    })

    @Test fun transitionsHaveFixedViewportAndPreserveStateThroughInterruptions() {
        var screen by mutableStateOf(Screen.HOME)
        var animated by mutableStateOf(true)
        compose.setContent {
            PulseScreenHost(screen, animated) { page ->
                var count by rememberSaveable { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize().background(Color(0xFF12131C)).padding(24.dp)) {
                    Text(page.name + ":" + count, color = Color.White)
                    Button({ count++ }) { Text("Increment") }
                }
            }
        }
        compose.onNodeWithText("Increment").performClick()
        compose.onNodeWithText("HOME:1").assertExists()
        val viewport = compose.onRoot().fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { screen = Screen.SETTINGS }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("HOME:1").assertDoesNotExist() // Outgoing screen is not accessible/clickable.
        val during = compose.onNodeWithTag("screen:SETTINGS").fetchSemanticsNode().boundsInRoot
        assertTrue("The entering page must still be sliding at 64 ms", during.left > viewport.left)
        assertEquals(viewport.height, during.height, 1f)
        compose.mainClock.advanceTimeBy(320)
        val after = compose.onNodeWithTag("screen:SETTINGS").fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.left, after.left, 1f)
        assertEquals(viewport.height, after.height, 1f)
        compose.runOnIdle { screen = Screen.HOME }
        compose.mainClock.advanceTimeBy(320)
        compose.onNodeWithText("HOME:1").assertExists()

        repeat(24) { index ->
            compose.runOnIdle { screen = listOf(Screen.ROUTES, Screen.SETTINGS, Screen.HOME)[index % 3] }
            compose.mainClock.advanceTimeBy(32)
        }
        compose.mainClock.advanceTimeBy(320)
        compose.onNodeWithTag("screen:HOME").assertExists()
        compose.onNodeWithText("HOME:1").assertExists()

        compose.runOnIdle { animated = false; screen = Screen.APPS }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("screen:APPS").assertExists()
        compose.onNodeWithText("HOME:1").assertDoesNotExist()
        assertEquals(viewport, compose.onRoot().fetchSemanticsNode().boundsInRoot)
        compose.mainClock.autoAdvance = true
    }
}

