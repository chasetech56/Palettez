// MODULE-BOUNDARY: This file must remain free of Android imports (core/color / core/layout)
package app.palettez.layout

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure Kotlin panel layout engine ported from design/engines/layout-engine.js.
 * Zero Android dependencies — runs fast in JVM unit tests.
 *
 * Implements:
 * - Quadrilateral tree-of-cuts geometry
 * - Gutters, slants, and inset rings
 * - 15 built-in panel layout templates across 5 families
 * - Mulberry32 deterministic PRNG
 * - Procedural "Shuffle" layout generator (4–9 panels, 30 retries)
 * - Vector polygon calculations and SVG exporter
 */
object LayoutEngine {

    data class Point(val x: Double, val y: Double)

    sealed class LayoutNode {
        data class Split(
            val direction: String, // "h" (horizontal) or "v" (vertical)
            val ratios: List<Double>,
            val slants: List<Double>? = null,
            val children: List<LayoutNode?>? = null,
        ) : LayoutNode()

        data class Inset(
            val inset: List<Double>, // [x, y, w, h] as fraction 0..1 of bounding box
            val base: LayoutNode,
        ) : LayoutNode()
    }

    enum class ItemKind { Panel, Ring }

    data class PolygonItem(
        val points: List<Point>,
        val kind: ItemKind,
        val panelIndex: Int,
    )

    data class LayoutFamily(
        val key: String,
        val name: String,
        val rule: String,
    )

    data class LayoutTemplate(
        val name: String,
        val family: String,
        val node: LayoutNode,
    ) {
        val panelCount: Int by lazy { countPanels(node) }
    }

    data class RenderOptions(
        val gutter: Double = 24.0,
        val margin: Double = 40.0,
        val slant: Double = 1.0,
        val mirrorX: Boolean = false,
        val mirrorY: Boolean = false,
        val style: String = "paper", // "paper", "ink", "palette"
        val customColors: List<String>? = null,
    )

    data class RenderedLayout(
        val items: List<PolygonItem>,
        val panelCount: Int,
        val pageWidth: Double,
        val pageHeight: Double,
        val backgroundColor: String,
        val panelColors: List<String>,
    )

    val FAMILIES = listOf(
        LayoutFamily("classic", "Classic", "Straight tiers that read left to right, top to bottom. The safe default for steady pacing."),
        LayoutFamily("stacks", "Stacks", "Columns lead. Tall panels give height, falls and full-figure shots room."),
        LayoutFamily("slants", "Slants", "Tilted cuts add speed and tension. Keep each tilt small and alternate direction."),
        LayoutFamily("insets", "Insets", "One floating panel breaks the grid for the key moment, ringed by a gutter."),
        LayoutFamily("mosaic", "Mosaic", "Nested splits at golden and off-grid ratios. One big panel anchors the page."),
    )

    val DEFAULT_PALETTE = listOf(
        "#E4572E", "#F2A65A", "#8C5383", "#1B998B",
        "#F6E7CB", "#2E4057", "#AFCBEA", "#A7D86E"
    )

