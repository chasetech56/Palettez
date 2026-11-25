package app.palettez.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun WN7_parseManifest_parsesValidVersionJson() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.2.0",
              "downloadUrl": "https://chasetech56.github.io/Palettez/releases/palettez-v1.2.0.apk",
              "homepageUrl": "https://chasetech56.github.io/Palettez/",
              "releaseNotes": [
                "Eyedropper tool on photos",
                "Adobe .ase export"
              ]
            }
        """.trimIndent()

        val parsed = UpdateChecker.parseManifest(json)
        assertNotNull(parsed)
        assertEquals(3, parsed!!.versionCode)
        assertEquals("1.2.0", parsed.versionName)
        assertEquals("https://chasetech56.github.io/Palettez/", parsed.homepageUrl)
        assertEquals(2, parsed.releaseNotes.size)
    }

    @Test
    fun WN7_parseManifest_rejectsInvalidOrHttpUrls() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.2.0",
              "homepageUrl": "http://insecure.example.com"
            }
        """.trimIndent()

        val parsed = UpdateChecker.parseManifest(json)
        assertNotNull(parsed)
        // Non-HTTPS URLs fall back safely to official HTTPS homepage
        assertEquals(UpdateChecker.HOMEPAGE_URL, parsed!!.homepageUrl)
        assertNull(UpdateChecker.parseManifest("{ invalid json"))
    }

    @Test
    fun WN7_isUpdateAvailable_comparesVersionCodes() {
        val remote = RemoteReleaseInfo(
            versionCode = 3,
            versionName = "1.2.0",
            downloadUrl = UpdateChecker.HOMEPAGE_URL,
            homepageUrl = UpdateChecker.HOMEPAGE_URL,
            releaseNotes = emptyList(),
        )
        assertTrue(UpdateChecker.isUpdateAvailable(currentVersionCode = 2, remote = remote))
        assertFalse(UpdateChecker.isUpdateAvailable(currentVersionCode = 3, remote = remote))
        assertFalse(UpdateChecker.isUpdateAvailable(currentVersionCode = 4, remote = remote))
        assertFalse(UpdateChecker.isUpdateAvailable(currentVersionCode = 2, remote = null))
    }
}
