package app.palettez.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.palettez.ui.theme.LocalPalettezColors
import app.palettez.ui.theme.PalettezTypography

/**
 * Primary CTA button.
 *
 * From Onboarding1.png: "GET STARTED →" — lime (#A7D86E) background, ink text, Anton uppercase
 * From Onboarding2.png: "NEXT →" — same style
 *
 * trailingArrow = true appends " →" to the label text.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingArrow: Boolean = false,
) {
    val colors = LocalPalettezColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.lime)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (trailingArrow) "$label →" else label,
            style = PalettezTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = colors.ink,
            ),
        )
    }
}

/**
 * Secondary button.
 *
 * From Onboarding1.png (secondary actions): ink background, white text, Anton uppercase, pill shape.
 * Also used as the primary CTA in dark contexts (Onboarding1 "GET STARTED" before lime existed).
 * Per the PRD: ink background, white/onInk text.
 */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingArrow: Boolean = false,
) {
    val colors = LocalPalettezColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.ink)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (trailingArrow) "$label →" else label,
            style = PalettezTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = colors.onInk,
            ),
        )
    }
}
