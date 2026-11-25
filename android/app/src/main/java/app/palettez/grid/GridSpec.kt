package app.palettez.grid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class GridPreset(val label: String, val width: Int, val height: Int)

val GridPresets = listOf(
    GridPreset("Instagram post", 1080, 1080),
    GridPreset("Instagram portrait", 1080, 1350),
    GridPreset("Story / Reel", 1080, 1920),
    GridPreset("HD slide", 1920, 1080),
    GridPreset("Desktop web", 1440, 900),
    GridPreset("Phone", 390, 844),
    GridPreset("A4 portrait @300dpi", 2480, 3508),
    GridPreset("A4 landscape @300dpi", 3508, 2480),
    GridPreset("US Letter @300dpi", 2550, 3300),
    GridPreset("Poster 18×24 in @150dpi", 2700, 3600),
)

const val MAX_SIDE = 4096

data class GridSpec(
    val width: Int = 1080,
    val height: Int = 1080,
    val columns: Int = 12,
    val rows: Int = 0,
    val gutter: Int = 20,
    val baseline: Int = 0,
    val marginTop: Int = 60,
    val marginBottom: Int = 60,
    val marginLeft: Int = 60,
    val marginRight: Int = 60,
    val color: Int = 0xFFFF3B6B.toInt(),
    val background: Int = 0xFFFFFFFF.toInt(),
    val fillOpacity: Float = 0.15f,
    val transparent: Boolean = false,
    val centerLines: Boolean = false,
    val thirds: Boolean = false,
) {
    // Values typed by the user are clamped here, not while typing.
    val w get() = width.coerceIn(16, MAX_SIDE)
    val h get() = height.coerceIn(16, MAX_SIDE)
    val cols get() = columns.coerceIn(0, 48)
    val rws get() = rows.coerceIn(0, 48)
    val gut get() = gutter.coerceIn(0, 2000)
    val base get() = baseline.coerceIn(0, 2000)

    fun toJson(): String = JSONObject().apply {
        put("width", width); put("height", height); put("columns", columns); put("rows", rows)
        put("gutter", gutter); put("baseline", baseline)
        put("mt", marginTop); put("mb", marginBottom); put("ml", marginLeft); put("mr", marginRight)
        put("color", color); put("background", background); put("fillOpacity", fillOpacity.toDouble())
        put("transparent", transparent); put("centerLines", centerLines); put("thirds", thirds)
    }.toString()

    companion object {
        fun fromJson(s: String?): GridSpec = runCatching {
            val j = JSONObject(s!!)
            val d = GridSpec()
            GridSpec(
                j.optInt("width", d.width), j.optInt("height", d.height), j.optInt("columns", d.columns),
                j.optInt("rows", d.rows), j.optInt("gutter", d.gutter), j.optInt("baseline", d.baseline),
                j.optInt("mt", d.marginTop), j.optInt("mb", d.marginBottom), j.optInt("ml", d.marginLeft),
                j.optInt("mr", d.marginRight), j.optInt("color", d.color), j.optInt("background", d.background),
                j.optDouble("fillOpacity", d.fillOpacity.toDouble()).toFloat(), j.optBoolean("transparent"),
                j.optBoolean("centerLines"), j.optBoolean("thirds"),
            )
        }.getOrDefault(GridSpec())
    }
}

/** Positions in canvas pixels. */
data class GridGeometry(
    val innerW: Float, val innerH: Float,
    val colW: Float, val rowH: Float,
    val colX: List<Float>, val rowY: List<Float>,
)

fun GridSpec.geometry(): GridGeometry {
    val innerW = (w - marginLeft - marginRight).toFloat()
    val innerH = (h - marginTop - marginBottom).toFloat()
    val colW = if (cols > 0) (innerW - gut * (cols - 1)) / cols else 0f
    val rowH = if (rws > 0) (innerH - gut * (rws - 1)) / rws else 0f
    return GridGeometry(
        innerW, innerH, colW, rowH,
        List(cols) { marginLeft + it * (colW + gut) },
        List(rws) { marginTop + it * (rowH + gut) },
    )
}

private fun withAlpha(color: Int, a: Float): Int = (((a.coerceIn(0f, 1f) * 255).toInt()) shl 24) or (color and 0xFFFFFF)

/**
 * Draws the grid in canvas pixel coordinates (0..w, 0..h).
 * The same code paints the on-screen preview (scaled) and the exported PNG.
 */
