package app.palettez.grid

import android.graphics.Bitmap
import app.palettez.layout.LayoutEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Result of on-device layout and grid analysis (GA-1 .. GA-6).
 */
data class GridAnalysisResult(
    val estimatedColumns: Int,
    val estimatedGutterPx: Int,
    val estimatedMarginLeft: Int,
    val estimatedMarginRight: Int,
    val estimatedMarginTop: Int,
    val estimatedMarginBottom: Int,
    val confidence: Int, // 0..100%
    val detectedFamily: LayoutEngine.LayoutFamily,
    val recommendedTemplate: LayoutEngine.LayoutTemplate,
    val detectedAspectDescription: String,
    val columnDividers: List<Float>, // normalized 0..1
    val rowDividers: List<Float>, // normalized 0..1
    val imageWidth: Int,
    val imageHeight: Int,
) {
    fun toGridSpec(): GridSpec {
        return GridSpec(
            width = imageWidth,
            height = imageHeight,
            columns = estimatedColumns,
            rows = if (rowDividers.size in 2..6) rowDividers.size + 1 else 0,
            gutter = estimatedGutterPx,
            marginLeft = estimatedMarginLeft,
            marginRight = estimatedMarginRight,
            marginTop = estimatedMarginTop,
            marginBottom = estimatedMarginBottom,
        )
    }
}

/**
 * Pure on-device layout grid analyzer.
 * Uses Sobel gradient convolution, vertical/horizontal intensity projections,
 * and layout geometry classification to reverse-engineer posters, flyers,
 * and design compositions without any network traffic.
 */
object GridAnalysisEngine {

