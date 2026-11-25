package app.palettez.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridAnalysisTest {

    @Test
    fun GA3_detectsThreeColumnLayoutWithMargins() {
        val w = 300
        val h = 400
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() } // White background

        // Draw 3 darker columns with 20px margins and 10px gutters
        // Margins: 20px on left and right -> inner width = 260px
        // 3 columns: col width ~ 80px, gutters at ~100px and ~190px
        for (y in 20 until 380) {
            for (x in 20 until 100) {
                pixels[y * w + x] = 0xFF202020.toInt()
            }
            for (x in 110 until 190) {
                pixels[y * w + x] = 0xFF202020.toInt()
            }
            for (x in 200 until 280) {
                pixels[y * w + x] = 0xFF202020.toInt()
            }
        }

        val result = GridAnalysisEngine.analyzePixels(
            origW = 1080,
            origH = 1440,
            sampleW = w,
            sampleH = h,
            pixels = pixels
        )

        assertEquals("Should detect 3 columns", 3, result.estimatedColumns)
        assertTrue("Confidence should be above 70%", result.confidence >= 70)
        assertTrue("Left margin should be positive", result.estimatedMarginLeft > 0)
        assertTrue("Right margin should be positive", result.estimatedMarginRight > 0)
        assertTrue("Gutter should be positive", result.estimatedGutterPx > 0)
        assertNotNull("Should detect a layout family", result.detectedFamily)
    }

    @Test
    fun GA4_aspectRatioAndFamilyClassification() {
        val w = 200
        val h = 400
        val pixels = IntArray(w * h) { 0xFFEFE9DF.toInt() }

        // Two tall stacks
        for (y in 20 until 380) {
            for (x in 20 until 95) {
                pixels[y * w + x] = 0xFF151515.toInt()
            }
            for (x in 105 until 180) {
                pixels[y * w + x] = 0xFF151515.toInt()
            }
        }

        val result = GridAnalysisEngine.analyzePixels(
            origW = 1080,
            origH = 2160,
            sampleW = w,
            sampleH = h,
            pixels = pixels
        )

        assertEquals("Stacks should detect 2 columns", 2, result.estimatedColumns)
        assertEquals("Tall portrait should classify into stacks", "stacks", result.detectedFamily.key)
    }

    @Test
    fun GA6_gridSpecConversion() {
        val w = 200
        val h = 200
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }

        val result = GridAnalysisEngine.analyzePixels(
            origW = 1080,
            origH = 1080,
            sampleW = w,
            sampleH = h,
            pixels = pixels
        )

        val spec = result.toGridSpec()
        assertEquals(1080, spec.width)
        assertEquals(1080, spec.height)
        assertEquals(result.estimatedColumns, spec.columns)
        assertEquals(result.estimatedGutterPx, spec.gutter)
        assertEquals(result.estimatedMarginLeft, spec.marginLeft)
    }
}
