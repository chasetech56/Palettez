package app.palettez

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palettez.ui.components.StepperTile
import app.palettez.ui.theme.PalettezTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GR-2: Validates stepper min/max clamping behavior in the StepperTile component.
 */
@RunWith(AndroidJUnit4::class)
class StepperTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stepperTile_minusButton_clampsAtMinimum() {
        var value = 1
        composeTestRule.setContent {
            PalettezTheme {
                var v by remember { mutableIntStateOf(1) }
                StepperTile(
                    label = "Columns",
                    value = v,
                    onDecrement = { if (v > 0) { v--; value = v } },
                    onIncrement = { if (v < 24) { v++; value = v } },
                    decrementEnabled = v > 0,
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Decrement Columns").performClick()
        assertEquals(0, value)
        composeTestRule.onNodeWithContentDescription("Decrement Columns").assertIsNotEnabled()
    }

    @Test
    fun stepperTile_plusButton_clampsAtMaximum() {
        var value = 23
        composeTestRule.setContent {
            PalettezTheme {
                var v by remember { mutableIntStateOf(23) }
                StepperTile(
                    label = "Columns",
                    value = v,
                    onDecrement = { if (v > 0) { v--; value = v } },
                    onIncrement = { if (v < 24) { v++; value = v } },
                    enabled = v < 24,
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Increment Columns").performClick()
        assertEquals(24, value)
        composeTestRule.onNodeWithContentDescription("Increment Columns").assertIsNotEnabled()
    }
}