    fun analyze(bitmap: Bitmap): GridAnalysisResult {
        val origW = bitmap.width
        val origH = bitmap.height

        val scale = min(1f, 480f / max(origW, origH))
        val targetW = max(32, (origW * scale).toInt())
        val targetH = max(32, (origH * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

        val pixels = IntArray(targetW * targetH)
        scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        return analyzePixels(
            origW = origW,
            origH = origH,
            sampleW = targetW,
            sampleH = targetH,
            pixels = pixels
        )
    }

    /**
     * Pure algorithm running on a pixel buffer for testing and fast execution.
     */
    fun analyzePixels(
        origW: Int,
        origH: Int,
        sampleW: Int,
        sampleH: Int,
        pixels: IntArray
    ): GridAnalysisResult {
        val targetW = sampleW
        val targetH = sampleH

        // 1. Grayscale luminance buffer
        val lum = FloatArray(targetW * targetH)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // 2. Horizontal & Vertical Sobel Edge Gradient Filter
        val gradX = FloatArray(targetW * targetH)
        val gradY = FloatArray(targetW * targetH)
        var totalGrad = 0.0
        var slantScore = 0.0

        for (y in 1 until targetH - 1) {
            for (x in 1 until targetW - 1) {
                val idx = y * targetW + x
                // Sobel X: [[-1, 0, 1], [-2, 0, 2], [-1, 0, 1]]
                val gx = (
                    -1 * lum[(y - 1) * targetW + (x - 1)] + 1 * lum[(y - 1) * targetW + (x + 1)]
                    -2 * lum[y * targetW + (x - 1)]       + 2 * lum[y * targetW + (x + 1)]
                    -1 * lum[(y + 1) * targetW + (x - 1)] + 1 * lum[(y + 1) * targetW + (x + 1)]
                )
                // Sobel Y: [[-1, -2, -1], [0, 0, 0], [1, 2, 1]]
                val gy = (
                    -1 * lum[(y - 1) * targetW + (x - 1)] - 2 * lum[(y - 1) * targetW + x] - 1 * lum[(y - 1) * targetW + (x + 1)]
                    +1 * lum[(y + 1) * targetW + (x - 1)] + 2 * lum[(y + 1) * targetW + x] + 1 * lum[(y + 1) * targetW + (x + 1)]
                )
                gradX[idx] = abs(gx)
                gradY[idx] = abs(gy)
                val mag = sqrt((gx * gx + gy * gy).toDouble())
                totalGrad += mag

                // Diagonal energy indicates slanted panel cuts
                if (abs(gx) > 30f && abs(gy) > 30f) {
                    val angle = abs(abs(gx) - abs(gy)) / (abs(gx) + abs(gy))
                    if (angle < 0.4f) slantScore += mag
                }
            }
        }

        // 3. Projections
        val projX = FloatArray(targetW)
        val projY = FloatArray(targetH)

        for (x in 0 until targetW) {
            var sum = 0f
            for (y in 0 until targetH) sum += gradX[y * targetW + x]
            projX[x] = sum
        }

        for (y in 0 until targetH) {
            var sum = 0f
            for (x in 0 until targetW) sum += gradY[y * targetW + x]
            projY[y] = sum
        }

        // 4. Margins detection (quiet edge zones)
        val avgGradX = projX.average().toFloat()
        val avgGradY = projY.average().toFloat()
        val threshX = avgGradX * 0.45f
        val threshY = avgGradY * 0.45f

        var leftCut = 0
        while (leftCut < targetW / 4 && projX[leftCut] < threshX) leftCut++

        var rightCut = targetW - 1
        while (rightCut > targetW * 3 / 4 && projX[rightCut] < threshX) rightCut--

        var topCut = 0
        while (topCut < targetH / 4 && projY[topCut] < threshY) topCut++

        var bottomCut = targetH - 1
        while (bottomCut > targetH * 3 / 4 && projY[bottomCut] < threshY) bottomCut--

        val normMarginL = (leftCut.toFloat() / targetW).coerceIn(0.02f, 0.20f)
        val normMarginR = ((targetW - 1 - rightCut).toFloat() / targetW).coerceIn(0.02f, 0.20f)
        val normMarginT = (topCut.toFloat() / targetH).coerceIn(0.02f, 0.20f)
        val normMarginB = ((targetH - 1 - bottomCut).toFloat() / targetH).coerceIn(0.02f, 0.20f)

        // 5. Column & Row Peak Detection
        val colCandidates = listOf(1, 2, 3, 4, 6, 8, 12)
        var bestColScore = -1f
        var bestCols = 3

        val innerStart = (targetW * normMarginL).toInt()
        val innerEnd = (targetW * (1f - normMarginR)).toInt()
        val innerWidth = innerEnd - innerStart

        for (cols in colCandidates) {
            if (cols == 1) continue
            var score = 0f
            for (c in 1 until cols) {
                val expectedX = innerStart + (innerWidth * c) / cols
                val window = max(1, (innerWidth / (cols * 10)))
                var localMax = 0f
                for (wx in (expectedX - window)..(expectedX + window)) {
                    if (wx in 0 until targetW) {
                        localMax = max(localMax, projX[wx])
                    }
                }
                score += localMax
            }
            val avgScore = score / (cols - 1)
            val penalty = if (cols > 4) 0.85f else 1.0f
            val finalScore = avgScore * penalty
            if (finalScore > bestColScore) {
                bestColScore = finalScore
                bestCols = cols
            }
        }

        val bestGutterFrac = (1f / (bestCols * 8)).coerceIn(0.012f, 0.05f)

        // Row dividers from horizontal projection
        val rowDivs = mutableListOf<Float>()
        val rowWindow = targetH / 12
        for (y in (targetH * 0.2f).toInt() until (targetH * 0.8f).toInt() step rowWindow) {
            var peakY = y
            var maxV = 0f
            for (wy in (y - rowWindow / 2) until (y + rowWindow / 2)) {
                if (wy in 0 until targetH && projY[wy] > maxV) {
                    maxV = projY[wy]
                    peakY = wy
                }
            }
            if (maxV > avgGradY * 1.3f && rowDivs.none { abs(it - peakY.toFloat() / targetH) < 0.12f }) {
                rowDivs.add(peakY.toFloat() / targetH)
            }
        }

        // 6. Family Classification (LayoutEngine.FAMILIES)
        val aspect = origW.toFloat() / origH.toFloat()
        val aspectDesc = when {
            aspect in 0.95f..1.05f -> "1:1 Square"
            aspect in 0.70f..0.80f -> "3:4 Portrait"
            aspect in 0.52f..0.60f -> "9:16 Story"
            aspect in 1.70f..1.85f -> "16:9 Landscape"
            aspect > 1.05f -> "Landscape Poster"
            else -> "Portrait Flyer"
        }

        val slantRatio = if (totalGrad > 0) (slantScore / totalGrad).toFloat() else 0f

        val detectedFamilyKey = when {
            slantRatio > 0.18f -> "slants"
            bestCols == 2 && aspect < 0.75f -> "stacks"
            rowDivs.size >= 2 -> "classic"
            bestCols >= 3 -> "mosaic"
            else -> "classic"
        }

        val family = LayoutEngine.FAMILIES.firstOrNull { it.key == detectedFamilyKey }
            ?: LayoutEngine.FAMILIES.first()

        val template = LayoutEngine.TEMPLATES.firstOrNull { it.family == family.key }
            ?: LayoutEngine.TEMPLATES.first()

        val confidence = (75 + (min(20, (bestColScore / (avgGradX + 1f) * 10).toInt()))).coerceIn(72, 96)

        val columnDividers = List(bestCols - 1) { c ->
            normMarginL + (1f - normMarginL - normMarginR) * (c + 1) / bestCols
        }

        return GridAnalysisResult(
            estimatedColumns = bestCols,
            estimatedGutterPx = (origW * bestGutterFrac).roundToInt().coerceIn(12, 120),
            estimatedMarginLeft = (origW * normMarginL).roundToInt().coerceIn(16, 240),
            estimatedMarginRight = (origW * normMarginR).roundToInt().coerceIn(16, 240),
            estimatedMarginTop = (origH * normMarginT).roundToInt().coerceIn(16, 240),
            estimatedMarginBottom = (origH * normMarginB).roundToInt().coerceIn(16, 240),
            confidence = confidence,
            detectedFamily = family,
            recommendedTemplate = template,
            detectedAspectDescription = aspectDesc,
            columnDividers = columnDividers,
            rowDividers = rowDivs,
            imageWidth = origW,
            imageHeight = origH,
        )
    }
}
