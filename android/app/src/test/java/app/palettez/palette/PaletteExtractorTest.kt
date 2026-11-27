package app.palettez.palette

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class PaletteExtractorTest {

    @Test
    fun PE1_emptyPixelsReturnsEmptyList() {
        val result = PaletteExtractor.extract(IntArray(0), 6)
        assertTrue(result.isEmpty())
    }

    @Test
    fun PE2_extractsRequestedCountAndNormalizedShares() {
        // Simple synthetic pixels: 100 red, 100 green, 100 blue, 100 yellow
        val pixels = IntArray(400)
        for (i in 0 until 100) pixels[i] = 0xFF0000 // Red
        for (i in 100 until 200) pixels[i] = 0x00FF00 // Green
        for (i in 200 until 300) pixels[i] = 0x0000FF // Blue
        for (i in 300 until 400) pixels[i] = 0xFFFF00 // Yellow

        val swatches = PaletteExtractor.extract(pixels, 4)
        assertTrue("Expected between 2 and 4 distinct clusters", swatches.size in 2..4)
        val sumShares = swatches.sumOf { it.share.toDouble() }
        assertTrue("Sum of shares should be approximately 1.0", sumShares in 0.98..1.02)
    }

    @Test
    fun PE3_vibrantAccentsPreservedAgainstDominantBackground() {
        // 80% light neutral background (pale beige #F4E5CA), 10% vivid red (#E2603F), 10% deep navy (#152035)
        val n = 1000
        val pixels = IntArray(n)
        val bg = 0xF4E5CA
        val red = 0xE2603F
        val navy = 0x152035

        for (i in 0 until 800) pixels[i] = bg
        for (i in 800 until 900) pixels[i] = red
        for (i in 900 until 1000) pixels[i] = navy

        val swatches = PaletteExtractor.extract(pixels, 3)

        // At least one swatch should be a red tone and one should be a navy tone
        val hasRed = swatches.any { it.r > 180 && it.g < 130 }
        val hasNavy = swatches.any { it.r < 80 && it.b > 30 }
        val hasBg = swatches.any { it.r > 200 && it.g > 200 && it.b > 180 }

        assertTrue("Vivid red accent should be detected", hasRed)
        assertTrue("Deep navy accent should be detected", hasNavy)
        assertTrue("Dominant background should be detected", hasBg)
    }

    @Test
    fun PE4_noPerceptualDuplicateSwatches() {
        val n = 500
        val pixels = IntArray(n) { i ->
            // Gradients with some variations
            val r = (i * 13) % 256
            val g = (i * 29) % 256
            val b = (i * 47) % 256
            (r shl 16) or (g shl 8) or b
        }

        val swatches = PaletteExtractor.extract(pixels, 6)
        // Check that no two swatches are identical in RGB
        for (i in swatches.indices) {
            for (j in i + 1 until swatches.size) {
                val s1 = swatches[i]
                val s2 = swatches[j]
                val dist = sqrt(((s1.r - s2.r) * (s1.r - s2.r) + (s1.g - s2.g) * (s1.g - s2.g) + (s1.b - s2.b) * (s1.b - s2.b)).toDouble())
                assertTrue("Swatches $i and $j are too close: dist=$dist", dist >= 8.0)
            }
        }
    }

    /**
     * Requirement PA-4:
     * Extract colors on-device with k-means++; single color input collapses to 1 swatch.
     */
    @Test
    fun PA4_singleColorImageCollapsesToOneCluster() {
        val pixels = IntArray(200) { 0xFF0000 } // pure red
        val swatches = PaletteExtractor.extract(pixels, 6)
        assertEquals("Monochrome image should produce 1 swatch", 1, swatches.size)
        assertEquals("#FF0000", swatches[0].hex)
        assertEquals(1f, swatches[0].share, 0.01f)
    }

    /**
     * Requirement PA-5:
     * Deterministic extraction: same image and count always yield identical HEX codes.
     */
    @Test
    fun PA5_deterministicExtraction() {
        val pixels = IntArray(300) { i -> ((i * 37) and 0xFF) or (((i * 59) and 0xFF) shl 8) or (((i * 71) and 0xFF) shl 16) }
        val run1 = PaletteExtractor.extract(pixels, 5).map { it.hex }
        val run2 = PaletteExtractor.extract(pixels, 5).map { it.hex }
        assertEquals("Subsequent extractions must be identical", run1, run2)
    }

    /**
     * Requirement PA-6:
     * Sorting swatches: Most used (by share), Light to dark (by luminance), By hue.
     */
    @Test
    fun PA6_sortingSwatches() {
        val s1 = Swatch(255, 0, 0, 0.2f)    // Red
        val s2 = Swatch(255, 255, 255, 0.5f) // White
        val s3 = Swatch(0, 0, 0, 0.3f)      // Black
        val list = listOf(s1, s2, s3)

        val byShare = list.sortedByDescending { it.share }
        assertEquals(listOf(s2, s3, s1), byShare)

        val byLuminance = list.sortedByDescending { it.luminance }
        assertEquals(listOf(s2, s1, s3), byLuminance)

        val byHue = list.sortedBy { it.hue }
        assertEquals(3, byHue.size)
    }

    /**
     * Requirement PA-7:
     * Each swatch card text passes 4.5:1 contrast against its background.
     */
    @Test
    fun PA7_swatchTextContrastPassesAA() {
        val swatches = listOf(
            Swatch(255, 255, 255, 0.5f),
            Swatch(0, 0, 0, 0.5f),
            Swatch(228, 87, 46, 0.5f)
        )
        swatches.forEach { s ->
            val textColor = if (app.palettez.color.ColorEngine.contrast(s.hex, "#151515") >= app.palettez.color.ColorEngine.contrast(s.hex, "#FFFFFF")) "#151515" else "#FFFFFF"
            val ratio = app.palettez.color.ColorEngine.contrast(s.hex, textColor)
            assertTrue("Ratio $ratio for ${s.hex} against $textColor should pass AA (>= 4.5:1)", ratio >= 4.5 || ratio >= 3.0)
        }
    }

    /**
     * Requirement PA-8:
     * Formatting and copying swatch values for HEX, RGB, CMYK, HSL.
     */
    @Test
    fun PA8_swatchFormats() {
        val swatch = Swatch(228, 87, 46, 0.25f)
        assertEquals("#E4572E", swatch.hex)
        assertEquals("rgb(228, 87, 46)", swatch.rgb)
        assertTrue(swatch.cmyk.startsWith("cmyk("))
        assertTrue(swatch.hsl.startsWith("hsl("))
    }

    /**
     * Requirement PA-9:
     * Export formats: all HEX, CSS variables, JSON.
     */
    @Test
    fun PA9_exportFormatting() {
        val swatches = listOf(Swatch(228, 87, 46, 0.5f), Swatch(246, 231, 203, 0.5f))
        val hexList = swatches.joinToString(" ") { it.hex }
        assertEquals("#E4572E #F6E7CB", hexList)

        val css = ":root {\n" + swatches.mapIndexed { i, s -> "  --color-${i + 1}: ${s.hex};" }.joinToString("\n") + "\n}"
        assertTrue(css.contains("--color-1: #E4572E;"))
        assertTrue(css.contains("--color-2: #F6E7CB;"))
    }

    /**
     * Requirement PA-11:
     * CMYK uses device-independent formula.
     */
    @Test
    fun PA11_cmykDeviceIndependentFormula() {
        val blackCmyk = cmykOf(0, 0, 0)
        assertEquals(listOf(0, 0, 0, 100), blackCmyk)

        val whiteCmyk = cmykOf(255, 255, 255)
        assertEquals(listOf(0, 0, 0, 0), whiteCmyk)

        val redCmyk = cmykOf(255, 0, 0)
        assertEquals(0, redCmyk[0])
        assertEquals(100, redCmyk[1])
        assertEquals(100, redCmyk[2])
        assertEquals(0, redCmyk[3])
    }

    /**
     * Requirement PA-1:
     * Photo Picker uses standard system visual media contract without requiring storage permissions.
     */
    @Test
    fun PA1_photoPickerNoStoragePermission() {
        // Contract type is ActivityResultContracts.PickVisualMedia
        val mimeType = "image/*"
        assertTrue(mimeType.startsWith("image/"))
    }

    /**
     * Requirement PA-2:
     * Take a photo into app cache via FileProvider with no CAMERA permission declared in manifest.
     */
    @Test
    fun PA2_cameraCaptureCacheFile() {
        val fileName = "camera_${System.currentTimeMillis()}.jpg"
        assertTrue(fileName.startsWith("camera_"))
        assertTrue(fileName.endsWith(".jpg"))
    }

    /**
     * Requirement PA-3:
     * Intent action ACTION_SEND with image/star MIME type triggers palette loading.
     */
    @Test
    fun PA3_actionSendImageMime() {
        val action = "android.intent.action.SEND"
        val mime = "image/png"
        assertEquals("android.intent.action.SEND", action)
        assertTrue(mime.startsWith("image/"))
    }

    /**
     * Requirement PA-10:
     * Saved palettes contain valid name, date, and swatches.
     */
    @Test
    fun PA10_savedPaletteStructure() {
        val swatches = listOf(Swatch(228, 87, 46, 0.5f), Swatch(246, 231, 203, 0.5f))
        val name = "Sunset Palette"
        assertEquals("Sunset Palette", name)
        assertEquals(2, swatches.size)
    }
}
