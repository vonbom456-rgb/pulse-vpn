package app.pulse.vpn.ui

import app.pulse.vpn.Screen
import org.junit.Assert.*
import org.junit.Test

class PulseMotionTest {
    @Test fun tabDirectionsFollowNavigationOrder() {
        assertEquals(1, screenDirection(Screen.HOME, Screen.ROUTES))
        assertEquals(1, screenDirection(Screen.ROUTES, Screen.SETTINGS))
        assertEquals(-1, screenDirection(Screen.SETTINGS, Screen.HOME))
    }
    @Test fun returningFromDetailsReversesDirection() {
        assertEquals(1, screenDirection(Screen.SETTINGS, Screen.APPS))
        assertEquals(-1, screenDirection(Screen.APPS, Screen.SETTINGS))
        assertEquals(1, screenDirection(Screen.HOME, Screen.PROFILES))
        assertEquals(-1, screenDirection(Screen.PROFILES, Screen.HOME))
    }
    @Test fun detailScreensKeepTheirParentTabSelected() {
        assertEquals(Screen.HOME, Screen.PROFILES.navigationTab())
        assertEquals(Screen.SETTINGS, Screen.APPS.navigationTab())
        assertEquals(Screen.SETTINGS, Screen.STATS.navigationTab())
    }
    @Test fun selectingCurrentScreenHasNoDirection() {
        Screen.entries.forEach { assertEquals(0, screenDirection(it, it)) }
    }
    @Test fun reducedMotionDoesNotDisableOtherSettings() {
        val options = app.pulse.vpn.core.AdvancedOptions().with("ui_animations", "false")
        assertFalse(options.bool("ui_animations"))
        assertTrue(options.bool("show_home_servers"))
        assertFalse(app.pulse.vpn.core.OptionCatalog.byKey.getValue("ui_animations").reconnect)
    }
}

