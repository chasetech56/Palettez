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

    /**
     * Requirement GR-1:
     * Standard column & row grid specifications with defaults.
     */
    @Test
    fun GR1_gridDefaultsAndGeometry() {
        val spec = GridSpec()
        assertEquals(1080, spec.width)
        assertEquals(1080, spec.height)
        assertEquals(12, spec.columns)
        assertEquals(20, spec.gutter)
        assertEquals(60, spec.marginTop)

        val geom = spec.geometry()
        assertEquals(960f, geom.innerW) // 1080 - 60 - 60
        assertEquals(12, geom.colX.size)
        assertTrue(geom.colW > 0)
    }

    /**
     * Requirement GR-2:
     * Preset canvas sizes match standard industry dimensions.
     */
    @Test
    fun GR2_presetCanvasSizes() {
        assertTrue(GridPresets.any { it.label == "Instagram post" && it.width == 1080 && it.height == 1080 })
        assertTrue(GridPresets.any { it.label == "Story / Reel" && it.width == 1080 && it.height == 1920 })
        assertTrue(GridPresets.any { it.label == "HD slide" && it.width == 1920 && it.height == 1080 })
    }

    /**
     * Requirement GR-3:
     * Margin & gutter controls with clamping and summary string.
     */
    @Test
    fun GR3_clampingAndSummary() {
        val spec = GridSpec(width = 5000, height = -10, columns = 60, gutter = -5)
        assertEquals(MAX_SIDE, spec.w)
        assertEquals(16, spec.h)
        assertEquals(48, spec.cols)
        assertEquals(0, spec.gut)

        val summary = spec.summary()
        assertTrue(summary.contains("px"))
    }

    /**
     * Requirement GR-4:
     * Aspect ratio controls, center lines, and rule of thirds guides.
     */
    @Test
    fun GR4_centerLinesAndThirds() {
        val spec = GridSpec(centerLines = true, thirds = true)
        val svg = spec.toSvg()
        assertTrue(svg.contains("stroke-dasharray"))
        assertTrue(svg.contains("id=\"grid\""))
        assertTrue(svg.contains("id=\"margins\""))
    }

    /**
     * Requirement GR-7:
     * Export grid as CSS and SVG.
     */
    @Test
    fun GR7_exportCssAndSvg() {
        val spec = GridSpec(columns = 12, gutter = 24)
        val css = spec.toCss()
        assertTrue(css.contains(".container {"))
        assertTrue(css.contains("grid-template-columns: repeat(12, 1fr);"))
        assertTrue(css.contains("gap: 24px;"))

        val svg = spec.toSvg()
        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.endsWith("</svg>"))
    }

    /**
     * Requirement GR-8:
     * Serialization: GridSpec toJson and fromJson round-trips cleanly.
     */
    @Test
    fun GR8_jsonRoundTrip() {
        val original = GridSpec(width = 1920, height = 1080, columns = 16, gutter = 32, centerLines = true)
        val json = original.toJson()
        val restored = GridSpec.fromJson(json)
        assertEquals(original.width, restored.width)
        assertEquals(original.height, restored.height)
        assertEquals(original.columns, restored.columns)
        assertEquals(original.gutter, restored.gutter)
        assertEquals(original.centerLines, restored.centerLines)
    }

    /**
     * Requirement GR-5:
     * On-device reverse grid analysis estimates columns and gutters.
     */
    @Test
    fun GR5_reverseGridDetection() {
        val w = 100
        val h = 100
        val pixels = IntArray(w * h) { 0xFFFFFFFF.toInt() }
        val result = GridAnalysisEngine.analyzePixels(1080, 1080, w, h, pixels)
        assertTrue(result.estimatedColumns in 1..12)
        assertNotNull(result.detectedFamily)
    }

    /**
     * Requirement GR-6:
     * Column and row guidelines preview geometry calculation.
     */
    @Test
    fun GR6_guidelinesPreviewCalculation() {
        val spec = GridSpec(columns = 6, rows = 4, gutter = 16)
        val geom = spec.geometry()
        assertEquals(6, geom.colX.size)
        assertEquals(4, geom.rowY.size)
        assertTrue(geom.colW > 0)
        assertTrue(geom.rowH > 0)
    }
}