    val TEMPLATES: List<LayoutTemplate> = listOf(
        // Classic
        LayoutTemplate("Tiers", "classic", LayoutNode.Split("h", listOf(1.1, 1.0, 0.9), null, listOf(
            LayoutNode.Split("v", listOf(1.0, 1.0)),
            LayoutNode.Split("v", listOf(0.65, 1.35)),
            null
        ))),
        LayoutTemplate("Hero base", "classic", LayoutNode.Split("h", listOf(0.75, 0.75, 1.5), null, listOf(
            LayoutNode.Split("v", listOf(2.0, 1.0)),
            LayoutNode.Split("v", listOf(1.3, 0.75, 0.75)),
            null
        ))),
        LayoutTemplate("Brick", "classic", LayoutNode.Split("h", listOf(1.0, 1.0, 1.0), null, listOf(
            LayoutNode.Split("v", listOf(1.1, 1.0)),
            LayoutNode.Split("v", listOf(0.6, 1.4)),
            LayoutNode.Split("v", listOf(1.1, 0.9))
        ))),

        // Stacks
        LayoutTemplate("Tall left", "stacks", LayoutNode.Split("v", listOf(1.0, 1.0), null, listOf(
            null,
            LayoutNode.Split("h", listOf(1.0, 1.25))
        ))),
        LayoutTemplate("Triptych", "stacks", LayoutNode.Split("h", listOf(0.55, 1.45), null, listOf(
            LayoutNode.Split("v", listOf(0.6, 1.4)),
            LayoutNode.Split("v", listOf(1.2, 0.8, 0.8))
        ))),
        LayoutTemplate("Ladder", "stacks", LayoutNode.Split("v", listOf(1.1, 0.9), null, listOf(
            LayoutNode.Split("h", listOf(0.8, 0.6, 1.0)),
            null
        ))),

        // Slants
        LayoutTemplate("Tilt", "slants", LayoutNode.Split("h", listOf(0.8, 1.0, 1.0, 1.1), listOf(0.05, -0.06, 0.08), listOf(
            null,
            LayoutNode.Split("v", listOf(1.0, 1.0), listOf(0.12)),
            null,
            null
        ))),
        LayoutTemplate("Wedge", "slants", LayoutNode.Split("h", listOf(1.0, 1.5), listOf(0.14), listOf(
            LayoutNode.Split("v", listOf(1.0, 1.0), listOf(-0.08)),
            null
        ))),
        LayoutTemplate("Rush", "slants", LayoutNode.Split("h", listOf(1.0, 1.2), listOf(-0.1), listOf(
            LayoutNode.Split("v", listOf(1.0, 1.0, 1.0), listOf(0.1, 0.1)),
            LayoutNode.Split("v", listOf(1.0, 1.3), listOf(-0.14))
        ))),

        // Insets
        LayoutTemplate("Spotlight", "insets", LayoutNode.Inset(listOf(0.22, 0.5, 0.56, 0.34),
            LayoutNode.Split("h", listOf(0.55, 1.45), null, listOf(
                null,
                LayoutNode.Split("v", listOf(1.0, 1.0))
            ))
        )),
        LayoutTemplate("Corner pop", "insets", LayoutNode.Inset(listOf(0.52, 0.05, 0.43, 0.3),
            LayoutNode.Split("h", listOf(1.0, 1.0), null, listOf(
                LayoutNode.Split("v", listOf(1.0, 1.0)),
                null
            ))
        )),
        LayoutTemplate("Center stage", "insets", LayoutNode.Inset(listOf(0.17, 0.31, 0.66, 0.38),
            LayoutNode.Split("h", listOf(1.0, 1.0, 1.0), null, listOf(
                LayoutNode.Split("v", listOf(1.0, 1.0)),
                null,
                LayoutNode.Split("v", listOf(1.0, 1.0, 1.0))
            ))
        )),

        // Mosaic
        LayoutTemplate("Spiral", "mosaic", LayoutNode.Split("h", listOf(1.618, 1.0), null, listOf(
            null,
            LayoutNode.Split("v", listOf(1.618, 1.0), null, listOf(
                null,
                LayoutNode.Split("h", listOf(1.0, 1.618), null, listOf(
                    null,
                    LayoutNode.Split("v", listOf(1.0, 1.618))
                ))
            ))
        ))),
        LayoutTemplate("Off-grid", "mosaic", LayoutNode.Split("v", listOf(1.0, 1.4), null, listOf(
            LayoutNode.Split("h", listOf(1.0, 0.6, 1.0)),
            LayoutNode.Split("h", listOf(0.7, 1.3), null, listOf(
                null,
                LayoutNode.Split("v", listOf(1.0, 1.0))
            ))
        ))),
        LayoutTemplate("Window", "mosaic", LayoutNode.Split("h", listOf(0.5, 1.6, 0.5), null, listOf(
            LayoutNode.Split("v", listOf(1.6, 1.0)),
            LayoutNode.Split("v", listOf(0.5, 1.6, 0.5), null, listOf(
                null,
                null,
                LayoutNode.Split("h", listOf(1.0, 1.0))
            )),
            LayoutNode.Split("v", listOf(1.0, 1.6))
        )))
    )

    fun lerp(a: Point, b: Point, t: Double): Point =
        Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    fun dist(a: Point, b: Point): Double =
        max(1e-6, hypot(b.x - a.x, b.y - a.y))

    fun rect(x: Double, y: Double, w: Double, h: Double): List<Point> =
        listOf(Point(x, y), Point(x + w, y), Point(x + w, y + h), Point(x, y + h))

    fun countPanels(node: LayoutNode?): Int {
        if (node == null) return 1
        return when (node) {
            is LayoutNode.Inset -> countPanels(node.base) + 1
            is LayoutNode.Split -> {
                var total = 0
                val n = node.ratios.size
                for (i in 0 until n) {
                    val child = node.children?.getOrNull(i)
                    total += countPanels(child)
                }
                total
            }
        }
    }

