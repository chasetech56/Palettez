package app.palettez.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.palettez.R

// ---------------------------------------------------------------------------
// Color tokens
// ---------------------------------------------------------------------------

@Immutable
data class PalettezColors(
    val ground: Color,       // screen background
    val surface: Color,      // cards, chips, inputs
    val ink: Color,          // primary text, tab bar, primary buttons
    val inkMuted: Color,     // secondary text
    val line: Color,         // outlines, dividers
    val grape: Color,
    val mustard: Color,
    val coral: Color,        // also error
    val sage: Color,
    val sky: Color,
    val lime: Color,         // main CTA, success
    val deep: Color,         // detail pages, code blocks
    val isDark: Boolean,
) {
    val onInk: Color get() = if (isDark) Color(0xFF151515) else Color.White
    val inkSecondary: Color get() = inkMuted
    val inkDisabled: Color get() = line
}

val LightColors = PalettezColors(
    ground    = Color(0xFFEFE9DF),
    surface   = Color(0xFFFBF8F3),
    ink       = Color(0xFF151515),
    inkMuted  = Color(0xFF5E584F),
    line      = Color(0xFFD7CFC2),
    grape     = Color(0xFF6A58CC),
    mustard   = Color(0xFFF4C542),
    coral     = Color(0xFFE2603F),
    sage      = Color(0xFF4E7A5B),
    sky       = Color(0xFFAFCBEA),
    lime      = Color(0xFFA7D86E),
    deep      = Color(0xFF1D2320),
    isDark    = false,
)

val DarkColors = PalettezColors(
    ground    = Color(0xFF1D2320),
    surface   = Color(0xFF2A312D),
    ink       = Color(0xFFFFFFFF),
    inkMuted  = Color(0xFFC9CEC9),
    line      = Color(0xFF343C37),
    grape     = Color(0xFF6A58CC),
    mustard   = Color(0xFFF4C542),
    coral     = Color(0xFFE2603F),
    sage      = Color(0xFF4E7A5B),
    sky       = Color(0xFFAFCBEA),
    lime      = Color(0xFFA7D86E),
    deep      = Color(0xFF11150F),
    isDark    = true,
)

val LocalPalettezColors = staticCompositionLocalOf { LightColors }

// Convenience accessor — use `PalettezTheme.colors` inside any composable
object PalettezTheme {
    val colors: PalettezColors
        @Composable get() = LocalPalettezColors.current
    val typography: PalettezTypographyData
        @Composable get() = LocalPalettezTypography.current
}

// ---------------------------------------------------------------------------
// Typography tokens
// ---------------------------------------------------------------------------

private val AntonFamily: FontFamily = FontFamily(
    Font(R.font.anton_regular, FontWeight.Normal)
)

private val FigtreeFamily: FontFamily = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

private val JetBrainsMonoFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_medium, FontWeight.Normal),
)

@Immutable
data class PalettezTypographyData(
    val display: TextStyle,
    val headline: TextStyle,
    val section: TextStyle,
    val buttonLabel: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val value: TextStyle,
) {
    val displayLarge: TextStyle get() = display
    val displayMedium: TextStyle get() = headline
    val titleLarge: TextStyle get() = section
    val labelLarge: TextStyle get() = label
    val labelMedium: TextStyle get() = label
    val labelSmall: TextStyle get() = caption
    val bodyMedium: TextStyle get() = body
}

object PalettezTypography {
    val display: TextStyle @Composable get() = LocalPalettezTypography.current.display
    val headline: TextStyle @Composable get() = LocalPalettezTypography.current.headline
    val section: TextStyle @Composable get() = LocalPalettezTypography.current.section
    val buttonLabel: TextStyle @Composable get() = LocalPalettezTypography.current.buttonLabel
    val body: TextStyle @Composable get() = LocalPalettezTypography.current.body
    val label: TextStyle @Composable get() = LocalPalettezTypography.current.label
    val caption: TextStyle @Composable get() = LocalPalettezTypography.current.caption
    val value: TextStyle @Composable get() = LocalPalettezTypography.current.value

    val displayLarge: TextStyle @Composable get() = LocalPalettezTypography.current.display
    val displayMedium: TextStyle @Composable get() = LocalPalettezTypography.current.headline
    val titleLarge: TextStyle @Composable get() = LocalPalettezTypography.current.section
    val labelLarge: TextStyle @Composable get() = LocalPalettezTypography.current.label
    val labelMedium: TextStyle @Composable get() = LocalPalettezTypography.current.label
    val labelSmall: TextStyle @Composable get() = LocalPalettezTypography.current.caption
    val bodyMedium: TextStyle @Composable get() = LocalPalettezTypography.current.body
}

private val defaultTypography = PalettezTypographyData(
    display = TextStyle(
        fontFamily = AntonFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
        lineHeight = 56.sp,
    ),
    headline = TextStyle(
        fontFamily = AntonFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    section = TextStyle(
        fontFamily = AntonFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    buttonLabel = TextStyle(
        fontFamily = AntonFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.6.sp,
    ),
    body = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    label = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    value = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

val LocalPalettezTypography = staticCompositionLocalOf { defaultTypography }

// ---------------------------------------------------------------------------
// Theme composable
// ---------------------------------------------------------------------------

@Composable
fun PalettezTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // Minimal M3 color scheme so infrastructure Composables (Surface, etc.)
    // inherit our tokens rather than M3 defaults.
    val m3Scheme = if (darkTheme) {
        darkColorScheme(
            primary           = colors.lime,
            onPrimary         = Color(0xFF151515),
            secondary         = colors.grape,
            onSecondary       = Color(0xFFFFFFFF),
            error             = colors.coral,
            onError           = Color(0xFFFFFFFF),
            background        = colors.ground,
            onBackground      = colors.ink,
            surface           = colors.surface,
            onSurface         = colors.ink,
            outline           = colors.line,
        )
    } else {
        lightColorScheme(
            primary           = colors.lime,
            onPrimary         = Color(0xFF151515),
            secondary         = colors.grape,
            onSecondary       = Color(0xFFFFFFFF),
            error             = colors.coral,
            onError           = Color(0xFFFFFFFF),
            background        = colors.ground,
            onBackground      = colors.ink,
            surface           = colors.surface,
            onSurface         = colors.ink,
            outline           = colors.line,
        )
    }

    CompositionLocalProvider(
        LocalPalettezColors provides colors,
        LocalPalettezTypography provides defaultTypography,
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            content = content,
        )
    }
}
