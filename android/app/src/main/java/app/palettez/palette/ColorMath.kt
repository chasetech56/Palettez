package app.palettez.palette

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One extracted color and the fraction of the image it covers (0..1). */
data class Swatch(val r: Int, val g: Int, val b: Int, val share: Float) {
    val argb: Int get() = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    val hex: String get() = "#%02X%02X%02X".format(r, g, b)
    val rgb: String get() = "rgb($r, $g, $b)"
    val cmyk: String get() = cmykOf(r, g, b).let { (c, m, y, k) -> "cmyk($c%, $m%, $y%, $k%)" }
    val hsl: String get() = hslOf(r, g, b).let { (h, s, l) -> "hsl($h, $s%, $l%)" }
    val cmykValues: List<Int> get() = cmykOf(r, g, b)
    val hslValues: List<Int> get() = hslOf(r, g, b)
    /** Perceived brightness 0..255, used to pick black or white text on top. */
    val luminance: Float get() = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val hue: Int get() = hslOf(r, g, b)[0]
}

/** Standard device-independent RGB → CMYK formula (no ICC profile). */
fun cmykOf(r: Int, g: Int, b: Int): List<Int> {
    val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
    val k = 1f - max(rf, max(gf, bf))
    if (k >= 1f) return listOf(0, 0, 0, 100)
    fun ch(v: Float) = (((1f - v - k) / (1f - k)) * 100).roundToInt()
    return listOf(ch(rf), ch(gf), ch(bf), (k * 100).roundToInt())
}

fun hslOf(r: Int, g: Int, b: Int): List<Int> {
    val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
    val mx = max(rf, max(gf, bf)); val mn = min(rf, min(gf, bf))
    val l = (mx + mn) / 2f
    var h = 0f; var s = 0f
    if (mx != mn) {
        val d = mx - mn
        s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        h = when (mx) {
            rf -> (gf - bf) / d + (if (gf < bf) 6f else 0f)
            gf -> (bf - rf) / d + 2f
            else -> (rf - gf) / d + 4f
        } * 60f
    }
    return listOf(h.roundToInt(), (s * 100).roundToInt(), (l * 100).roundToInt())
}
