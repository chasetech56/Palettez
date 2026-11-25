package app.palettez.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Returns the color (ink #151515 or white) that provides the higher WCAG 2.1
 * contrast ratio against [background].
 *
 * WCAG contrast ratio = (L1 + 0.05) / (L2 + 0.05)
 * where L1 is the lighter relative luminance and L2 is the darker one.
 */
fun contrastColor(background: Color): Color {
    val bgLuminance = background.relativeLuminance()

    val inkLuminance   = Color(0xFF151515).relativeLuminance()
    val whiteLuminance = Color.White.relativeLuminance()

    val contrastWithInk   = contrastRatio(bgLuminance, inkLuminance)
    val contrastWithWhite = contrastRatio(bgLuminance, whiteLuminance)

    return if (contrastWithInk >= contrastWithWhite) Color(0xFF151515) else Color.White
}

/** WCAG relative luminance of this color (sRGB). */
private fun Color.relativeLuminance(): Double {
    fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
}

/** WCAG contrast ratio between two relative luminances. */
private fun contrastRatio(lum1: Double, lum2: Double): Double {
    val lighter = maxOf(lum1, lum2)
    val darker  = minOf(lum1, lum2)
    return (lighter + 0.05) / (darker + 0.05)
}
