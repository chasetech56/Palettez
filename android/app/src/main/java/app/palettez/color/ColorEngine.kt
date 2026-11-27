// MODULE-BOUNDARY: This file must remain free of Android imports (core/color / core/layout)
package app.palettez.color

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure Kotlin implementation of the Palettez color system engine.
 * No Android dependencies — runs fast in JVM unit tests.
 *
 * Implements:
 * - sRGB ↔ OKLab color space conversions
 * - 10-step scales (50–900) mixed in OKLab
 * - WCAG 2.2 relative luminance and contrast ratios
 * - Harmonies: Complementary, Analogous (±30°), Triadic, Split
 * - Auto-role assignment: Background, Text, Primary, Secondary, Accent
 * - Exporters: CSS variables, Tailwind config, and JSON tokens
 */
object ColorEngine {

    val STEPS = listOf(50, 100, 200, 300, 400, 500, 600, 700, 800, 900)

    // Tints mix toward white, shades toward black, in OKLab so steps look even.
    // Pair(direction: 1=white, -1=black, 0=none, ratio: Double)
    val MIX = listOf(
        Pair(1, 0.92),
        Pair(1, 0.82),
        Pair(1, 0.64),
        Pair(1, 0.44),
        Pair(1, 0.22),
        Pair(0, 0.0),
        Pair(-1, 0.15),
        Pair(-1, 0.32),
        Pair(-1, 0.50),
        Pair(-1, 0.66),
    )

    val ROLE_ORDER = listOf("background", "text", "primary", "secondary", "accent")
    val ROLE_NAMES = mapOf(
        "background" to "Background",
        "text" to "Text",
        "primary" to "Primary",
        "secondary" to "Secondary",
        "accent" to "Accent",
    )

    data class Oklab(val l: Double, val a: Double, val b: Double)

    data class ColorScaleStep(val step: Int, val hex: String)

    data class Harmony(val name: String, val colors: List<String>)

    data class Roles(
        val background: String,
        val text: String,
        val primary: String,
        val secondary: String,
        val accent: String,
    ) {
        operator fun get(role: String): String = when (role.lowercase()) {
            "background" -> background
            "text" -> text
            "primary" -> primary
            "secondary" -> secondary
            "accent" -> accent
            else -> primary
        }
    }

    data class ContrastGrade(val ratio: Double, val grade: String)

    fun hexToRgb(h: String): Triple<Int, Int, Int> {
        val clean = h.trim().removePrefix("#")
        val n = clean.toInt(16)
        return Triple((n shr 16) and 255, (n shr 8) and 255, n and 255)
    }

    fun rgbToHex(r: Double, g: Double, b: Double): String {
        val ir = r.roundToInt().coerceIn(0, 255)
        val ig = g.roundToInt().coerceIn(0, 255)
        val ib = b.roundToInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", ir, ig, ib)
    }

    private fun toLin(v: Double): Double {
        val vf = v / 255.0
        return if (vf <= 0.04045) vf / 12.92 else ((vf + 0.055) / 1.055).pow(2.4)
    }

    private fun fromLin(v: Double): Double {
        val vf = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
        return vf * 255.0
    }

