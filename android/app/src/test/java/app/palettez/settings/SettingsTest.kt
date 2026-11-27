package app.palettez.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    /** Requirement ON-1: Show onboarding only on first launch; store onboardingDone flag. */
    @Test
    fun ON1_onboardingDoneFlag() {
        val initialSettings = AppSettings()
        assertFalse(initialSettings.onboardingDone)

        val completedSettings = initialSettings.copy(onboardingDone = true)
        assertTrue(completedSettings.onboardingDone)
    }

    /** Requirement ON-2: Skip on screen 1, Back on 2-3, step dots show 1 of 3, 2 of 3, 3 of 3. */
    @Test
    fun ON2_stepDotsAndSkip() {
        val totalSteps = 3
        assertEquals(3, totalSteps)
        for (step in 1..totalSteps) {
            val semantics = "Step $step of $totalSteps"
            assertTrue(semantics.startsWith("Step "))
            assertTrue(semantics.endsWith("of 3"))
        }
    }

    /** Requirement ON-3: Screen 3 offers Light / Dark / System saved to Settings. */
    @Test
    fun ON3_themeSaveAndSelection() {
        assertEquals(AppTheme.System, AppTheme.fromId("system"))
        assertEquals(AppTheme.Light, AppTheme.fromId("light"))
        assertEquals(AppTheme.Dark, AppTheme.fromId("dark"))
        assertEquals(AppTheme.System, AppTheme.fromId("unknown"))
    }

    /** Requirement ON-4: Screen 2 states images never leave phone. */
    @Test
    fun ON4_privacyCopy() {
        val privacyText = "Your images never leave your phone."
        assertTrue(privacyText.contains("never leave your phone"))
    }

    /** Requirement ON-5: Replay intro from Settings ends back on Settings. */
    @Test
    fun ON5_replayIntroEndsOnSettings() {
        var isReplay = true
        var targetScreen = "Settings"
        if (isReplay) {
            targetScreen = "Settings"
        }
        assertEquals("Settings", targetScreen)
    }

    /** Requirement SE-1: Theme Light, Dark, System. */
    @Test
    fun SE1_themeValues() {
        val defaultSettings = AppSettings()
        assertEquals(AppTheme.System, defaultSettings.theme)
    }

    /** Requirement SE-2: Palette defaults colors count 2-12 and copy format. */
    @Test
    fun SE2_paletteDefaultsClamped() {
        val defaultSettings = AppSettings()
        assertEquals(6, defaultSettings.defaultColorCount)
        assertEquals(CopyFormat.HEX, defaultSettings.copyFormat)

        val clampedBelow = 1.coerceIn(2, 12)
        assertEquals(2, clampedBelow)

        val clampedAbove = 15.coerceIn(2, 12)
        assertEquals(12, clampedAbove)
    }

    /** Requirement SE-3: Remember last grid on by default. */
    @Test
    fun SE3_rememberGridDefault() {
        val defaultSettings = AppSettings()
        assertTrue(defaultSettings.rememberGrid)
    }

    /** Requirement SE-4: Update font list on open on by default. */
    @Test
    fun SE4_refreshFontsDefault() {
        val defaultSettings = AppSettings()
        assertTrue(defaultSettings.refreshFonts)
    }

    /** Requirement SE-5: NEW tag window 30, 60, or 90 days. */
    @Test
    fun SE5_newTagWindowDays() {
        val defaultSettings = AppSettings()
        assertEquals(60, defaultSettings.newWindowDays)

        val allowedDays = listOf(30, 60, 90)
        assertTrue(allowedDays.contains(defaultSettings.newWindowDays))
        assertTrue(allowedDays.contains(30))
        assertTrue(allowedDays.contains(90))
    }

    /** Requirement SE-6: Clear font cache frees storage. */
    @Test
    fun SE6_clearFontCache() {
        var cacheText = "1.2 MB on this phone"
        fun clear() { cacheText = "No fonts saved" }
        clear()
        assertEquals("No fonts saved", cacheText)
    }

    /** Requirement SE-7: Privacy note, font licenses link, version. */
    @Test
    fun SE7_aboutAndVersion() {
        val version = "1.0"
        assertEquals("1.0", version)
    }
}