fun GridSpec.draw(canvas: Canvas, underlay: Bitmap?) {
    val g = geometry()
    val lw = max(1f, min(w, h) / 540f)
    val wf = w.toFloat(); val hf = h.toFloat()

    if (!transparent) canvas.drawColor(background)
    underlay?.let { canvas.drawBitmap(it, null, RectF(0f, 0f, wf, hf), Paint(Paint.FILTER_BITMAP_FLAG)) }

    val fill = Paint().apply { color = withAlpha(this@draw.color, fillOpacity) }
    if (g.colW > 0 || g.rowH > 0) {
        when {
            cols > 0 && rws == 0 -> g.colX.forEach { x -> canvas.drawRect(x, marginTop.toFloat(), x + g.colW, marginTop + g.innerH, fill) }
            rws > 0 && cols == 0 -> g.rowY.forEach { y -> canvas.drawRect(marginLeft.toFloat(), y, marginLeft + g.innerW, y + g.rowH, fill) }
            cols > 0 && rws > 0 -> g.rowY.forEach { y -> g.colX.forEach { x -> canvas.drawRect(x, y, x + g.colW, y + g.rowH, fill) } }
        }
    }

    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = lw; color = withAlpha(this@draw.color, 0.9f) }
    canvas.drawRect(marginLeft + lw / 2, marginTop + lw / 2, marginLeft + g.innerW - lw / 2, marginTop + g.innerH - lw / 2, stroke)

    if (base > 0) {
        val bl = Paint().apply { style = Paint.Style.STROKE; strokeWidth = max(1f, lw / 2); color = withAlpha(this@draw.color, 0.35f) }
        var y = (marginTop + base).toFloat()
        while (y < hf - marginBottom) { canvas.drawLine(marginLeft.toFloat(), y, wf - marginRight, y, bl); y += base }
    }

    val dash = DashPathEffect(floatArrayOf(lw * 6, lw * 4), 0f)
    if (centerLines) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = lw; color = withAlpha(this@draw.color, 0.9f); pathEffect = dash }
        canvas.drawLine(wf / 2, 0f, wf / 2, hf, p); canvas.drawLine(0f, hf / 2, wf, hf / 2, p)
    }
    if (thirds) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = lw; color = 0xE63CA0FF.toInt(); pathEffect = dash }
        for (i in 1..2) {
            canvas.drawLine(wf * i / 3, 0f, wf * i / 3, hf, p); canvas.drawLine(0f, hf * i / 3, wf, hf * i / 3, p)
        }
    }
}

fun GridSpec.render(underlay: Bitmap?): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    draw(Canvas(bmp), underlay)
    return bmp
}

private fun r(v: Float) = "%.2f".format(java.util.Locale.US, v).trimEnd('0').trimEnd('.')
private fun rgba(c: Int, a: Float) = "rgba(${(c shr 16) and 255},${(c shr 8) and 255},${c and 255},${r(a)})"
private fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)

fun GridSpec.toSvg(): String {
    val g = geometry(); val f = rgba(color, fillOpacity); val s = rgba(color, 0.9f)
    return buildString {
        appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$w" height="$h" viewBox="0 0 $w $h">""")
        if (!transparent) appendLine("""  <rect width="100%" height="100%" fill="${hex(background)}"/>""")
        appendLine("""  <g id="grid" fill="$f">""")
        when {
            cols > 0 && rws == 0 -> g.colX.forEach { appendLine("""    <rect x="${r(it)}" y="$marginTop" width="${r(g.colW)}" height="${r(g.innerH)}"/>""") }
            rws > 0 && cols == 0 -> g.rowY.forEach { appendLine("""    <rect x="$marginLeft" y="${r(it)}" width="${r(g.innerW)}" height="${r(g.rowH)}"/>""") }
            cols > 0 && rws > 0 -> g.rowY.forEach { y -> g.colX.forEach { x -> appendLine("""    <rect x="${r(x)}" y="${r(y)}" width="${r(g.colW)}" height="${r(g.rowH)}"/>""") } }
        }
        appendLine("  </g>")
        appendLine("""  <rect id="margins" x="$marginLeft" y="$marginTop" width="${r(g.innerW)}" height="${r(g.innerH)}" fill="none" stroke="$s"/>""")
        if (base > 0) {
            appendLine("""  <g id="baseline" stroke="${rgba(color, 0.35f)}">""")
            var y = marginTop + base
            while (y < h - marginBottom) { appendLine("""    <line x1="$marginLeft" y1="$y" x2="${w - marginRight}" y2="$y"/>"""); y += base }
            appendLine("  </g>")
        }
        if (centerLines) appendLine("""  <g stroke="$s" stroke-dasharray="6 4"><line x1="${r(w / 2f)}" y1="0" x2="${r(w / 2f)}" y2="$h"/><line x1="0" y1="${r(h / 2f)}" x2="$w" y2="${r(h / 2f)}"/></g>""")
        if (thirds) {
            appendLine("""  <g stroke="rgba(60,160,255,0.9)" stroke-dasharray="6 4">""")
            for (i in 1..2) appendLine("""    <line x1="${r(w * i / 3f)}" y1="0" x2="${r(w * i / 3f)}" y2="$h"/><line x1="0" y1="${r(h * i / 3f)}" x2="$w" y2="${r(h * i / 3f)}"/>""")
            appendLine("  </g>")
        }
        append("</svg>")
    }
}

fun GridSpec.toCss(): String = buildString {
    appendLine(".container {")
    appendLine("  max-width: ${w}px;")
    appendLine("  padding: ${marginTop}px ${marginRight}px ${marginBottom}px ${marginLeft}px;")
    appendLine("  display: grid;")
    if (cols > 0) appendLine("  grid-template-columns: repeat($cols, 1fr);")
    if (rws > 0) appendLine("  grid-template-rows: repeat($rws, 1fr);")
    appendLine("  gap: ${gut}px;")
    append("}")
    if (base > 0) append("\n\n/* Baseline: ${base}px. Use line-heights that are multiples of it. */")
}

fun GridSpec.summary(): String {
    val g = geometry()
    return buildString {
        append("$w × $h px · content ${r(g.innerW)} × ${r(g.innerH)}")
        if (cols > 0) append(" · column ${r(g.colW)} px")
        if (rws > 0) append(" · row ${r(g.rowH)} px")
        if ((cols > 0 && g.colW <= 0) || (rws > 0 && g.rowH <= 0) || g.innerW <= 0 || g.innerH <= 0)
            append(" · margins or gutters are too large")
    }
}
