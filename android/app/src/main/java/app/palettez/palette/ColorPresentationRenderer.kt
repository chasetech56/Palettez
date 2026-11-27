package app.palettez.palette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
        thumbnailBitmap: Bitmap? = null, // kept for API compatibility, not rendered
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

        val fontFigtreeRegular = runCatching {
            ResourcesCompat.getFont(context, R.font.figtree_regular)
        }.getOrNull() ?: Typeface.DEFAULT

        val fontMono = runCatching {
            ResourcesCompat.getFont(context, R.font.jetbrains_mono_medium)
        }.getOrNull() ?: Typeface.MONOSPACE

        // Theme colors matching Bold Edition tokens
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
            textSize = if (aspectRatio == PresentationAspectRatio.Ratio16x9) 62f else 52f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 20f
        }

        val headerY = margin + 50f
        canvas.drawText(projectName.uppercase(), margin, headerY, titlePaint)
        canvas.drawText(
            "CURATED WITH PALETTEZ  •  ${swatches.size} HARMONIZED COLORS  •  LOCAL-FIRST",
            margin,
            headerY + 32f,
            subPaint
        )

        // Top-Right: Clean specimen badge pill (replaces reference image)
        val pillH = 38f
        val pillW = 270f
        val pillRight = w - margin
        val pillLeft = pillRight - pillW
        val pillTop = margin + 14f
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillTop + pillH)

        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = surfaceColor
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint)
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBorderPaint)

        // Status dot on pill (primary color dot)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = swatches.firstOrNull()?.argb ?: Color.parseColor("#A7D86E")
        }
        canvas.drawCircle(pillLeft + 20f, pillRect.centerY(), 5.5f, dotPaint)

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontMono
            textSize = 15f
        }
        canvas.drawText("PALETTEZ SPECIMEN", pillLeft + 36f, pillRect.centerY() + 5.5f, pillTextPaint)

        // 3. Bottom Proportional Distribution Ribbon & Footer
        val ribbonH = 26f
        val ribbonY = h - margin - 48f
        var currentRibbonX = margin
        val availableRibbonW = w - (margin * 2)

        val totalShare = swatches.sumOf { it.share.toDouble() }.toFloat().coerceAtLeast(1f)
        val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw segmented ribbon with rounded outer clip
        val ribbonPath = Path().apply {
            addRoundRect(RectF(margin, ribbonY, w - margin, ribbonY + ribbonH), 13f, 13f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(ribbonPath)

        val gap = 2f
        swatches.forEachIndexed { idx, s ->
            val swatchW = (s.share / totalShare) * availableRibbonW
            swatchPaint.color = s.argb
            val segLeft = if (idx == 0) currentRibbonX else currentRibbonX + (gap / 2f)
            val segRight = if (idx == swatches.size - 1) currentRibbonX + swatchW else currentRibbonX + swatchW - (gap / 2f)
            canvas.drawRect(segLeft, ribbonY, segRight, ribbonY + ribbonH, swatchPaint)
            currentRibbonX += swatchW
        }
        canvas.restore()

        // Outline ribbon
        val ribbonBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(margin, ribbonY, w - margin, ribbonY + ribbonH), 13f, 13f, ribbonBorder)

        // Footer left info
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontMono
            textSize = 16f
        }
        canvas.drawText("PALETTEZ BRAND SPECIMEN  •  sRGB / OKLab COLOR SPACE", margin, h - margin, footerPaint)

        // WCAG Contrast Badge in footer if enabled
        if (showContrastBadge && swatches.isNotEmpty()) {
            val hexList = swatches.map { it.hex }
            val autoRoles = ColorEngine.autoRoles(hexList)
            val bgHex = autoRoles.background
            val primaryHex = autoRoles.primary
            val contrastRatio = ColorEngine.contrast(primaryHex, bgHex)
            val grade = ColorEngine.grade(contrastRatio)

            val badgeText = "WCAG $grade  •  ${String.format("%.1f", contrastRatio)}:1 Contrast"
            val badgeW = 290f
            val badgeH = 34f
            val badgeX = w - margin - badgeW
            val badgeY = h - margin - 28f
            val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)

            val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
            canvas.drawRoundRect(badgeRect, 17f, 17f, badgeBg)
            canvas.drawRoundRect(badgeRect, 17f, 17f, ribbonBorder)

            // Contrast grade dot: green for AAA/AA, amber for AA Large, red for Fail
            val gradeDotColor = when (grade) {
                "AAA", "AA" -> Color.parseColor("#4E7A5B")
                "AA Large" -> Color.parseColor("#F4C542")
                else -> Color.parseColor("#E2603F")
            }
            val gradeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gradeDotColor }
            canvas.drawCircle(badgeX + 20f, badgeRect.centerY(), 5f, gradeDotPaint)

            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                typeface = fontFigtree
                textSize = 16f
            }
            canvas.drawText(badgeText, badgeX + 36f, badgeRect.centerY() + 5.5f, badgeTextPaint)
        }

        // 4. Central Content Area
        val contentTop = headerY + 54f
        val contentBottom = ribbonY - 28f
        val contentH = contentBottom - contentTop
        val contentW = w - (margin * 2)

        if (aspectRatio == PresentationAspectRatio.Ratio16x9) {
            // 16:9 Slide: Left Hero Card (40% width), Right Grid (60% width)
            val heroW = contentW * 0.40f
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
            val rightLeft = margin + heroW + 24f
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
            // 1:1 Square: Top Hero Card + Bottom Grid
            val heroH = contentH * 0.40f
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
                showOklab = showCmykAndOklab,
            )

            // Remaining cards below
            val gridTop = contentTop + heroH + 18f
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

        // Large Swatch Block inside hero (46% height of card)
        val pad = 24f
        val blockH = rect.height() * 0.46f
        val swatchRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.top + pad + blockH)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.argb }
        canvas.drawRoundRect(swatchRect, 20f, 20f, fillPaint)

        // Contrast-aware pill badge in top-right of the swatch
        val textColorHex = ColorEngine.inkOn(swatch.hex)
        val textColor = Color.parseColor(textColorHex)
        val shareText = "DOMINANT  •  ${(swatch.share * 100).roundToInt()}%"
        val shareBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontFigtree
            textSize = 15f
        }
        val textWidth = shareBadgePaint.measureText(shareText)
        val badgeW = textWidth + 24f
        val badgeH = 30f
        val badgeRect = RectF(swatchRect.right - badgeW - 14f, swatchRect.top + 14f, swatchRect.right - 14f, swatchRect.top + 14f + badgeH)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (textColorHex == "#FFFFFF") Color.parseColor("#44000000") else Color.parseColor("#44FFFFFF")
        }
        canvas.drawRoundRect(badgeRect, 15f, 15f, badgeBgPaint)
        shareBadgePaint.color = textColor
        canvas.drawText(shareText, badgeRect.left + 12f, badgeRect.centerY() + 5f, shareBadgePaint)

        // Scale bar underneath swatch block: 10 rounded pill segments with gaps
        val scaleList = ColorEngine.scaleOf(swatch.hex)
        val scaleH = 22f
        val scaleY = swatchRect.bottom + 16f
        val gap = 3f
        val availableScaleW = swatchRect.width() - (gap * (scaleList.size - 1))
        val stepW = availableScaleW / scaleList.size
        val stepPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        scaleList.forEachIndexed { i, step ->
            val stepLeft = swatchRect.left + (i * (stepW + gap))
            val stepRect = RectF(stepLeft, scaleY, stepLeft + stepW, scaleY + scaleH)
            stepPaint.color = Color.parseColor(step.hex)
            canvas.drawRoundRect(stepRect, 4f, 4f, stepPaint)
        }

        // Color Details Section
        val textStartY = scaleY + scaleH + 42f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 18f
        }
        val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontAnton
            textSize = 58f
        }
        canvas.drawText("PRIMARY BRAND COLOR", rect.left + pad, textStartY, labelPaint)
        canvas.drawText(swatch.hex, rect.left + pad, textStartY + 50f, hexPaint)

        // Two-column layout for color metrics
        val metricsTop = textStartY + 96f
        val colWidth = (swatchRect.width() - 16f) / 2f
        val col1Left = rect.left + pad
        val col2Left = col1Left + colWidth + 16f

        val metricKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 15f
        }
        val metricValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontMono
            textSize = 17f
        }

        val cmyk = swatch.cmykValues
        val hsl = swatch.hslValues
        val ok = ColorEngine.toOklab(swatch.hex)

        // Col 1: RGB & CMYK
        canvas.drawText("RGB", col1Left, metricsTop, metricKeyPaint)
        canvas.drawText("${swatch.r} · ${swatch.g} · ${swatch.b}", col1Left, metricsTop + 24f, metricValPaint)

        canvas.drawText("CMYK", col1Left, metricsTop + 62f, metricKeyPaint)
        canvas.drawText("${cmyk[0]}% · ${cmyk[1]}% · ${cmyk[2]}% · ${cmyk[3]}%", col1Left, metricsTop + 86f, metricValPaint)

        // Col 2: HSL & OKLab
        canvas.drawText("HSL", col2Left, metricsTop, metricKeyPaint)
        canvas.drawText("${hsl[0]}° · ${hsl[1]}% · ${hsl[2]}%", col2Left, metricsTop + 24f, metricValPaint)

        if (showOklab) {
            canvas.drawText("OKLab (Uniform L·a·b)", col2Left, metricsTop + 62f, metricKeyPaint)
            val okFormatted = "${String.format("%.2f", ok.l)} · ${String.format("%.2f", ok.a)} · ${String.format("%.2f", ok.b)}"
            canvas.drawText(okFormatted, col2Left, metricsTop + 86f, metricValPaint)
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
        showOklab: Boolean,
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
        val swatchW = rect.width() * 0.40f
        val swatchRect = RectF(rect.left + pad, rect.top + pad, rect.left + pad + swatchW, rect.bottom - pad)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.argb }
        canvas.drawRoundRect(swatchRect, 18f, 18f, fillPaint)

        val textLeft = swatchRect.right + 24f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 17f
        }
        val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontAnton
            textSize = 50f
        }
        val metricKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 14f
        }
        val metricValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontMono
            textSize = 16f
        }

        canvas.drawText("PRIMARY BRAND COLOR", textLeft, rect.top + pad + 24f, labelPaint)
        canvas.drawText(swatch.hex, textLeft, rect.top + pad + 68f, hexPaint)

        val cmyk = swatch.cmykValues
        val hsl = swatch.hslValues
        val valTop = rect.top + pad + 104f

        canvas.drawText("RGB", textLeft, valTop, metricKeyPaint)
        canvas.drawText("${swatch.r} · ${swatch.g} · ${swatch.b}", textLeft + 54f, valTop, metricValPaint)

        canvas.drawText("CMYK", textLeft, valTop + 28f, metricKeyPaint)
        canvas.drawText("${cmyk[0]}% · ${cmyk[1]}% · ${cmyk[2]}% · ${cmyk[3]}%", textLeft + 54f, valTop + 28f, metricValPaint)

        canvas.drawText("HSL", textLeft, valTop + 56f, metricKeyPaint)
        canvas.drawText("${hsl[0]}° · ${hsl[1]}% · ${hsl[2]}%", textLeft + 54f, valTop + 56f, metricValPaint)
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
            textSize = if (cols >= 4) 26f else 34f
        }
        val monoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMutedColor
            typeface = fontFigtree
            textSize = 13f
        }
        val monoValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = fontMono
            textSize = if (cols >= 4) 14f else 15f
        }

        swatches.forEachIndexed { i, s ->
            val col = i % cols
            val row = i / cols
            val cardLeft = rect.left + (col * (cardW + gap))
            val cardTop = rect.top + (row * (cardH + gap))
            val cardRect = RectF(cardLeft, cardTop, cardLeft + cardW, cardTop + cardH)

            // Draw card surface
            canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)
            canvas.drawRoundRect(cardRect, 20f, 20f, borderPaint)

            // Swatch top block (50% of card height)
            val pad = 16f
            val swatchBlockH = cardH * 0.50f
            val swatchRect = RectF(cardLeft + pad, cardTop + pad, cardLeft + cardW - pad, cardTop + pad + swatchBlockH)
            fillPaint.color = s.argb
            canvas.drawRoundRect(swatchRect, 14f, 14f, fillPaint)

            // Contrast-aware share pill on the top right inside swatch
            val textColorHex = ColorEngine.inkOn(s.hex)
            val textColor = Color.parseColor(textColorHex)
            val pctText = "${(s.share * 100).roundToInt()}%"
            val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                typeface = fontFigtree
                textSize = 14f
            }
            val pctWidth = pctPaint.measureText(pctText)
            val pillW = pctWidth + 18f
            val pillH = 26f
            val pillRect = RectF(swatchRect.right - pillW - 10f, swatchRect.top + 10f, swatchRect.right - 10f, swatchRect.top + 10f + pillH)
            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (textColorHex == "#FFFFFF") Color.parseColor("#44000000") else Color.parseColor("#44FFFFFF")
            }
            canvas.drawRoundRect(pillRect, 13f, 13f, pillBg)
            canvas.drawText(pctText, pillRect.left + 9f, pillRect.centerY() + 5f, pctPaint)

            // Hex Code
            val textY = swatchRect.bottom + 32f
            canvas.drawText(s.hex, cardLeft + pad, textY, hexPaint)

            // Clean RGB & CMYK readouts
            val cmyk = s.cmykValues
            val val1Y = textY + 26f
            canvas.drawText("RGB", cardLeft + pad, val1Y, monoLabelPaint)
            canvas.drawText("${s.r} · ${s.g} · ${s.b}", cardLeft + pad + 40f, val1Y, monoValPaint)

            if (showCmyk && cardH > 170f) {
                val val2Y = val1Y + 22f
                canvas.drawText("CMYK", cardLeft + pad, val2Y, monoLabelPaint)
                canvas.drawText("${cmyk[0]}% · ${cmyk[1]}% · ${cmyk[2]}% · ${cmyk[3]}%", cardLeft + pad + 40f, val2Y, monoValPaint)
            }
        }
    }
}
