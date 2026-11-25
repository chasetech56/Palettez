package app.palettez.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palettez.palette.Swatch
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.components.StepperTile
import app.palettez.ui.components.SwatchCard
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
 * Roborazzi screenshot tests for core components.
 * PRD test plan: "Screenshot tests: Roborazzi on every screen in light, dark and 200% font scale."
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class ComponentScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = Options(
            outputDirectoryPath = "src/test/snapshots/images",
        )
    )

    @Test
    fun primaryButton_light() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = false) {
                PrimaryButton(label = "EXPLORE NEW FEATURES", onClick = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/primaryButton_light.png"
        )
    }

    @Test
    fun primaryButton_dark() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = true) {
                PrimaryButton(label = "EXPLORE NEW FEATURES", onClick = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/primaryButton_dark.png"
        )
    }

    @Test
    fun swatchCard_coral_light() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = false) {
                SwatchCard(
                    swatch = Swatch(228, 87, 46, 0.28f),
                    onCopy = { _, _ -> },
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/swatchCard_coral_light.png"
        )
    }

    @Test
    fun stepperTile_light() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = false) {
                StepperTile(
                    label = "Columns",
                    value = 6,
                    onDecrement = {},
                    onIncrement = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/stepperTile_light.png"
        )
    }

    @Test
    fun stepperTile_dark() {
        composeTestRule.setContent {
            PalettezTheme(darkTheme = true) {
                StepperTile(
                    label = "Columns",
                    value = 6,
                    onDecrement = {},
                    onIncrement = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/stepperTile_dark.png"
        )
    }
}
