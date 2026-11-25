package app.palettez.screenshot

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palettez.color.PresetPalette
import app.palettez.palette.ColorSystemScreen
import app.palettez.ui.theme.PalettezTheme
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.RoborazziRule.Options
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Full-screen Roborazzi screenshot tests for Color System.
 * Covers light, dark, and 200% font scale (large-print) baselines.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenScreenshotTest {

    private val samplePalette = PresetPalette(
        name = "Market Day",
        mood = "Earthy",
        colors = listOf("#E4572E", "#F4C542", "#1B998B", "#6A58CC", "#2E4057"),
    )

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = Options(
            outputDirectoryPath = "src/test/snapshots/images",
        )
    )

    @Test
    fun colorSystemScreen_light() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = false) {
                ColorSystemScreen(
                    initialPalette = samplePalette,
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/colorSystemScreen_light.png"
        )
    }

    @Test
    fun colorSystemScreen_dark() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = true) {
                ColorSystemScreen(
                    initialPalette = samplePalette,
                    onBack = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/colorSystemScreen_dark.png"
        )
    }

    @Test
    fun colorSystemScreen_largePrint() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                PalettezTheme(darkTheme = false) {
                    ColorSystemScreen(
                        initialPalette = samplePalette,
                        onBack = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/colorSystemScreen_largePrint.png"
        )
    }
}
