package app.palettez.fonts

import java.net.URLEncoder

/**
 * A pairing recommendation between the current font and a companion font.
 */
data class FontPairing(
    val companionFamily: String,
    val role: String,
    val matchScore: Int,
    val rationale: String,
    val companionFont: FontInfo? = null,
)

/**
 * Pure Kotlin archetype-based font pairing engine (FP-1 .. FP-6).
 * Recommends balanced, harmonious companion families from Google Fonts
 * using contrast metrics, category rules, and curated design pairings.
 */
object FontPairingEngine {

    private data class CuratedPairing(
        val companion: String,
        val role: String,
        val score: Int,
        val rationale: String
    )

    // Curated pairings for widespread Google Fonts families
    private val curatedMap = mapOf(
        "Inter" to listOf(
            CuratedPairing("Playfair Display", "Editorial Contrast", 98, "Dramatic thick-thin stroke contrast elevates headlines against neutral body clarity"),
            CuratedPairing("Merriweather", "Warm Editorial", 95, "Sturdy bracketed serifs offer a welcoming, traditional reading tone"),
            CuratedPairing("Fraunces", "Expressive Heritage", 93, "Old-style soft terminals add artisanal warmth to structured digital UI"),
            CuratedPairing("Space Grotesk", "Technical Accent", 90, "Monospaced proportions provide a cybernetic, futuristic contrast")
        ),
        "Playfair Display" to listOf(
            CuratedPairing("Inter", "Modern Grotesque", 99, "Clean objective clarity provides effortless legibility for dense text"),
            CuratedPairing("Figtree", "Contemporary Sans", 95, "Friendly circular geometry balances dramatic high-contrast serif flourishes"),
            CuratedPairing("Lato", "Humanist Sans", 92, "Semi-rounded details echo classical proportions without competing"),
            CuratedPairing("Source Sans 3", "Editorial Workhorse", 91, "Robust reading rhythm designed specifically for user interfaces")
        ),
        "Montserrat" to listOf(
            CuratedPairing("Merriweather", "Classic Editorial", 97, "Traditional serif structure tempers modernist geometric minimalism"),
            CuratedPairing("Lora", "Literary Serif", 95, "Calligraphic curves soften hard angles and wide uppercase geometry"),
            CuratedPairing("Roboto Slab", "Industrial Slab", 91, "Architectural slab serifs reinforce sturdy geometric branding"),
            CuratedPairing("Cormorant Garamond", "Fine Luxury", 90, "Delicate historic serif elegance juxtaposes bold geometric capitals")
        ),
        "Roboto" to listOf(
            CuratedPairing("Roboto Slab", "Same-DNA Slab", 98, "Identical skeletal proportions ensure seamless cross-weight harmony"),
            CuratedPairing("Playfair Display", "High Contrast Accent", 94, "Expressive headline drama counterweights neutral body legibility"),
            CuratedPairing("Lora", "Graceful Serif", 92, "Bookish literary warmth enriches clean mechanical neo-grotesque text")
        ),
        "Anton" to listOf(
            CuratedPairing("Figtree", "Clean Body Sans", 99, "Friendly open apertures and crisp geometry perfectly balance heavy condensed headline punch"),
            CuratedPairing("Inter", "Neutral Workhorse", 96, "Hyper-neutral proportions let heavy wood-type display shouting take center stage"),
            CuratedPairing("JetBrains Mono", "Technical Code", 92, "Monospaced monospace counters the intense vertical rhythm of poster headlines"),
            CuratedPairing("Merriweather", "Sturdy Serif", 89, "Warm reading serifs anchor dynamic display impact")
        ),
        "Figtree" to listOf(
            CuratedPairing("Anton", "Bold Poster Headline", 98, "Palettez signature duo: heavyweight display impact paired with friendly geometric body text"),
            CuratedPairing("Fraunces", "Chiseled Editorial", 95, "Organic display serifs bring tactile warmth to modern digital layouts"),
            CuratedPairing("Playfair Display", "Classic High Contrast", 93, "Fashion and lifestyle sophistication paired with crisp digital navigation"),
            CuratedPairing("JetBrains Mono", "Specimen & Code", 91, "Developer aesthetic balancing clean contemporary branding")
        ),
        "JetBrains Mono" to listOf(
            CuratedPairing("Inter", "Clean Body Sans", 97, "Balances technical character grids with fluid organic paragraph flow"),
            CuratedPairing("Anton", "Impact Display", 94, "High-energy poster titles anchored by precise engineering monospacing"),
            CuratedPairing("Playfair Display", "High-Contrast Luxury", 91, "Bold juxtaposition between high-end fashion and utilitarian engineering"),
            CuratedPairing("Figtree", "Warm UI Sans", 90, "Pairs code and data readouts with welcoming modern product UI")
        ),
        "Lora" to listOf(
            CuratedPairing("Montserrat", "Geometric Sans", 97, "Sharp geometry and wide proportions create architectural contrast"),
            CuratedPairing("Inter", "Neutral Grotesque", 94, "Unobtrusive modernist sans lets graceful calligraphic details shine"),
            CuratedPairing("Nunito", "Soft Friendly Sans", 91, "Rounded sans curves harmonize with delicate calligraphic transitions")
        ),
        "Merriweather" to listOf(
            CuratedPairing("Open Sans", "Humanist Workhorse", 98, "Open forms and friendly tall x-height pair seamlessly for editorial reading"),
            CuratedPairing("Montserrat", "Geometric Sans", 95, "Sharp modern uppercase headers elevate robust bracketed book text"),
            CuratedPairing("Inter", "Modern Grotesque", 92, "Neutral functional precision keeps layout focus on literary content")
        ),
        "Poppins" to listOf(
            CuratedPairing("Playfair Display", "Classic Drama", 96, "High-contrast serif flourishes elevate pure circular geometric body typography"),
            CuratedPairing("Source Serif 4", "Editorial Classic", 94, "Balanced academic reading serifs complement geometric heading clarity"),
            CuratedPairing("Bitter", "Contemporary Slab", 90, "Crisp slab serifs designed for screen reading complement geometric rhythms")
        ),
        "Oswald" to listOf(
            CuratedPairing("Open Sans", "Humanist Sans", 96, "Open, welcoming proportions relieve condensed display tension"),
            CuratedPairing("EB Garamond", "Renaissance Serif", 92, "Historic humanist serifs contrast sharply with modern industrial shouting"),
            CuratedPairing("Lato", "Clean Geometric", 89, "Warm classical proportions balance tall compressed headline bars")
        ),
        "Fraunces" to listOf(
            CuratedPairing("Inter", "Modern Grotesque", 98, "Neutral modern clarity balances tactile, expressive display curves"),
            CuratedPairing("Figtree", "Clean Contemporary", 94, "Soft circular geometry matches warm artisanal serif terminals"),
            CuratedPairing("Space Grotesk", "Technical Sans", 91, "Unexpected pairing of old-style warmth and quirky tech neo-grotesque")
        ),
        "Space Grotesk" to listOf(
            CuratedPairing("Space Mono", "Sister Monospace", 99, "Designed in tandem; consistent idiosyncratic ink traps and DNA"),
            CuratedPairing("Inter", "Neutral Sans", 93, "Calm standard proportions ground quirky tech display letterforms"),
            CuratedPairing("Fraunces", "Expressive Serif", 90, "Striking clash of digital brutalism and organic tactile bookmaking")
        ),
        "Bebas Neue" to listOf(
            CuratedPairing("Montserrat", "Geometric Sans", 96, "Wide geometric letterforms balance narrow condensed uppercase power"),
            CuratedPairing("Roboto", "Clean Workhorse", 93, "Industrial sans heritage shared across display and body hierarchy"),
            CuratedPairing("Lora", "Literary Serif", 90, "Soft curves and fine serifs counter intense condensed woodblock headlines")
        ),
        "DM Sans" to listOf(
            CuratedPairing("DM Serif Display", "Matching Display Serif", 99, "Sisters designed by Colophon Foundry with shared geometric proportions"),
            CuratedPairing("Playfair Display", "Romantic High Contrast", 94, "Dramatic headline weight paired with clean low-contrast UI text"),
            CuratedPairing("Space Mono", "Technical Accent", 91, "Quirky code details accent minimal geometric sans structures")
        ),
        "DM Serif Display" to listOf(
            CuratedPairing("DM Sans", "Sister Geometric Sans", 99, "Sister companion crafted to provide effortless harmonious hierarchy"),
            CuratedPairing("Inter", "Clean Body Sans", 95, "Pure utilitarian neutral reading allows expressive serifs to dominate"),
            CuratedPairing("Figtree", "Approachable UI Sans", 92, "Friendly circular geometry softens bold editorial seriousness")
        ),
        "Cormorant Garamond" to listOf(
            CuratedPairing("Montserrat", "Geometric Sans", 96, "Geometric rigidity frames exquisite traditional Renaissance serifs"),
            CuratedPairing("Figtree", "Warm Modern Sans", 94, "Brings historical elegance into contemporary digital product design"),
            CuratedPairing("Proza Libre", "Humanist Sans", 91, "Diagonal axes and calligraphic origins share genuine historical roots")
        )
    )

