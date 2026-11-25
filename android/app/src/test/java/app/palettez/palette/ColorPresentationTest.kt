package app.palettez.palette

import app.palettez.color.ColorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPresentationTest {

    @Test
    fun CP1_aspectRatiosMatchStandardDimensions() {
        assertEquals(1920, PresentationAspectRatio.Ratio16x9.width)
        assertEquals(1080, PresentationAspectRatio.Ratio16x9.height)

        assertEquals(1080, PresentationAspectRatio.Ratio1x1.width)
        assertEquals(1080, PresentationAspectRatio.Ratio1x1.height)
    }

    @Test
    fun CP3_colorConversionsProduceAccurateValues() {
        // Red color #E4572E (rgb: 228, 87, 46)
        val cmyk = cmykOf(228, 87, 46)
        assertEquals(0, cmyk[0]) // C
        assertTrue("M should be around 62%", cmyk[1] in 60..64)
        assertTrue("Y should be around 80%", cmyk[2] in 78..82)
        assertTrue("K should be around 11%", cmyk[3] in 9..12)

        val hsl = hslOf(228, 87, 46)
        assertTrue("H should be around 14°", hsl[0] in 12..16)
        assertTrue("S should be around 77%", hsl[1] in 75..79)
        assertTrue("L should be around 54%", hsl[2] in 52..56)

        val oklab = ColorEngine.toOklab("#E4572E")
        assertTrue("OKLab L should be in valid range", oklab.l in 0.55..0.65)
    }

    @Test
    fun CP4_wcagAccessibilityCalculationEvaluatesCorrectly() {
        val contrastHigh = ColorEngine.contrast("#151515", "#FFFFFF")
        assertEquals("AAA", ColorEngine.grade(contrastHigh))
        assertTrue(contrastHigh >= 7.0)

        val contrastLow = ColorEngine.contrast("#888888", "#777777")
        assertEquals("Fail", ColorEngine.grade(contrastLow))
    }
}
