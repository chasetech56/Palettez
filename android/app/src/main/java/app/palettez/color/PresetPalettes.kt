package app.palettez.color

data class PresetPalette(
    val name: String,
    val mood: String,
    val colors: List<String>,
    val isMine: Boolean = false,
) {
    val roles: ColorEngine.Roles by lazy { ColorEngine.autoRoles(colors) }
    val contrastRatio: Double by lazy { ColorEngine.contrast(roles.background, roles.text) }
    val contrastGrade: String by lazy { ColorEngine.grade(contrastRatio) }
}

object PresetPalettes {
    val MOODS = listOf("All", "Earthy", "Warm", "Cool", "Pastel", "Bold", "Neutral")

    val ALL: List<PresetPalette> = listOf(
        PresetPalette(
            name = "Sunset",
            mood = "Warm",
            isMine = true,
            colors = listOf("#F6E7CB", "#E4572E", "#8C5383", "#F2A65A", "#2E4057", "#1B998B"),
        ),
        PresetPalette(
            name = "Terracotta",
            mood = "Earthy",
            colors = listOf("#2F3E46", "#C8553D", "#F28F3B", "#588B8B", "#FFE8DC"),
        ),
        PresetPalette(
            name = "Clay & Moss",
            mood = "Earthy",
            colors = listOf("#2E3A23", "#8C6A4F", "#6B7F4E", "#D9B48F", "#F3ECE0"),
        ),
        PresetPalette(
            name = "Savanna",
            mood = "Earthy",
            colors = listOf("#1F2A22", "#C8643B", "#E59A55", "#3E5641", "#F4E6C9"),
        ),
        PresetPalette(
            name = "Ember",
            mood = "Warm",
            colors = listOf("#3D0C11", "#D94F30", "#8E1B1B", "#F59E42", "#FCE8C8"),
        ),
        PresetPalette(
            name = "Market day",
            mood = "Warm",
            colors = listOf("#16323F", "#D91A60", "#F25C05", "#1B8C6D", "#FFF4D6"),
        ),
        PresetPalette(
            name = "Lagoon",
            mood = "Cool",
            colors = listOf("#0B3954", "#087E8B", "#5FB49C", "#F2A541", "#F5F1E8"),
        ),
        PresetPalette(
            name = "Night swim",
            mood = "Cool",
            colors = listOf("#101A2B", "#45607F", "#86A0BE", "#E4C36B", "#E4E7EA"),
        ),
        PresetPalette(
            name = "Sorbet",
            mood = "Pastel",
            colors = listOf("#3B3450", "#E7798F", "#C1F0DC", "#C9D7F8", "#FFF6E6"),
        ),
        PresetPalette(
            name = "Linen",
            mood = "Pastel",
            colors = listOf("#4B5859", "#C9828C", "#AFC5B3", "#E9B4BC", "#F6EEE8"),
        ),
        PresetPalette(
            name = "Pop",
            mood = "Bold",
            colors = listOf("#151515", "#FF3B6B", "#2979FF", "#FFC233", "#FFFFFF"),
        ),
        PresetPalette(
            name = "Riso",
            mood = "Bold",
            colors = listOf("#1A1A40", "#FF48B0", "#0078BF", "#FFE800", "#F5F1E8"),
        ),
        PresetPalette(
            name = "Paper",
            mood = "Neutral",
            colors = listOf("#151515", "#4A453E", "#9C958A", "#D7CFC2", "#FBF8F3"),
        ),
        PresetPalette(
            name = "Concrete",
            mood = "Neutral",
            colors = listOf("#1F2124", "#3C4046", "#7D838C", "#C3C8CE", "#F2F3F5"),
        ),
        PresetPalette(
            name = "Palettez",
            mood = "Bold",
            colors = listOf("#151515", "#6A58CC", "#E2603F", "#F4C542", "#A7D86E", "#EFE9DF"),
        ),
    )
}
