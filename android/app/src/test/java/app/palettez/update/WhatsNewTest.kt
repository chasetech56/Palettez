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
        assertEquals(4, latest.versionCode)
        assertEquals("1.2.1", latest.versionName)
        assertTrue("Headline must not be empty", latest.headline.isNotEmpty())
        assertTrue("Release must contain highlights", latest.items.isNotEmpty())
    }

    @Test
    fun WN3_changelogIncludesNewFeaturesAndImprovements() {
        val latest = WhatsNewRegistry.getLatestRelease()
        val hasNew = latest.items.any { it.tag == ChangeTag.New }
        val hasImproved = latest.items.any { it.tag == ChangeTag.Improved }
        val hasFixed = latest.items.any { it.tag == ChangeTag.Fixed }

        assertTrue("Release should highlight brand new features", hasNew)
        assertTrue("Release should highlight improvements", hasImproved)
        assertTrue("Release should highlight fixes", hasFixed)

        val titles = latest.items.map { it.title }
        assertTrue(titles.any { it.contains("Camera") })
        assertTrue(titles.any { it.contains("Engine") })
    }

    @Test
    fun WN5_releaseNotesCanBeRetrievedByVersionCode() {
        val v1 = WhatsNewRegistry.getReleaseForVersion(1)
        assertEquals("1.0.0", v1.versionName)

        val v2 = WhatsNewRegistry.getReleaseForVersion(2)
        assertEquals("1.1.0", v2.versionName)

        val v3 = WhatsNewRegistry.getReleaseForVersion(3)
        assertEquals("1.2.0", v3.versionName)

        val v4 = WhatsNewRegistry.getReleaseForVersion(4)
        assertEquals("1.2.1", v4.versionName)
    }
}
