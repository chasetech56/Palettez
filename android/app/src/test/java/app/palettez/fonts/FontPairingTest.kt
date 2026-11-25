package app.palettez.fonts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FontPairingTest {

    private val sampleCatalog = listOf(
        FontInfo("Inter", FontCategory.SansSerif, listOf(FontStyle(400, false), FontStyle(700, false)), 1, "2020-01-01", false),
        FontInfo("Playfair Display", FontCategory.Serif, listOf(FontStyle(400, false), FontStyle(700, true)), 5, "2020-01-01", false),
        FontInfo("Figtree", FontCategory.SansSerif, listOf(FontStyle(400, false), FontStyle(600, false)), 10, "2022-01-01", false),
        FontInfo("Anton", FontCategory.Display, listOf(FontStyle(400, false)), 15, "2020-01-01", false),
        FontInfo("JetBrains Mono", FontCategory.Monospace, listOf(FontStyle(400, false)), 20, "2021-01-01", false),
        FontInfo("Montserrat", FontCategory.SansSerif, listOf(FontStyle(400, false)), 2, "2020-01-01", false),
        FontInfo("Merriweather", FontCategory.Serif, listOf(FontStyle(400, false)), 3, "2020-01-01", false),
        FontInfo("Fraunces", FontCategory.Serif, listOf(FontStyle(400, false)), 12, "2021-01-01", false),
    )

    @Test
    fun FP1_curatedPairingsForAnton() {
        val anton = sampleCatalog.first { it.family == "Anton" }
        val pairings = FontPairingEngine.getPairings(anton, sampleCatalog)

        assertTrue("Anton must have pairing recommendations", pairings.isNotEmpty())
        val firstCompanion = pairings.first()
        assertEquals("Figtree", firstCompanion.companionFamily)
        assertEquals(99, firstCompanion.matchScore)
        assertNotNull("Companion font info should be resolved from catalog", firstCompanion.companionFont)
        assertTrue(firstCompanion.rationale.isNotEmpty())
    }

    @Test
    fun FP1_curatedPairingsForPlayfairDisplay() {
        val playfair = sampleCatalog.first { it.family == "Playfair Display" }
        val pairings = FontPairingEngine.getPairings(playfair, sampleCatalog)

        assertTrue(pairings.isNotEmpty())
        val interPairing = pairings.firstOrNull { it.companionFamily == "Inter" }
        assertNotNull("Playfair should pair with Inter", interPairing)
        assertEquals(99, interPairing!!.matchScore)
    }

    @Test
    fun FP2_fallbackArchetypeForUnknownFont() {
        val unknownDisplayFont = FontInfo(
            family = "CustomDisplayFamily",
            category = FontCategory.Display,
            styles = listOf(FontStyle(400, false)),
            popularity = 999,
            dateAdded = "2024-01-01",
            isNew = true
        )
        val pairings = FontPairingEngine.getPairings(unknownDisplayFont, sampleCatalog)
        assertTrue("Unknown font should get category archetype recommendations", pairings.isNotEmpty())
        assertTrue(pairings.all { it.matchScore >= 80 })
        assertTrue(pairings.none { it.companionFamily == "CustomDisplayFamily" })
    }

    @Test
    fun FP5_dualEmbedSnippetGeneration() {
        val anton = sampleCatalog.first { it.family == "Anton" }
        val figtree = sampleCatalog.first { it.family == "Figtree" }

        val snippet = FontPairingEngine.getDualEmbedSnippet(anton, figtree)
        assertTrue(snippet.contains("fonts.googleapis.com/css2"))
        assertTrue(snippet.contains("family=Anton"))
        assertTrue(snippet.contains("family=Figtree"))
        assertTrue(snippet.contains("--font-headline: \"Anton\""))
        assertTrue(snippet.contains("--font-body: \"Figtree\""))
    }
}
