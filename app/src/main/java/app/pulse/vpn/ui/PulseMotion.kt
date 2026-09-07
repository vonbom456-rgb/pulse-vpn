package app.pulse.vpn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.pulse.vpn.Screen

internal fun Screen.navigationTab(): Screen = when (this) {
    Screen.PROFILES -> Screen.HOME
    Screen.APPS, Screen.STATS -> Screen.SETTINGS
    else -> this
}

internal fun screenDirection(from: Screen, to: Screen): Int {
    if (from == to) return 0
    val tabs = listOf(Screen.HOME, Screen.ROUTES, Screen.SETTINGS)
    val fromTab = tabs.indexOf(from.navigationTab())
    val toTab = tabs.indexOf(to.navigationTab())
    return if (fromTab != toTab) if (toTab > fromTab) 1 else -1
    else if (to == to.navigationTab()) -1 else 1
}

/** Fixed viewport, short directional motion, no scaling or animated screen resizing. */
@Composable
internal fun PulseScreenHost(screen: Screen, animated: Boolean, content: @Composable (Screen) -> Unit) {
    val saved = rememberSaveableStateHolder()
    val distance = with(LocalDensity.current) { 24.dp.roundToPx() }
    AnimatedContent(
        targetState = screen,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        contentKey = { it },
        transitionSpec = {
            val direction = screenDirection(initialState, targetState)
            val enter = if (animated) fadeIn(tween(180, delayMillis = 40)) +
                slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { direction * distance }
                else EnterTransition.None
            val exit = if (animated) fadeOut(tween(90)) +
                slideOutHorizontally(tween(170, easing = FastOutSlowInEasing)) { -direction * distance / 2 }
                else ExitTransition.None
            (enter togetherWith exit).using(null)
        },
        label = "navigation",
    ) { visibleScreen ->
        saved.SaveableStateProvider(visibleScreen.name) {
            val active = screen == visibleScreen
            Box(Modifier.fillMaxSize()
                .then(if (active) Modifier.testTag("screen:" + visibleScreen.name) else Modifier.clearAndSetSemantics {})
                .pointerInput(active) {
                    if (!active) awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }) { content(visibleScreen) }
        }
    }
}

/** Shared, top-anchored disclosure animation without the default spring bounce. */
@Composable
internal fun PulseDisclosure(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val animated = LocalAdvancedOptions.current.bool("ui_animations")
    AnimatedVisibility(
        visible,
        enter = if (animated) expandVertically(tween(200, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(160)) else EnterTransition.None,
        exit = if (animated) shrinkVertically(tween(160, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(100)) else ExitTransition.None,
    ) { Column(content = content) }
}