    private fun splitNode(
        node: LayoutNode?,
        quad: List<Point>,
        gutter: Double,
        slantMult: Double,
        out: MutableList<PolygonItem>,
        nextIndex: () -> Int,
    ) {
        if (node == null) {
            out.add(PolygonItem(quad, ItemKind.Panel, nextIndex()))
            return
        }
        when (node) {
            is LayoutNode.Inset -> {
                splitNode(node.base, quad, gutter, slantMult, out, nextIndex)
                val xs = quad.map { it.x }
                val ys = quad.map { it.y }
                val x0 = xs.minOrNull() ?: 0.0
                val y0 = ys.minOrNull() ?: 0.0
                val bw = (xs.maxOrNull() ?: 0.0) - x0
                val bh = (ys.maxOrNull() ?: 0.0) - y0
                val i = node.inset
                val x = x0 + i[0] * bw
                val y = y0 + i[1] * bh
                val w = i[2] * bw
                val h = i[3] * bh
                out.add(PolygonItem(rect(x - gutter, y - gutter, w + 2 * gutter, h + 2 * gutter), ItemKind.Ring, -1))
                out.add(PolygonItem(rect(x, y, w, h), ItemKind.Panel, nextIndex()))
            }
            is LayoutNode.Split -> {
                val r = node.ratios
                val n = r.size
                val tot = r.sum()
                var acc = 0.0
                val cuts = mutableListOf<Pair<Double, Double>>()
                for (k in 0 until n - 1) {
                    acc += r[k]
                    val c = acc / tot
                    val s = ((node.slants?.getOrNull(k)) ?: 0.0) * slantMult
                    cuts.add(Pair(c - s / 2.0, c + s / 2.0))
                }
                val tl = quad[0]
                val tr = quad[1]
                val br = quad[2]
                val bl = quad[3]
                val (e1a, e1b, e2a, e2b) = if (node.direction == "v") {
                    listOf(tl, tr, bl, br)
                } else {
                    listOf(tl, bl, tr, br)
                }
                val d1 = gutter / 2.0 / dist(e1a, e1b)
                val d2 = gutter / 2.0 / dist(e2a, e2b)
                for (j in 0 until n) {
                    val s1 = if (j == 0) 0.0 else cuts[j - 1].first + d1
                    val s2 = if (j == 0) 0.0 else cuts[j - 1].second + d2
                    val t1 = if (j == n - 1) 1.0 else cuts[j].first - d1
                    val t2 = if (j == n - 1) 1.0 else cuts[j].second - d2
                    val A = lerp(e1a, e1b, s1)
                    val B = lerp(e1a, e1b, t1)
                    val C = lerp(e2a, e2b, t2)
                    val D = lerp(e2a, e2b, s2)
                    val nextQuad = if (node.direction == "v") listOf(A, B, C, D) else listOf(A, D, C, B)
                    val child = node.children?.getOrNull(j)
                    splitNode(child, nextQuad, gutter, slantMult, out, nextIndex)
                }
            }
        }
    }

    /**
     * Renders a layout tree to polygons.
     * Page canonical units: 1000 width × (1000 * height / width) height.
     */
    fun renderLayout(
        node: LayoutNode,
        width: Double,
        height: Double,
        options: RenderOptions = RenderOptions(),
    ): RenderedLayout {
        val pw = 1000.0
        val ph = 1000.0 * height / width
        val sc = width / pw

        val rawOut = mutableListOf<PolygonItem>()
        var panelCounter = 0
        splitNode(
            node = node,
            quad = rect(options.margin, options.margin, pw - 2 * options.margin, ph - 2 * options.margin),
            gutter = options.gutter,
            slantMult = options.slant,
            out = rawOut,
            nextIndex = { panelCounter++ },
        )

        // Apply mirroring and scaling
        val transformedItems = rawOut.map { item ->
            val pts = item.points.map { p ->
                val mx = if (options.mirrorX) pw - p.x else p.x
                val my = if (options.mirrorY) ph - p.y else p.y
                Point(mx * sc, my * sc)
            }
            PolygonItem(pts, item.kind, item.panelIndex)
        }

        // Determine colors
        val bg: String
        val colors: List<String>
        when (options.style) {
            "ink" -> {
                bg = "#151515"
                colors = (0 until panelCounter).map { "#FBF8F3" }
            }
            "palette" -> {
                bg = "#151515"
                val palette = options.customColors ?: DEFAULT_PALETTE
                colors = (0 until panelCounter).map { i -> palette[i % palette.size] }
            }
            else -> { // "paper"
                bg = "#D7CFC2"
                colors = (0 until panelCounter).map { "#FFFFFF" }
            }
        }

        return RenderedLayout(
            items = transformedItems,
            panelCount = panelCounter,
            pageWidth = width,
            pageHeight = height,
            backgroundColor = bg,
            panelColors = colors,
        )
    }

