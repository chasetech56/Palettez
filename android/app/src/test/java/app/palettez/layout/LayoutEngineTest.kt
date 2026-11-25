package app.palettez.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutEngineTest {

    /**
     * Requirement LA-1:
     * 15 built-in templates across 5 families (Classic, Stacks, Slants, Insets, Mosaic).
     * Done when: All 15 templates produce valid panel counts and non-empty rendered items.
     */
    @Test
    fun LA1_all15TemplatesValid() {
        assertEquals("Should have exactly 15 templates", 15, LayoutEngine.TEMPLATES.size)
        assertEquals("Should have 5 families", 5, LayoutEngine.FAMILIES.size)

        val familyCounts = LayoutEngine.TEMPLATES.groupBy { it.family }.mapValues { it.value.size }
        assertEquals(3, familyCounts["classic"])
        assertEquals(3, familyCounts["stacks"])
        assertEquals(3, familyCounts["slants"])
        assertEquals(3, familyCounts["insets"])
        assertEquals(3, familyCounts["mosaic"])

        for (template in LayoutEngine.TEMPLATES) {
            val count = template.panelCount
            assertTrue("Template ${template.name} panelCount $count should be >= 2", count >= 2)

            val rendered = LayoutEngine.renderLayout(template.node, 1000.0, 1000.0)
            assertEquals(count, rendered.panelCount)
            assertTrue(rendered.items.isNotEmpty())
        }
    }

    /**
     * Requirement LA-4:
     * Procedural "Shuffle" layout generator with 2-3 cuts per level, alternating direction,
     * ratios between 0.6 and 1.5, optional small slants and occasional insets.
     * Done when: Property test generating 1,000 layouts from seeds 1..1000 yields 4..9 panels for every layout.
     */
    @Test
    fun LA4_thousandSeedPropertyTest() {
        for (seed in 1..1000) {
            val node = LayoutEngine.generateLayout(seed, 0.45)
            val count = LayoutEngine.countPanels(node)
            assertTrue(
                "Seed $seed produced $count panels, expected 4..9",
                count in 4..9
            )
        }
    }

    /**
     * Requirement LA-5:
     * Deterministic generation: seed ensures repeatable layout generation.
     */
    @Test
    fun LA5_deterministicSeed() {
        val testSeeds = listOf(1, 42, 137, 7919, 99999)
        for (seed in testSeeds) {
            val node1 = LayoutEngine.generateLayout(seed, 0.45)
            val node2 = LayoutEngine.generateLayout(seed, 0.45)
            assertEquals("Seed $seed must produce identical node structure", node1, node2)
        }
    }

    /**
     * Requirement LA-7:
     * Export SVG with named groups and one editable <polygon> per panel.
     */
    @Test
    fun LA7_exportSvg() {
        val template = LayoutEngine.TEMPLATES.first()
        val svg = LayoutEngine.exportSvg(template.node, 800.0, 600.0)
        assertTrue("SVG should contain xml header", svg.contains("<?xml"))
        assertTrue("SVG should contain svg element", svg.contains("<svg width=\"800\" height=\"600\""))
        assertTrue("SVG should contain background rect", svg.contains("<rect width=\"100%\" height=\"100%\""))
        assertTrue("SVG should contain panels group", svg.contains("<g id=\"panels\">"))
        assertTrue("SVG should contain polygon elements", svg.contains("<polygon id=\"panel-1\""))
    }
}
