package app.palettez.grid

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import app.palettez.layout.LayoutEngine
import kotlin.math.roundToInt

@Composable
fun LayoutPreviewCanvas(
    rendered: LayoutEngine.RenderedLayout,
    modifier: Modifier = Modifier,
) {
    val bgColor = parseHexColor(rendered.backgroundColor)

    Canvas(
        modifier = modifier
            .aspectRatio((rendered.pageWidth / rendered.pageHeight).toFloat())
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
    ) {
        val sx = size.width / rendered.pageWidth.toFloat()
        val sy = size.height / rendered.pageHeight.toFloat()

        for (item in rendered.items) {
            val color = if (item.kind == LayoutEngine.ItemKind.Ring) {
                bgColor
            } else {
                val hex = rendered.panelColors.getOrElse(item.panelIndex) { "#FFFFFF" }
                parseHexColor(hex)
            }

            if (item.points.isNotEmpty()) {
                val path = Path().apply {
                    val p0 = item.points.first()
                    moveTo((p0.x * sx).toFloat(), (p0.y * sy).toFloat())
                    for (i in 1 until item.points.size) {
                        val p = item.points[i]
                        lineTo((p.x * sx).toFloat(), (p.y * sy).toFloat())
                    }
                    close()
                }
                drawPath(path, color = color)
            }
        }
    }
}

fun renderLayoutToBitmap(
    rendered: LayoutEngine.RenderedLayout,
    targetWidth: Int = 1080,
): Bitmap {
    val targetHeight = (targetWidth * (rendered.pageHeight / rendered.pageWidth)).roundToInt()
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Background
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor(rendered.backgroundColor)
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

    val sx = targetWidth.toFloat() / rendered.pageWidth.toFloat()
    val sy = targetHeight.toFloat() / rendered.pageHeight.toFloat()

    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    for (item in rendered.items) {
        val colorHex = if (item.kind == LayoutEngine.ItemKind.Ring) {
            rendered.backgroundColor
        } else {
            rendered.panelColors.getOrElse(item.panelIndex) { "#FFFFFF" }
        }
        paint.color = android.graphics.Color.parseColor(colorHex)

        if (item.points.isNotEmpty()) {
            val path = android.graphics.Path().apply {
                val p0 = item.points.first()
                moveTo((p0.x * sx).toFloat(), (p0.y * sy).toFloat())
                for (i in 1 until item.points.size) {
                    val p = item.points[i]
                    lineTo((p.x * sx).toFloat(), (p.y * sy).toFloat())
                }
                close()
            }
            canvas.drawPath(path, paint)
        }
    }

    return bitmap
}

internal fun parseHexColor(hex: String): Color {
    val clean = if (hex.startsWith("#")) hex else "#$hex"
    return try {
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        Color.White
    }
}
