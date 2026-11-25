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
}
