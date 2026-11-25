package app.palettez.palette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import app.palettez.R
import app.palettez.color.ColorEngine
import kotlin.math.roundToInt

enum class PresentationAspectRatio(val label: String, val width: Int, val height: Int) {
    Ratio16x9("16:9 Slide", 1920, 1080),
    Ratio1x1("1:1 Square", 1080, 1080),
}

object ColorPresentationRenderer {

    fun render(
        context: Context,
        swatches: List<Swatch>,
        aspectRatio: PresentationAspectRatio = PresentationAspectRatio.Ratio16x9,
        projectName: String = "Brand Palette",
        isDark: Boolean = false,
        showCmykAndOklab: Boolean = true,
        showContrastBadge: Boolean = true,
        thumbnailBitmap: Bitmap? = null,
    ): Bitmap {
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Typefaces
        val fontAnton = runCatching {
            ResourcesCompat.getFont(context, R.font.anton_regular)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD

        val fontFigtree = runCatching {
            ResourcesCompat.getFont(context, R.font.figtree_semibold)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD

        val fontMono = runCatching {
            ResourcesCompat.getFont(context, R.font.jetbrains_mono_medium)
        }.getOrNull() ?: Typeface.MONOSPACE

        // Theme colors
        val groundColor = if (isDark) Color.parseColor("#141916") else Color.parseColor("#EFE9DF")
        val surfaceColor = if (isDark) Color.parseColor("#222A25") else Color.parseColor("#FBF8F3")
        val inkColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#151515")
        val inkMutedColor = if (isDark) Color.parseColor("#A8B0A9") else Color.parseColor("#5E584F")
        val lineColor = if (isDark) Color.parseColor("#323D36") else Color.parseColor("#D7CFC2")

        // 1. Fill ground background
        canvas.drawColor(groundColor)

        val margin = if (aspectRatio == PresentationAspectRatio.Ratio16x9) 64f else 48f

        // 2. Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontAnton
            textSize = if (aspectRatio == PresentationAspectRatio.Ratio16x9) 64f else 54f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 22f
        }

        val headerY = margin + 50f
        canvas.drawText(projectName.uppercase(), margin, headerY, titlePaint)
        canvas.drawText(
            "CURATED WITH PALETTEZ  •  ${swatches.size} HARMONIZED COLORS  •  LOCAL-FIRST",
            margin,
            headerY + 34f,
            subPaint
        )

        // Thumbnail photo in top-right if available
        if (thumbnailBitmap != null) {
            val thumbSize = if (aspectRatio == PresentationAspectRatio.Ratio16x9) 100f else 84f
            val thumbLeft = w - margin - thumbSize
            val thumbTop = margin
            val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize)
            val thumbScaled = Bitmap.createScaledBitmap(thumbnailBitmap, thumbSize.toInt(), thumbSize.toInt(), true)
            val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawRoundRect(thumbRect, 18f, 18f, thumbPaint.apply { color = surfaceColor })
            canvas.drawBitmap(thumbScaled, thumbLeft, thumbTop, null)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lineColor
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(thumbRect, 18f, 18f, borderPaint)
        }

        // 3. Bottom Proportional Ribbon & Footer
        val ribbonH = 32f
        val ribbonY = h - margin - 52f
        var currentRibbonX = margin
        val availableRibbonW = w - (margin * 2)

        val totalShare = swatches.sumOf { it.share.toDouble() }.toFloat().coerceAtLeast(1f)
        val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        swatches.forEach { s ->
            val swatchW = (s.share / totalShare) * availableRibbonW
            swatchPaint.color = s.argb
            canvas.drawRect(currentRibbonX, ribbonY, currentRibbonX + swatchW, ribbonY + ribbonH, swatchPaint)
            currentRibbonX += swatchW
        }

        // Outline ribbon
        val ribbonBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(margin, ribbonY, w - margin, ribbonY + ribbonH), 12f, 12f, ribbonBorder)