    /**
     * Resolves recommendations for a given font.
     */
    fun getPairings(target: FontInfo, catalog: List<FontInfo>): List<FontPairing> {
        val targetFamily = target.family.trim()
        val curated = curatedMap.entries.firstOrNull { it.key.equals(targetFamily, ignoreCase = true) }?.value

        val recommendations = mutableListOf<CuratedPairing>()
        if (curated != null) {
            recommendations.addAll(curated)
        }

        // If not enough curated entries, generate archetype recommendations based on category
        if (recommendations.size < 4) {
            val genericRecommendations = when (target.category) {
                FontCategory.Display -> listOf(
                    CuratedPairing("Figtree", "Clean Body Sans", 96, "Friendly open apertures and clean geometry balance bold display forms"),
                    CuratedPairing("Inter", "Neutral Workhorse", 94, "Neutral proportions allow expressive display headlines to shine without clash"),
                    CuratedPairing("Open Sans", "Humanist Workhorse", 91, "High x-height and open shapes ensure maximum readability for body copy"),
                    CuratedPairing("Merriweather", "Warm Editorial Serif", 88, "Rich bracketed serifs ground energetic headlines with classic dignity")
                )
                FontCategory.Serif -> listOf(
                    CuratedPairing("Montserrat", "Geometric Sans", 97, "Sharp geometry and wide capitals contrast traditional serif stroke variation"),
                    CuratedPairing("Inter", "Modern Grotesque", 94, "Clean unadorned shapes let classic serif details take center stage"),
                    CuratedPairing("Figtree", "Contemporary UI Sans", 92, "Approachability and clarity keep long reads fresh and modern"),
                    CuratedPairing("JetBrains Mono", "Technical Monospace", 89, "Calculated precision juxtaposes elegant literary letterforms")
                )
                FontCategory.SansSerif -> listOf(
                    CuratedPairing("Playfair Display", "High-Contrast Serif", 96, "Dramatic stroke modulation creates instant editorial impact"),
                    CuratedPairing("Lora", "Literary Calligraphic", 93, "Flowing organic rhythm warms up clean corporate sans-serif layouts"),
                    CuratedPairing("Fraunces", "Expressive Heritage", 91, "Handcrafted soft terminals introduce personality and warmth"),
                    CuratedPairing("Roboto Slab", "Structured Slab Serif", 88, "Geometric slab serifs provide bold architectural authority")
                )
                FontCategory.Monospace -> listOf(
                    CuratedPairing("Inter", "Humanist Grotesque", 96, "Relieves the rigid tabular character grid with fluid natural text"),
                    CuratedPairing("Playfair Display", "Luxury Editorial", 92, "Bold juxtaposition between high fashion and utilitarian engineering"),
                    CuratedPairing("Figtree", "Warm Modern Sans", 90, "Brings friendly consumer appeal to data-dense interfaces")
                )
                FontCategory.Handwriting -> listOf(
                    CuratedPairing("Inter", "Solid Grotesque", 95, "Grounds freeform gestural script with dependable typographic structure"),
                    CuratedPairing("Montserrat", "Geometric Caps", 91, "Provides strong architectural boundaries around expressive handwriting")
                )
                FontCategory.All -> emptyList()
            }

            for (rec in genericRecommendations) {
                if (!rec.companion.equals(targetFamily, ignoreCase = true) &&
                    recommendations.none { it.companion.equals(rec.companion, ignoreCase = true) }
                ) {
                    recommendations.add(rec)
                }
            }
        }

        // Map companion family name to actual FontInfo in catalog
        val catalogMap = catalog.associateBy { it.family.lowercase() }
        return recommendations.take(5).map { rec ->
            val companionFont = catalogMap[rec.companion.lowercase()]
            FontPairing(
                companionFamily = rec.companion,
                role = rec.role,
                matchScore = rec.score,
                rationale = rec.rationale,
                companionFont = companionFont
            )
        }
    }

    /**
     * Generates a combined Google Fonts embed snippet (<link> and CSS) for both fonts (FP-5).
     */
    fun getDualEmbedSnippet(fontA: FontInfo, fontB: FontInfo): String {
        val encA = URLEncoder.encode(fontA.family, "UTF-8")
        val encB = URLEncoder.encode(fontB.family, "UTF-8")
        val specA = fontA.embedSpec
        val specB = fontB.embedSpec

        return buildString {
            append("<!-- Google Fonts Pairing: ${fontA.family} + ${fontB.family} -->\n")
            append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n")
            append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n")
            append("<link href=\"https://fonts.googleapis.com/css2?family=$encA$specA&family=$encB$specB&display=swap\" rel=\"stylesheet\">\n\n")
            append("/* CSS Variables */\n")
            append(":root {\n")
            append("  --font-headline: \"${fontA.family}\", ${fontA.genericFallback};\n")
            append("  --font-body: \"${fontB.family}\", ${fontB.genericFallback};\n")
            append("}\n\n")
            append("h1, h2, h3 {\n")
            append("  font-family: var(--font-headline);\n")
            append("}\n\n")
            append("p, body, input {\n")
            append("  font-family: var(--font-body);\n")
            append("}")
        }
    }
}