    fun toOklab(hex: String): Oklab {
        val (rInt, gInt, bInt) = hexToRgb(hex)
        val r = toLin(rInt.toDouble())
        val g = toLin(gInt.toDouble())
        val b = toLin(bInt.toDouble())

        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

        return Oklab(
            l = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    fun fromOklab(ok: Oklab): String {
        val l = (ok.l + 0.3963377774 * ok.a + 0.2158037573 * ok.b).pow(3.0)
        val m = (ok.l - 0.1055613458 * ok.a - 0.0638541728 * ok.b).pow(3.0)
        val s = (ok.l - 0.0894841775 * ok.a - 1.2914855480 * ok.b).pow(3.0)

        val rLin = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        val gLin = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        val bLin = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

        return rgbToHex(fromLin(rLin), fromLin(gLin), fromLin(bLin))
    }

    fun mixHex(hexA: String, hexB: String, t: Double): String {
        val a = toOklab(hexA)
        val b = toOklab(hexB)
        return fromOklab(
            Oklab(
                l = a.l + (b.l - a.l) * t,
                a = a.a + (b.a - a.a) * t,
                b = a.b + (b.b - a.b) * t,
            )
        )
    }

    fun scaleOf(hex: String): List<ColorScaleStep> {
        val clean = if (hex.startsWith("#")) hex.uppercase() else "#${hex.uppercase()}"
        return STEPS.indices.map { i ->
            val (dir, t) = MIX[i]
            val v = when (dir) {
                0 -> clean
                1 -> mixHex(clean, "#FFFFFF", t)
                else -> mixHex(clean, "#000000", t)
            }
            ColorScaleStep(STEPS[i], v)
        }
    }

    fun lum(hex: String): Double {
        val (r, g, b) = hexToRgb(hex)
        return 0.2126 * toLin(r.toDouble()) + 0.7152 * toLin(g.toDouble()) + 0.0722 * toLin(b.toDouble())
    }

    fun contrast(hexA: String, hexB: String): Double {
        val x = lum(hexA)
        val y = lum(hexB)
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    fun grade(contrastRatio: Double): String = when {
        contrastRatio >= 7.0 -> "AAA"
        contrastRatio >= 4.5 -> "AA"
        contrastRatio >= 3.0 -> "AA Large"
        else -> "Fail"
    }

    fun inkOn(hex: String): String {
        return if (contrast(hex, "#151515") >= contrast(hex, "#FFFFFF")) "#151515" else "#FFFFFF"
    }

    fun hsl(hex: String): Triple<Double, Double, Double> {
        val (rInt, gInt, bInt) = hexToRgb(hex)
        val r = rInt / 255.0
        val g = gInt / 255.0
        val b = bInt / 255.0

        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        var h = 0.0
        var s = 0.0
        val l = (mx + mn) / 2.0

        if (mx != mn) {
            val d = mx - mn
            s = if (l > 0.5) d / (2.0 - mx - mn) else d / (mx + mn)
            h = when (mx) {
                r -> (g - b) / d + (if (g < b) 6.0 else 0.0)
                g -> (b - r) / d + 2.0
                else -> (r - g) / d + 4.0
            } * 60.0
        }
        return Triple(h, s, l)
    }

    fun fromHsl(h: Double, s: Double, l: Double): String {
        val normalizedH = ((h % 360.0) + 360.0) % 360.0
        fun f(n: Int): Double {
            val k = (n + normalizedH / 30.0) % 12.0
            val a = s * min(l, 1.0 - l)
            return 255.0 * (l - a * max(-1.0, minOf(k - 3.0, 9.0 - k, 1.0)))
        }
        return rgbToHex(f(0), f(8), f(4))
    }

    fun harmonies(hex: String): List<Harmony> {
        val (h, s, l) = hsl(hex)
        fun at(d: Double): String = fromHsl(h + d, s, l)

        val clean = if (hex.startsWith("#")) hex.uppercase() else "#${hex.uppercase()}"
        return listOf(
            Harmony("Complementary", listOf(clean, at(180.0))),
            Harmony("Analogous", listOf(at(-30.0), clean, at(30.0))),
            Harmony("Triadic", listOf(clean, at(120.0), at(240.0))),
            Harmony("Split", listOf(clean, at(150.0), at(210.0))),
        )
    }

    /**
     * Auto-assign 5 roles:
     * - background = lightest
     * - text = darkest (or ink/white if contrast on background < 7)
     * - primary = most colorful color with contrast >= 3 on background
     * - secondary = next colorful color
     * - accent = hue furthest from primary
     */
    fun autoRoles(colors: List<String>): Roles {
        val byL = colors.sortedByDescending { lum(it) }
        val bg = byL.first()
        var text = byL.last()
        if (contrast(bg, text) < 7.0) {
            text = if (contrast(bg, "#151515") >= contrast(bg, "#FFFFFF")) "#151515" else "#FFFFFF"
        }

        var rest = colors.filter { it != bg && it != text }
        if (rest.isEmpty()) rest = listOf(text)

        fun chroma(c: String): Double {
            val o = toOklab(c)
            return hypot(o.a, o.b)
        }

        val ranked = rest.sortedByDescending { c ->
            chroma(c) + (if (contrast(c, bg) >= 3.0) 1.0 else 0.0)
        }
        val primary = ranked[0]
        val secondary = if (ranked.size > 1) ranked[1] else ranked[0]

        val hp = hsl(primary).first
        var pool = rest.filter { it != primary && it != secondary }
        if (pool.isEmpty()) pool = rest.filter { it != primary }
        if (pool.isEmpty()) pool = listOf(primary)

        val accent = pool.minByOrNull { c ->
            val h = hsl(c).first
            abs(((h - hp + 540.0) % 360.0) - 180.0)
        } ?: primary

        return Roles(
            background = bg,
            text = text,
            primary = primary,
            secondary = secondary,
            accent = accent,
        )
    }

    /**
     * Exporters for CSS variables, Tailwind config, and JSON tokens
     */
    fun exportCode(kind: String, paletteName: String, colors: List<String>, roles: Roles): String {
        val names = colors.indices.map { "color-${it + 1}" }
        return when (kind.lowercase()) {
            "css" -> {
                val sb = java.lang.StringBuilder()
                sb.append(":root {\n")
                ROLE_ORDER.forEach { r ->
                    sb.append("  --$r: ${roles[r]};\n")
                }
                colors.forEachIndexed { i, c ->
                    scaleOf(c).forEach { s ->
                        sb.append("  --${names[i]}-${s.step}: ${s.hex};\n")
                    }
                }
                sb.append("}")
                sb.toString()
            }
            "tailwind" -> {
                val sb = java.lang.StringBuilder()
                sb.append("// tailwind.config.js\nmodule.exports = {\n  theme: {\n    extend: {\n      colors: {\n")
                ROLE_ORDER.forEach { r ->
                    sb.append("        \"$r\": \"${roles[r]}\",\n")
                }
                colors.forEachIndexed { i, c ->
                    sb.append("        \"${names[i]}\": {\n")
                    scaleOf(c).forEach { s ->
                        sb.append("          \"${s.step}\": \"${s.hex}\",\n")
                    }
                    sb.append("        },\n")
                }
                sb.append("      }\n    }\n  }\n};")
                sb.toString()
            }
            else -> { // json
                val sb = java.lang.StringBuilder()
                sb.append("{\n")
                sb.append("  \"name\": \"$paletteName\",\n")
                sb.append("  \"roles\": {\n")
                ROLE_ORDER.forEachIndexed { idx, r ->
                    sb.append("    \"$r\": \"${roles[r]}\"${if (idx < ROLE_ORDER.size - 1) "," else ""}\n")
                }
                sb.append("  },\n")
                sb.append("  \"scales\": {\n")
                colors.forEachIndexed { i, c ->
                    sb.append("    \"${names[i]}\": {\n")
                    val steps = scaleOf(c)
                    steps.forEachIndexed { sIdx, s ->
                        sb.append("      \"${s.step}\": \"${s.hex}\"${if (sIdx < steps.size - 1) "," else ""}\n")
                    }
                    sb.append("    }${if (i < colors.size - 1) "," else ""}\n")
                }
                sb.append("  }\n")
                sb.append("}")
                sb.toString()
            }
        }
    }
}
