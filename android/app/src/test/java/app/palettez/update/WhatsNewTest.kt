package app.palettez.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {

    @Test
    fun WN1_latestReleaseIsRegistered() {
        val latest = WhatsNewRegistry.getLatestRelease()
        assertNotNull("Latest release note must exist", latest)
        assertEquals(2, latest.versionCode)
        assertEquals("1.1.0", latest.versionName)
        assertTrue("Headline must not be empty", latest.headline.isNotEmpty())
        assertTrue("Release must contain highlights", latest.items.isNotEmpty())
    }

    @Test
    fun WN3_changelogIncludesNewFeaturesAndImprovements() {
        val latest = WhatsNewRegistry.getLatestRelease()
        val hasNew = latest.items.any { it.tag == ChangeTag.New }
        val hasImproved = latest.items.any { it.tag == ChangeTag.Improved }

        assertTrue("Release should highlight brand new features", hasNew)
        assertTrue("Release should highlight improvements", hasImproved)

        // Check key features in v1.1.0
        val titles = latest.items.map { it.title }
        assertTrue(titles.any { it.contains("Grid Analysis") })
        assertTrue(titles.any { it.contains("Brand Identity") })
        assertTrue(titles.any { it.contains("Font Pairing") })
    }

    @Test
    fun WN5_releaseNotesCanBeRetrievedByVersionCode() {
        val v1 = WhatsNewRegistry.getReleaseForVersion(1)
        assertEquals("1.0.0", v1.versionName)

        val v2 = WhatsNewRegistry.getReleaseForVersion(2)
        assertEquals("1.1.0", v2.versionName)
    }
}
