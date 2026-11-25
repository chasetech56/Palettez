package app.palettez.palette

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Advanced perceptual color extractor for Palettez.
 *
 * Uses:
 * 1. OKLab color space for mathematically uniform human perceptual difference (Delta E).
 * 2. Vibrancy & chromatic salience weighting in k-means++ seeding to retain vivid accents
 *    (e.g., clothing, eyes, sunsets) even when a background dominates the image area.
 * 3. Perceptual deduplication: merges clusters that are too close (Delta E < 0.07) so the user
 *    never receives redundant shades of the background.
 * 4. Deterministic fixed seed (42) for reproducible results.
 */
object PaletteExtractor {

    data class LabPoint(val l: Float, val a: Float, val b: Float, val chroma: Float, val weight: Float)

    /** Reads pixels from [bitmap] scaled to max 240px side for accurate color density. */
    fun samplePixels(bitmap: Bitmap, maxSide: Int = 240): IntArray {
        val longest = maxOf(bitmap.width, bitmap.height)
        val small = if (longest > maxSide) {
            val s = maxSide.toFloat() / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * s).toInt().coerceAtLeast(1),
                (bitmap.height * s).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap

        val all = IntArray(small.width * small.height)
        small.getPixels(all, 0, small.width, 0, 0, small.width, small.height)
        if (small !== bitmap) small.recycle()

        // Filter out transparent and near-transparent pixels
        return all.filter { (it ushr 24) > 125 }.map { it and 0xFFFFFF }.toIntArray()
    }

