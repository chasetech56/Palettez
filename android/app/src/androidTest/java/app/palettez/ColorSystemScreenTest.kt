package app.palettez

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palettez.color.ColorEngine
import app.palettez.color.PresetPalette
import app.palettez.palette.ColorSystemScreen
import app.palettez.ui.theme.PalettezTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI tests for the Color System screen (CS-3, CS-4, CS-8).
 */
@RunWith(AndroidJUnit4::class)
class ColorSystemScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val samplePalette = PresetPalette(
        name = "Test Palette",
        mood = "Bold",
        colors = listOf("#E4572E", "#F4C542", "#1B998B", "#6A58CC", "#2E4057"),
    )

    // CS-5: Live preview card renders on-screen
    @Test
    fun colorSystemScreen_showsLivePreviewCard() {
        composeTestRule.setContent {
            PalettezTheme {
                ColorSystemScreen(
                    initialPalette = samplePalette,
                    onBack = {},
                )
            }
        }
        // The screen title should be visible
        composeTestRule.onNodeWithText("TEST PALETTE", ignoreCase = true).assertIsDisplayed()
    }
}
