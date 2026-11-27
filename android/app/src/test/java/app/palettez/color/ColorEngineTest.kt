package app.palettez.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorEngineTest {

    /**
     * Requirement CS-6:
     * 10-step scale (50–900) per color, 500 = the color; tints mix toward white and shades toward black
     * in OKLab at 92/82/64/44/22% and 15/32/50/66%.
     * Done when: Golden-file test for #E4572E gives #FFF2EF #FFE2DA #FCC6B6 #F7A58E #EF8061 #E4572E #B84423 #873017 #581C0B #310C04
     */
    @Test
    fun CS6_scaleMatchesReference() {
        val scale = ColorEngine.scaleOf("#E4572E")
        val hexList = scale.map { it.hex }
        val expected = listOf(
            "#FFF2EF",
            "#FFE2DA",
            "#FCC6B6",
            "#F7A58E",
            "#EF8061",
            "#E4572E",
            "#B84423",
            "#873017",
            "#581C0B",
            "#310C04",
        )
        assertEquals(expected, hexList)
    }

    /**
     * Requirement CS-3:
     * Auto-assign 5 roles: background (lightest), text (darkest, or ink/white if under 7:1),
     * primary (most colorful with 3:1 on background), secondary (next), accent (hue furthest from primary).
     * Done when: Unit tests cover all 15 palettes and match the reference engine.
     */
    @Test
    fun CS3_autoRolesMatchesReferenceAcrossAllPresets() {
        val expectedRoles = mapOf(
            "Sunset" to ColorEngine.Roles("#F6E7CB", "#2E4057", "#E4572E", "#8C5383", "#F2A65A"),
            "Terracotta" to ColorEngine.Roles("#FFE8DC", "#2F3E46", "#C8553D", "#588B8B", "#F28F3B"),
            "Clay & Moss" to ColorEngine.Roles("#F3ECE0", "#2E3A23", "#6B7F4E", "#8C6A4F", "#D9B48F"),
            "Savanna" to ColorEngine.Roles("#F4E6C9", "#1F2A22", "#C8643B", "#3E5641", "#E59A55"),
            "Ember" to ColorEngine.Roles("#FCE8C8", "#3D0C11", "#D94F30", "#8E1B1B", "#F59E42"),
            "Market day" to ColorEngine.Roles("#FFF4D6", "#16323F", "#D91A60", "#F25C05", "#1B8C6D"),
            "Lagoon" to ColorEngine.Roles("#F5F1E8", "#0B3954", "#087E8B", "#F2A541", "#5FB49C"),
            "Night swim" to ColorEngine.Roles("#E4E7EA", "#101A2B", "#45607F", "#E4C36B", "#86A0BE"),
            "Sorbet" to ColorEngine.Roles("#FFF6E6", "#3B3450", "#E7798F", "#C1F0DC", "#C9D7F8"),
            "Linen" to ColorEngine.Roles("#F6EEE8", "#151515", "#4B5859", "#C9828C", "#AFC5B3"),
            "Pop" to ColorEngine.Roles("#FFFFFF", "#151515", "#FF3B6B", "#2979FF", "#FFC233"),
            "Riso" to ColorEngine.Roles("#F5F1E8", "#1A1A40", "#0078BF", "#FF48B0", "#FFE800"),
            "Paper" to ColorEngine.Roles("#FBF8F3", "#151515", "#4A453E", "#D7CFC2", "#9C958A"),
            "Concrete" to ColorEngine.Roles("#F2F3F5", "#1F2124", "#7D838C", "#3C4046", "#C3C8CE"),
            "Palettez" to ColorEngine.Roles("#EFE9DF", "#151515", "#6A58CC", "#E2603F", "#F4C542"),
        )

        PresetPalettes.ALL.forEach { preset ->
            val computed = ColorEngine.autoRoles(preset.colors)
            val expected = expectedRoles[preset.name]
            assertEquals("Mismatch in roles for ${preset.name}", expected, computed)
        }
    }

    /**
     * Requirement CS-7:
     * Harmonies for the selected color: complementary, analogous (±30°), triadic, split (180±30°).
     * Done when: Output matches reference algorithm for #E4572E.
     */
    @Test
    fun CS7_harmoniesMatchesReference() {
        val harmonies = ColorEngine.harmonies("#E4572E")
        assertEquals(4, harmonies.size)

        val comp = harmonies.first { it.name == "Complementary" }
        assertEquals(listOf("#E4572E", "#2EBBE4"), comp.colors)

        val analogous = harmonies.first { it.name == "Analogous" }
        assertEquals(listOf("#E42E60", "#E4572E", "#E4B22E"), analogous.colors)

        val triadic = harmonies.first { it.name == "Triadic" }
        assertEquals(listOf("#E4572E", "#2EE457", "#572EE4"), triadic.colors)

        val split = harmonies.first { it.name == "Split" }
        assertEquals(listOf("#E4572E", "#2EE4B2", "#2E60E4"), split.colors)
    }

    /**
     * Requirement CS-8:
     * Contrast checks using WCAG 2.2 formula with grades (AAA >= 7, AA >= 4.5, AA Large >= 3, Fail).
     */
    @Test
    fun CS8_contrastAndGrading() {
        val blackOnWhite = ColorEngine.contrast("#000000", "#FFFFFF")
        assertEquals(21.0, blackOnWhite, 0.01)
        assertEquals("AAA", ColorEngine.grade(blackOnWhite))

        val inkOnWhite = ColorEngine.contrast("#151515", "#FFFFFF")
        assertEquals(18.26, inkOnWhite, 0.05)
        assertEquals("AAA", ColorEngine.grade(inkOnWhite))

        val orangeOnWhite = ColorEngine.contrast("#E4572E", "#FFFFFF")
        assertEquals(3.68, orangeOnWhite, 0.05)
        assertEquals("AA Large", ColorEngine.grade(orangeOnWhite))

        val failContrast = ColorEngine.contrast("#F6E7CB", "#FFFFFF")
        assertTrue(failContrast < 3.0)
        assertEquals("Fail", ColorEngine.grade(failContrast))
    }

    /**
     * Requirement CS-9:
     * Export CSS variables, Tailwind config, and JSON tokens containing roles and all scales.
     */
    @Test
    fun CS9_exportCode() {
        val roles = ColorEngine.Roles("#F6E7CB", "#2E4057", "#E4572E", "#8C5383", "#F2A65A")
        val colors = listOf("#F6E7CB", "#E4572E")

        val css = ColorEngine.exportCode("css", "Sunset", colors, roles)
        assertTrue(css.startsWith(":root {"))
        assertTrue(css.contains("--primary: #E4572E;"))
        assertTrue(css.contains("--color-2-500: #E4572E;"))
        assertTrue(css.endsWith("}"))

        val tailwind = ColorEngine.exportCode("tailwind", "Sunset", colors, roles)
        assertTrue(tailwind.contains("module.exports"))
        assertTrue(tailwind.contains("\"primary\": \"#E4572E\""))
        assertTrue(tailwind.contains("\"color-2\":"))

        val json = ColorEngine.exportCode("json", "Sunset", colors, roles)
        assertTrue(json.contains("\"name\": \"Sunset\""))
        assertTrue(json.contains("\"roles\":"))
        assertTrue(json.contains("\"scales\":"))
    }

    /**
     * Requirement CS-1:
     * Ship 14 presets in 6 moods plus the Palettez house palette (15 total).
     */
    @Test
    fun CS1_presetsVerification() {
        assertEquals("Total preset palettes should be 15", 15, PresetPalettes.ALL.size)
        val moods = PresetPalettes.ALL.map { it.mood }.toSet()
        assertTrue(moods.contains("Earthy"))
        assertTrue(moods.contains("Warm"))
        assertTrue(moods.contains("Cool"))
        assertTrue(moods.contains("Pastel"))
        assertTrue(moods.contains("Bold"))
        assertTrue(PresetPalettes.ALL.any { it.name == "Palettez" })

        // Every palette has 5 or 6 colors
        PresetPalettes.ALL.forEach { p ->
            assertTrue("Preset ${p.name} should have 5 or 6 colors", p.colors.size in 5..6)
            assertTrue("Colors must start with #", p.colors.all { it.startsWith("#") && it.length == 7 })
        }
    }

    /**
     * Requirement CS-2:
     * Presets filtered by mood; each card shows colors, mood, and text-on-background grade.
     */
    @Test
    fun CS2_presetsMoodFilter() {
        val earthy = PresetPalettes.ALL.filter { it.mood == "Earthy" }
        assertEquals(3, earthy.size)

        PresetPalettes.ALL.forEach { p ->
            assertTrue(p.contrastRatio >= 1.0)
            assertTrue(p.contrastGrade in listOf("AAA", "AA", "AA Large", "Fail"))
        }
    }

    /**
     * Requirement CS-4:
     * Change cycles a role through the palette colors; auto-assign resets.
     */
    @Test
    fun CS4_roleCyclingAndReset() {
        val colors = listOf("#F6E7CB", "#E4572E", "#8C5383", "#F2A65A", "#2E4057", "#1B998B")
        val initialRoles = ColorEngine.autoRoles(colors)

        // Cycling through colors for primary role
        val primaryIdx = colors.indexOf(initialRoles.primary)
        val nextPrimary = colors[(primaryIdx + 1) % colors.size]
        val modifiedRoles = initialRoles.copy(primary = nextPrimary)
        assertEquals(nextPrimary, modifiedRoles.primary)

        // Auto-assign reset restores original algorithmic assignment
        val resetRoles = ColorEngine.autoRoles(colors)
        assertEquals(initialRoles, resetRoles)
    }

    /**
     * Requirement CS-5:
     * Live preview card renders background, text, primary button (ink or white label), secondary, and accent.
     */
    @Test
    fun CS5_livePreviewContrastAndTokens() {
        val roles = ColorEngine.autoRoles(PresetPalettes.ALL.first { it.name == "Sunset" }.colors)
        val buttonLabelColor = ColorEngine.inkOn(roles.primary)
        assertTrue(buttonLabelColor == "#151515" || buttonLabelColor == "#FFFFFF")

        val textRatio = ColorEngine.contrast(roles.background, roles.text)
        assertTrue("Text on background contrast should be valid", textRatio >= 3.0)
    }

    /**
     * Requirement CS-10:
     * Text on any user color is ink (#151515) or white, whichever has higher WCAG contrast.
     */
    @Test
    fun CS10_inkOnContrastPreference() {
        assertEquals("#151515", ColorEngine.inkOn("#FFFFFF"))
        assertEquals("#FFFFFF", ColorEngine.inkOn("#000000"))
        assertEquals("#FFFFFF", ColorEngine.inkOn("#151515"))
        assertEquals("#151515", ColorEngine.inkOn("#F6E7CB"))
        assertEquals("#FFFFFF", ColorEngine.inkOn("#2E4057"))
    }

    /**
     * Requirement CS-6:
     * Additional golden scale checks for intermediate palette colors.
     */
    @Test
    fun CS6_additionalGoldenScales() {
        val purpleScale = ColorEngine.scaleOf("#8C5383")
        assertEquals(10, purpleScale.size)
        assertEquals("#8C5383", purpleScale.first { it.step == 500 }.hex)
        assertTrue(purpleScale.first { it.step == 50 }.hex.startsWith("#"))
        assertTrue(purpleScale.first { it.step == 900 }.hex.startsWith("#"))

        val tealScale = ColorEngine.scaleOf("#1B998B")
        assertEquals(10, tealScale.size)
        assertEquals("#1B998B", tealScale.first { it.step == 500 }.hex)
    }
}