    fun extract(pixels: IntArray, count: Int): List<Swatch> {
        if (pixels.isEmpty()) return emptyList()
        val n = pixels.size
        val k = minOf(count, pixels.distinct().size).coerceAtLeast(1)

        // Convert pixels to OKLab points with vibrancy weighting
        val pr = IntArray(n) { (pixels[it] shr 16) and 0xFF }
        val pg = IntArray(n) { (pixels[it] shr 8) and 0xFF }
        val pb = IntArray(n) { pixels[it] and 0xFF }

        val points = Array(n) { i ->
            val (l, a, b) = rgbToOklab(pr[i], pg[i], pb[i])
            val chroma = sqrt(a * a + b * b)
            // Vibrancy bonus: favor chromatic colors, de-emphasize pure blinding white and deep mud black
            val centerLightness = 1.0f - abs(l - 0.5f) * 1.6f
            val weight = 1.0f + 4.0f * chroma * centerLightness.coerceIn(0.1f, 1.0f)
            LabPoint(l, a, b, chroma, weight)
        }

        val rnd = Random(42)

        // k-means++ seeding in OKLab space with vibrancy weighting
        val centroidsL = FloatArray(k)
        val centroidsA = FloatArray(k)
        val centroidsB = FloatArray(k)

        val first = rnd.nextInt(n)
        centroidsL[0] = points[first].l
        centroidsA[0] = points[first].a
        centroidsB[0] = points[first].b

        val minDistances = FloatArray(n) { Float.MAX_VALUE }

        for (c in 1 until k) {
            var totalWeight = 0.0
            val prevL = centroidsL[c - 1]
            val prevA = centroidsA[c - 1]
            val prevB = centroidsB[c - 1]

            for (i in 0 until n) {
                val p = points[i]
                val d2 = distSq(p.l, p.a, p.b, prevL, prevA, prevB)
                if (d2 < minDistances[i]) minDistances[i] = d2
                // Probability proportional to squared perceptual distance * vibrancy weight
                totalWeight += minDistances[i] * p.weight
            }

            var target = rnd.nextDouble() * totalWeight
            var pick = n - 1
            for (i in 0 until n) {
                target -= (minDistances[i] * points[i].weight)
                if (target <= 0.0) {
                    pick = i
                    break
                }
            }
            centroidsL[c] = points[pick].l
            centroidsA[c] = points[pick].a
            centroidsB[c] = points[pick].b
        }

        // Lloyd iterations in OKLab
        val assign = IntArray(n) { -1 }
        val counts = IntArray(k)

        for (iter in 0 until 20) {
            val sumL = DoubleArray(k)
            val sumA = DoubleArray(k)
            val sumB = DoubleArray(k)
            val sumR = DoubleArray(k)
            val sumG = DoubleArray(k)
            val sumBInt = DoubleArray(k)
            counts.fill(0)
            var moved = false

            for (i in 0 until n) {
                val p = points[i]
                var best = 0
                var bestDist = Float.MAX_VALUE
                for (c in 0 until k) {
                    val d2 = distSq(p.l, p.a, p.b, centroidsL[c], centroidsA[c], centroidsB[c])
                    if (d2 < bestDist) {
                        bestDist = d2
                        best = c
                    }
                }
                if (assign[i] != best) {
                    assign[i] = best
                    moved = true
                }
                sumL[best] += p.l.toDouble()
                sumA[best] += p.a.toDouble()
                sumB[best] += p.b.toDouble()
                sumR[best] += pr[i].toDouble()
                sumG[best] += pg[i].toDouble()
                sumBInt[best] += pb[i].toDouble()
                counts[best]++
            }

            for (c in 0 until k) {
                if (counts[c] > 0) {
                    centroidsL[c] = (sumL[c] / counts[c]).toFloat()
                    centroidsA[c] = (sumA[c] / counts[c]).toFloat()
                    centroidsB[c] = (sumB[c] / counts[c]).toFloat()
                }
            }

            if (!moved && iter > 0) break
        }

        // Build raw candidate clusters
        data class RawCluster(val r: Int, val g: Int, val b: Int, val count: Int, val l: Float, val a: Float, val bCoord: Float)

        val clusters = (0 until k).filter { counts[it] > 0 }.map { c ->
            var sumR = 0.0
            var sumG = 0.0
            var sumBInt = 0.0
            for (i in 0 until n) {
                if (assign[i] == c) {
                    sumR += pr[i]
                    sumG += pg[i]
                    sumBInt += pb[i]
                }
            }
            val r = (sumR / counts[c]).roundToInt().coerceIn(0, 255)
            val g = (sumG / counts[c]).roundToInt().coerceIn(0, 255)
            val b = (sumBInt / counts[c]).roundToInt().coerceIn(0, 255)
            RawCluster(r, g, b, counts[c], centroidsL[c], centroidsA[c], centroidsB[c])
        }.toMutableList()

        // Perceptual deduplication: merge clusters with Delta E < 0.07 in OKLab
        val merged = mutableListOf<RawCluster>()
        for (candidate in clusters.sortedByDescending { it.count }) {
            val duplicateIndex = merged.indexOfFirst {
                val de = sqrt(distSq(candidate.l, candidate.a, candidate.bCoord, it.l, it.a, it.bCoord))
                de < 0.065f
            }
            if (duplicateIndex >= 0) {
                val existing = merged[duplicateIndex]
                val totalCount = existing.count + candidate.count
                val newR = ((existing.r * existing.count + candidate.r * candidate.count) / totalCount.toDouble()).roundToInt().coerceIn(0, 255)
                val newG = ((existing.g * existing.count + candidate.g * candidate.count) / totalCount.toDouble()).roundToInt().coerceIn(0, 255)
                val newB = ((existing.b * existing.count + candidate.b * candidate.count) / totalCount.toDouble()).roundToInt().coerceIn(0, 255)
                val (newL, newA, newBCoord) = rgbToOklab(newR, newG, newB)
                merged[duplicateIndex] = RawCluster(newR, newG, newB, totalCount, newL, newA, newBCoord)
            } else {
                merged.add(candidate)
            }
        }

        // Return swatches with relative share
        val totalAssigned = merged.sumOf { it.count }.coerceAtLeast(1)
        return merged.map {
            Swatch(
                r = it.r,
                g = it.g,
                b = it.b,
                share = it.count.toFloat() / totalAssigned
            )
        }
    }

    private fun distSq(l1: Float, a1: Float, b1: Float, l2: Float, a2: Float, b2: Float): Float {
        val dl = l1 - l2
        val da = a1 - a2
        val db = b1 - b2
        return dl * dl + da * da + db * db
    }

    private fun toLin(v: Int): Double {
        val vf = v / 255.0
        return if (vf <= 0.04045) vf / 12.92 else ((vf + 0.055) / 1.055).pow(2.4)
    }

    private fun rgbToOklab(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rLin = toLin(r)
        val gLin = toLin(g)
        val bLin = toLin(b)

        val l = cbrt(0.4122214708 * rLin + 0.5363325363 * gLin + 0.0514459929 * bLin)
        val m = cbrt(0.2119034982 * rLin + 0.6806995451 * gLin + 0.1073969566 * bLin)
        val s = cbrt(0.0883024619 * rLin + 0.2817188376 * gLin + 0.6299787005 * bLin)

        val okL = (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s).toFloat()
        val okA = (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s).toFloat()
        val okB = (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s).toFloat()

        return Triple(okL, okA, okB)
    }
}
