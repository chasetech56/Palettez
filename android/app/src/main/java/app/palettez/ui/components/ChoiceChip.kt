package app.palettez.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.ui.theme.LocalPalettezColors
import app.palettez.ui.theme.PalettezTypography

/**
 * Filter pill chip — custom Bold Edition component.
 *
 * Selected   → ink background (#151515), white label (Figtree SemiBold/Bold), no border
 * Unselected → transparent / surface background, line border (#D7CFC2, 1.5dp), ink label
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPalettezColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.ink else Color.Transparent)
            .border(
                width = if (selected) 0.dp else 1.5.dp,
                color = colors.line,
                shape = RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = PalettezTypography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) colors.onInk else colors.ink,
                fontSize = 14.sp
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}
