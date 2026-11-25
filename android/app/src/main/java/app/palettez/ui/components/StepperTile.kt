package app.palettez.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.ui.theme.LocalPalettezColors
import app.palettez.ui.theme.PalettezTypography

/**
 * Stepper tile component.
 *
 * From Grid.png: "Columns 12 − +", "Rows 0 − +"
 * From Settings.png: "Colors to find 6 − +"
 *
 * Layout: surface card (24dp radius) | Label (Figtree body) | Large Anton value | − circle | + circle
 * The minus button is outlined/surface when value == minValue (not passed — caller decides via enabled),
 * or ink when above min. The plus button is always ink.
 *
 * Note: `minValue` is not part of the public API — callers control enabled state via onDecrement
 * being a no-op at the minimum. The visual distinction (outlined vs filled minus) is driven by
 * whether the value is at the lower bound; callers should pass a `decrementEnabled` bool.
 */
@Composable
fun StepperTile(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    valueText: String = value.toString(),
    enabled: Boolean = true,
    decrementEnabled: Boolean = true,
) {
    val colors = LocalPalettezColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = PalettezTypography.caption.copy(color = colors.inkSecondary, fontWeight = FontWeight.SemiBold),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = valueText,
                    style = PalettezTypography.displayMedium.copy(
                        fontSize = 28.sp,
                        color = colors.ink
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )

                // Minus button — outlined when at minimum, ink fill otherwise
                val isDecrementActive = decrementEnabled && enabled
                val decrementInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(
                            if (isDecrementActive) {
                                Modifier.background(colors.surface)
                                    .border(1.5.dp, colors.inkSecondary, CircleShape)
                            } else {
                                Modifier.background(colors.surface)
                                    .border(1.5.dp, colors.inkDisabled, CircleShape)
                            }
                        )
                        .clickable(
                            interactionSource = decrementInteraction,
                            indication = null,
                            enabled = isDecrementActive,
                            onClick = onDecrement,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrement $label",
                        tint = if (isDecrementActive) colors.ink else colors.inkDisabled,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Plus button — always ink filled
                val incrementInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (enabled) colors.ink else colors.inkDisabled)
                        .clickable(
                            interactionSource = incrementInteraction,
                            indication = null,
                            enabled = enabled,
                            onClick = onIncrement,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increment $label",
                        tint = colors.onInk,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Compact vertical stepper tile for 3-column rows (used in LayoutEditor).
 * Stacks: Label on top -> Value in middle -> Minus/Plus buttons side-by-side underneath.
 */
@Composable
fun CompactStepperTile(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    valueText: String = value.toString(),
    enabled: Boolean = true,
    decrementEnabled: Boolean = true,
) {
    val colors = LocalPalettezColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(top = 12.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = PalettezTypography.caption.copy(color = colors.inkSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp),
            )

            Text(
                text = valueText,
                style = PalettezTypography.displayMedium.copy(fontSize = 28.sp, color = colors.ink),
                maxLines = 1,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val isDecrementActive = decrementEnabled && enabled
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.5.dp, if (isDecrementActive) colors.inkSecondary else colors.inkDisabled, CircleShape)
                        .clickable(enabled = isDecrementActive, onClick = onDecrement),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease $label",
                        tint = if (isDecrementActive) colors.ink else colors.inkDisabled,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (enabled) colors.ink else colors.inkDisabled)
                        .clickable(enabled = enabled, onClick = onIncrement),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase $label",
                        tint = colors.onInk,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
