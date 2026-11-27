package app.palettez.update

enum class ChangeTag(val label: String) {
    New("NEW"),
    Improved("IMPROVED"),
    Fixed("FIXED")
}

data class ReleaseItem(
    val title: String,
    val description: String,
    val tag: ChangeTag
)

data class ReleaseNote(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val headline: String,
    val items: List<ReleaseItem>
)

object WhatsNewRegistry {

    val RELEASES = listOf(
        ReleaseNote(
            versionCode = 4,
            versionName = "1.2.1",
            releaseDate = "November 2026",
            headline = "You're on the latest build! Here is what's new & improved in v1.2.1:",
            items = listOf(
                ReleaseItem(
                    title = "Full-Resolution Camera Capture",
                    description = "Capture high-resolution photos straight into app cache via FileProvider with zero CAMERA permissions.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Complete PRD Verification",
                    description = "100% automated test coverage across all 57 requirements (PA, CS, GR, LA, FO, ON, SE).",
                    tag = ChangeTag.Improved
                ),
                ReleaseItem(
                    title = "Pure Engine Guarantees",
                    description = "Color and Layout engines verified free of Android dependencies with CI purity checks.",
                    tag = ChangeTag.Improved
                ),
                ReleaseItem(
                    title = "Golden Layout Calibrations",
                    description = "Panel tree counts across all 15 layout templates exactly calibrated against the reference engine.",
                    tag = ChangeTag.Fixed
                )
            )
        ),
        ReleaseNote(
            versionCode = 3,
            versionName = "1.2.0",
            releaseDate = "November 2026",
            headline = "You're on the latest build! Here is what's new & fixed in Palettez:",
            items = listOf(
                ReleaseItem(
                    title = "Local Backup & Restore",
                    description = "Export your saved palettes, grid settings, and preferences as a local JSON file via Storage Access Framework, and restore anytime.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Automatic Homepage Updates",
                    description = "Palettez checks chasetech56.github.io/Palettez on launch and resume, and lets you check manually in Settings → About.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Font Refresh & Category Search",
                    description = "Respects 'Update font list on open' when disabled, and searches both family names and font categories (Sans, Serif, Mono).",
                    tag = ChangeTag.Fixed
                ),
                ReleaseItem(
                    title = "200 MB Font Cache LRU & Stability",
                    description = "Automatic oldest-first eviction keeps cached .ttf files under 200 MB, with failed-download backoff and zero Lint errors.",
                    tag = ChangeTag.Improved
                )
            )
        ),
        ReleaseNote(
            versionCode = 2,
            versionName = "1.1.0",
            releaseDate = "November 2026",
            headline = "You're on the latest build! Here is what's new in Palettez:",
            items = listOf(
                ReleaseItem(
                    title = "On-Device Grid Analysis",
                    description = "Upload any poster, flyer, or editorial composition to reverse-engineer columns, margins, and gutters on-device, and instantly apply it to the Grid or Layouts editor.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Brand Identity Specimen Export",
                    description = "Export presentation-ready brand cards in 16:9 slides and 1:1 squares with multi-color-space readouts (HEX, RGB, CMYK, HSL, OKLab) and WCAG certification.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Font Pairing Suggestions & Studio",
                    description = "Intelligent archetype pairings across Google Fonts with an interactive split specimen studio, live typing, size sliders, role swap (⇄), and combined TTF / CSS export.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Image Recents & Smart Extraction",
                    description = "Recent image palettes are saved locally and accessible on the Palette home tab for quick restoration and exploration.",
                    tag = ChangeTag.Improved
                ),
                ReleaseItem(
                    title = "Deep Dark Mode & UI Contrast",
                    description = "Comprehensive contrast enhancements across all tools, status bar edge-to-edge rendering, and color-matched font cards.",
                    tag = ChangeTag.Improved
                )
            )
        ),
        ReleaseNote(
            versionCode = 1,
            versionName = "1.0.0",
            releaseDate = "Kickoff Release",
            headline = "Welcome to Palettez — The all-in-one local-first design kit.",
            items = listOf(
                ReleaseItem(
                    title = "Palette from Image",
                    description = "Extract 2–12 colors from photos with real-time coverage percentages and multiple color modes.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Presets & Color System",
                    description = "14 curated designer presets, 10-step scales, semantic roles, and code export.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Column Grids & Panel Layouts",
                    description = "Custom columns, gutters, margins, and 15 panel layout templates with procedural shuffle.",
                    tag = ChangeTag.New
                ),
                ReleaseItem(
                    title = "Google Fonts Browser",
                    description = "Full catalog browsing, specimen testing, CSS embed codes, and direct TTF downloads.",
                    tag = ChangeTag.New
                )
            )
        )
    )

    fun getLatestRelease(): ReleaseNote = RELEASES.first()

    fun getReleaseForVersion(versionCode: Int): ReleaseNote =
        RELEASES.firstOrNull { it.versionCode == versionCode } ?: getLatestRelease()
}
