package app.palettez.settings

import app.palettez.palette.RecentPalette
import app.palettez.palette.Swatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupTest {

    @Test
    fun backupRoundTrip_preservesSettingsGridAndPalettes() {
        val settings = AppSettings(
            theme = AppTheme.Dark,
            defaultColorCount = 8,
            copyFormat = CopyFormat.CMYK,
            rememberGrid = false,
            refreshFonts = false,
            newWindowDays = 90,
            onboardingDone = true,
        )
        val palettes = listOf(
            RecentPalette(
                id = "p1",
                name = "Sunset Poster",
                timestamp = 1764000000000L,
                swatches = listOf(
                    Swatch(228, 87, 46, 0.55f),
                    Swatch(244, 197, 66, 0.45f),
                ),
            )
        )
        val gridJson = """{"width":1080,"height":1350,"columns":6,"rows":4}"""

        val exported = LocalBackupManager.serializeBackup(
            settings = settings,
            recentPalettes = palettes,
            gridSpecJson = gridJson,
            appVersion = "1.1.0",
            exportedAt = 1764000100000L,
        )

        val parsed = LocalBackupManager.parseBackup(exported)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.schemaVersion)
        assertEquals(AppTheme.Dark, parsed.settings.theme)
        assertEquals(8, parsed.settings.defaultColorCount)
        assertEquals(CopyFormat.CMYK, parsed.settings.copyFormat)
        assertEquals(false, parsed.settings.rememberGrid)
        assertEquals(false, parsed.settings.refreshFonts)
        assertEquals(90, parsed.settings.newWindowDays)
        assertTrue(parsed.gridSpecJson!!.contains("1350"))
        assertEquals(1, parsed.recentPalettes.size)
        assertEquals("Sunset Poster", parsed.recentPalettes[0].name)
        assertEquals(2, parsed.recentPalettes[0].swatches.size)
    }

    @Test
    fun parseBackup_rejectsInvalidFormat() {
        assertNull(LocalBackupManager.parseBackup("""{"format":"other","schemaVersion":1}"""))
        assertNull(LocalBackupManager.parseBackup("""not json"""))
    }
}