        // Footer info
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontMono
            textSize = 18f
        }
        canvas.drawText("PALETTEZ BRAND IDENTITY SPECIMEN", margin, h - margin, footerPaint)

        // WCAG Contrast Badge in footer if enabled
        if (showContrastBadge && swatches.isNotEmpty()) {
            val hexList = swatches.map { it.hex }
            val autoRoles = ColorEngine.autoRoles(hexList)
            val bgHex = autoRoles.background
            val primaryHex = autoRoles.primary
            val contrastRatio = ColorEngine.contrast(primaryHex, bgHex)
            val grade = ColorEngine.grade(contrastRatio)

            val badgeText = "WCAG $grade  •  ${String.format("%.1f", contrastRatio)}:1 Contrast"
            val badgeW = 280f
            val badgeH = 34f
            val badgeX = w - margin - badgeW
            val badgeY = h - margin - 30f
            val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)

            val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = surfaceColor
            }
            canvas.drawRoundRect(badgeRect, 17f, 17f, badgeBg)
            canvas.drawRoundRect(badgeRect, 17f, 17f, ribbonBorder)

            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                typeface = fontFigtree
                textSize = 17f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + 6f, badgeTextPaint)
        }

        // 4. Central Content: Swatches Cards Layout
        val contentTop = headerY + 60f
        val contentBottom = ribbonY - 32f
        val contentH = contentBottom - contentTop
        val contentW = w - (margin * 2)

        if (aspectRatio == PresentationAspectRatio.Ratio16x9) {
            // 16:9 Layout: Hero on Left, Grid of remaining swatches on Right
            val heroW = contentW * 0.38f
            val heroRect = RectF(margin, contentTop, margin + heroW, contentBottom)
            renderHeroCard(
                canvas = canvas,
                rect = heroRect,
                swatch = swatches.firstOrNull(),
                fontAnton = fontAnton,
                fontFigtree = fontFigtree,
                fontMono = fontMono,
                surfaceColor = surfaceColor,
                inkColor = inkColor,
                inkMutedColor = inkMutedColor,
                lineColor = lineColor,
                showOklab = showCmykAndOklab,
            )

            // Right side cards grid
            val rightLeft = margin + heroW + 28f
            val rightW = w - margin - rightLeft
            val remainingSwatches = swatches.drop(1)
            renderSwatchesGrid(
                canvas = canvas,
                rect = RectF(rightLeft, contentTop, rightLeft + rightW, contentBottom),
                swatches = remainingSwatches,
                fontAnton = fontAnton,
                fontFigtree = fontFigtree,
                fontMono = fontMono,
                surfaceColor = surfaceColor,
                inkColor = inkColor,
                inkMutedColor = inkMutedColor,
                lineColor = lineColor,
                showCmyk = showCmykAndOklab,
            )
        } else {
            // 1:1 Square Layout: Top Hero strip + 2 or 3-column swatch cards grid
            val heroH = contentH * 0.38f
            val heroRect = RectF(margin, contentTop, w - margin, contentTop + heroH)
            renderHeroHorizontal(
                canvas = canvas,
                rect = heroRect,
                swatch = swatches.firstOrNull(),
                fontAnton = fontAnton,
                fontFigtree = fontFigtree,
                fontMono = fontMono,
                surfaceColor = surfaceColor,
                inkColor = inkColor,
                inkMutedColor = inkMutedColor,
                lineColor = lineColor,
            )

            // Remaining cards below
            val gridTop = contentTop + heroH + 20f
            val gridRect = RectF(margin, gridTop, w - margin, contentBottom)
            renderSwatchesGrid(
                canvas = canvas,
                rect = gridRect,
                swatches = swatches.drop(1),
                fontAnton = fontAnton,
                fontFigtree = fontFigtree,
                fontMono = fontMono,
                surfaceColor = surfaceColor,
                inkColor = inkColor,
                inkMutedColor = inkMutedColor,
                lineColor = lineColor,
                showCmyk = showCmykAndOklab,
            )
        }

        return bmp
    }

    private fun renderHeroCard(
        canvas: Canvas,
        rect: RectF,
        swatch: Swatch?,
        fontAnton: Typeface,
        fontFigtree: Typeface,
        fontMono: Typeface,
        surfaceColor: Int,
        inkColor: Int,
        inkMutedColor: Int,
        lineColor: Int,
        showOklab: Boolean,
    ) {
        if (swatch == null) return

        // Card background
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
        canvas.drawRoundRect(rect, 28f, 28f, cardPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(rect, 28f, 28f, borderPaint)

        // Large Swatch Block inside hero
        val blockH = rect.height() * 0.44f
        val pad = 24f
        val swatchRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.top + pad + blockH)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.argb }
        canvas.drawRoundRect(swatchRect, 20f, 20f, fillPaint)

        // Scale bar underneath swatch block
        val scaleList = ColorEngine.scaleOf(swatch.hex)
        val scaleH = 24f
        val scaleY = swatchRect.bottom + 18f
        val stepW = (swatchRect.width()) / scaleList.size
        scaleList.forEachIndexed { i, step ->
            val stepX = swatchRect.left + (i * stepW)
            val stepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(step.hex)
            }
            canvas.drawRect(stepX, scaleY, stepX + stepW, scaleY + scaleH, stepPaint)
        }
        canvas.drawRoundRect(RectF(swatchRect.left, scaleY, swatchRect.right, scaleY + scaleH), 6f, 6f, borderPaint)

        // Text info
        val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontAnton
            textSize = 52f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 20f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontMono
            textSize = 20f
        }

        val textStartY = scaleY + scaleH + 46f
        canvas.drawText("PRIMARY BRAND COLOR", rect.left + pad, textStartY, labelPaint)
        canvas.drawText(swatch.hex, rect.left + pad, textStartY + 46f, hexPaint)

        // Readouts
        val readoutsY = textStartY + 96f
        val lineSpacing = 32f
        canvas.drawText("RGB:   ${swatch.rgb}", rect.left + pad, readoutsY, monoPaint)
        canvas.drawText("CMYK:  ${swatch.cmyk}", rect.left + pad, readoutsY + lineSpacing, monoPaint)
        canvas.drawText("HSL:   ${swatch.hsl}", rect.left + pad, readoutsY + (lineSpacing * 2), monoPaint)

        if (showOklab) {
            val ok = ColorEngine.toOklab(swatch.hex)
            val okText = "OKLab: L %.2f  a %.2f  b %.2f".format(ok.l, ok.a, ok.b)
            canvas.drawText(okText, rect.left + pad, readoutsY + (lineSpacing * 3), monoPaint)
        }
    }

    private fun renderHeroHorizontal(
        canvas: Canvas,
        rect: RectF,
        swatch: Swatch?,
        fontAnton: Typeface,
        fontFigtree: Typeface,
        fontMono: Typeface,
        surfaceColor: Int,
        inkColor: Int,
        inkMutedColor: Int,
        lineColor: Int,
    ) {
        if (swatch == null) return
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
        canvas.drawRoundRect(rect, 24f, 24f, cardPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        val pad = 20f
        val swatchW = rect.width() * 0.38f
        val swatchRect = RectF(rect.left + pad, rect.top + pad, rect.left + pad + swatchW, rect.bottom - pad)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.argb }
        canvas.drawRoundRect(swatchRect, 18f, 18f, fillPaint)

        val textLeft = swatchRect.right + 24f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 18f
        }
        val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontAnton
            textSize = 46f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontMono
            textSize = 18f
        }

        canvas.drawText("PRIMARY ACCENT COLOR", textLeft, rect.top + pad + 30f, labelPaint)
        canvas.drawText(swatch.hex, textLeft, rect.top + pad + 76f, hexPaint)
        canvas.drawText("RGB:   ${swatch.rgb}", textLeft, rect.top + pad + 120f, monoPaint)
        canvas.drawText("CMYK:  ${swatch.cmyk}", textLeft, rect.top + pad + 150f, monoPaint)
        canvas.drawText("HSL:   ${swatch.hsl}", textLeft, rect.top + pad + 180f, monoPaint)
    }

    private fun renderSwatchesGrid(
        canvas: Canvas,
        rect: RectF,
        swatches: List<Swatch>,
        fontAnton: Typeface,
        fontFigtree: Typeface,
        fontMono: Typeface,
        surfaceColor: Int,
        inkColor: Int,
        inkMutedColor: Int,
        lineColor: Int,
        showCmyk: Boolean,
    ) {
        if (swatches.isEmpty()) return

        val cols = if (swatches.size <= 4) 2 else if (swatches.size <= 8) 3 else 4
        val rows = ((swatches.size + cols - 1) / cols).coerceAtLeast(1)

        val gap = 16f
        val cardW = (rect.width() - (gap * (cols - 1))) / cols
        val cardH = (rect.height() - (gap * (rows - 1))) / rows

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontAnton
            textSize = if (cols >= 4) 26f else 32f
        }
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 16f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontMono
            textSize = if (cols >= 4) 14f else 16f
        }

        swatches.forEachIndexed { i, s ->
            val col = i % cols
            val row = i / cols
            val cardLeft = rect.left + (col * (cardW + gap))
            val cardTop = rect.top + (row * (cardH + gap))
            val cardRect = RectF(cardLeft, cardTop, cardLeft + cardW, cardTop + cardH)

            // Draw card bg
            canvas.drawRoundRect(cardRect, 18f, 18f, cardPaint)
            canvas.drawRoundRect(cardRect, 18f, 18f, borderPaint)

            // Swatch top block or circle
            val pad = 14f
            val swatchBlockH = cardH * 0.42f
            val swatchRect = RectF(cardLeft + pad, cardTop + pad, cardLeft + cardW - pad, cardTop + pad + swatchBlockH)
            fillPaint.color = s.argb
            canvas.drawRoundRect(swatchRect, 12f, 12f, fillPaint)

            // Hex & percentage
            val textY = swatchRect.bottom + 28f
            canvas.drawText(s.hex, cardLeft + pad, textY, hexPaint)

            val pctText = "${(s.share * 100).roundToInt()}%"
            canvas.drawText(pctText, cardLeft + cardW - pad - pctPaint.measureText(pctText), textY - 4f, pctPaint)

            // RGB / CMYK values
            val valY = textY + 24f
            canvas.drawText(s.rgb, cardLeft + pad, valY, monoPaint)
            if (showCmyk && cardH > 160f) {
                canvas.drawText(s.cmyk, cardLeft + pad, valY + 20f, monoPaint)
            }
        }
    }
}