    /**
     * Seeded 32-bit PRNG matching JavaScript mulberry32.
     */
    class Mulberry32(private var seed: Int) {
        fun nextDouble(): Double {
            seed += 0x6D2B79F5
            var t = (seed xor (seed ushr 15)) * (1 or seed)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
            return unsigned.toDouble() / 4294967296.0
        }
    }

    /**
     * Procedural "Shuffle" layout generator matching LA-4 & LA-5.
     * Generates a new layout with 2-3 cuts per level, alternating direction,
     * ratios between 0.6 and 1.5, slant chance ~45%, and inset chance ~20%.
     * Guaranteed to produce 4 to 9 valid panels.
     */
    fun generateLayout(seed: Int, slantChance: Double = 0.45): LayoutNode {
        for (tries in 0 until 30) {
            val rand = Mulberry32(seed * 7919 + tries)

            fun grow(depth: Int, dir: String): LayoutNode? {
                if (depth > 0 && (depth >= 3 || rand.nextDouble() < 0.25 + depth * 0.2)) {
                    return null
                }
                val n = 2 + (rand.nextDouble() * (if (depth == 0) 3 else 2)).toInt()
                val r = mutableListOf<Double>()
                val s = mutableListOf<Double>()
                val c = mutableListOf<LayoutNode?>()

                for (i in 0 until n) {
                    r.add((((0.6 + rand.nextDouble() * 0.9) * 100.0).roundToInt()) / 100.0)
                }
                for (k in 0 until n - 1) {
                    val slant = if (rand.nextDouble() < slantChance) {
                        (((rand.nextDouble() - 0.5) * 0.24 * 100.0).roundToInt()) / 100.0
                    } else 0.0
                    s.add(slant)
                }
                for (j in 0 until n) {
                    c.add(grow(depth + 1, if (dir == "h") "v" else "h"))
                }
                return LayoutNode.Split(dir, r, s, c)
            }

            var node: LayoutNode? = grow(0, if (rand.nextDouble() < 0.75) "h" else "v")
            if (node != null && rand.nextDouble() < 0.2) {
                node = LayoutNode.Inset(
                    listOf(
                        0.15 + rand.nextDouble() * 0.3,
                        0.20 + rand.nextDouble() * 0.4,
                        0.40,
                        0.28
                    ),
                    node
                )
            }

            if (node != null) {
                val count = countPanels(node)
                if (count in 4..9) {
                    return node
                }
            }
        }
        return TEMPLATES[0].node
    }

    /**
     * Requirement LA-7:
     * Export SVG with named groups and one editable <polygon> per panel.
     */
    fun exportSvg(
        node: LayoutNode,
        width: Double,
        height: Double,
        options: RenderOptions = RenderOptions(),
    ): String {
        val layout = renderLayout(node, width, height, options)
        val sb = java.lang.StringBuilder()
        val w = width.roundToInt()
        val h = height.roundToInt()

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        sb.append("<svg width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\" xmlns=\"http://www.w3.org/2000/svg\">\n")
        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"${layout.backgroundColor}\" />\n")

        sb.append("  <g id=\"panels\">\n")
        layout.items.forEach { item ->
            val color = if (item.kind == ItemKind.Ring) {
                layout.backgroundColor
            } else {
                layout.panelColors.getOrElse(item.panelIndex) { "#FFFFFF" }
            }
            val ptsStr = item.points.joinToString(" ") { p ->
                "%.1f,%.1f".format(p.x, p.y)
            }
            val idAttr = if (item.kind == ItemKind.Ring) "class=\"inset-ring\"" else "id=\"panel-${item.panelIndex + 1}\""
            sb.append("    <polygon $idAttr points=\"$ptsStr\" fill=\"$color\" stroke=\"none\" />\n")
        }
        sb.append("  </g>\n")
        sb.append("</svg>")
        return sb.toString()
    }
}
