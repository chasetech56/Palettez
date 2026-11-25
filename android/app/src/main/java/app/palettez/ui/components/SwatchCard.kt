package app.palettez.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import app.palettez.palette.Swatch
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.contrastColor
import kotlin.math.roundToInt

/**
 * Swatch card from Main.png:
 * - Full card in the swatch color
 * - Large Anton HEX title
 * - Coverage percentage pill on the top right
 * - Rounded value pills for RGB, CMYK, HSL (tap to copy)
 * - Text color is dynamically ink (#151515) or white based on WCAG contrast
 */
@Composable
fun SwatchCard(
    swatch: Swatch,
    onCopy: (value: String, label: String) -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
) {
    val swatchColor = Color(swatch.argb)
    val contentColor = contrastColor(swatchColor)
    // Value pill background: translucent overlay matching content color
    val pillBackground = if (contentColor == Color.White) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    val typography = PalettezTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(swatchColor)
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else Modifier
            )
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row: Anton HEX and Coverage pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = swatch.hex,
                    style = typography.headline.copy(
                        fontSize = 38.sp,
                        color = contentColor,
                    ),
                )

                // Coverage pill
                val percentText = if (swatch.share < 0.01f) "<1%" else "${(swatch.share * 100).roundToInt()}%"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillBackground)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = percentText,
                        style = typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Value pills: RGB, CMYK, HSL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // RGB pill
                SwatchValuePill(
                    label = "RGB",
                    value = "${swatch.r}, ${swatch.g}, ${swatch.b}",
                    contentColor = contentColor,
                    backgroundColor = pillBackground,
                    onClick = { onCopy(swatch.rgb, "RGB") },
                )

                // CMYK pill
                val (c, m, y, k) = swatch.cmykValues
                SwatchValuePill(
                    label = "CMYK",
                    value = "$c · $m · $y · $k",
                    contentColor = contentColor,
                    backgroundColor = pillBackground,
                    onClick = { onCopy(swatch.cmyk, "CMYK") },
                )

                // HSL pill
                val (h, s, l) = swatch.hslValues
                SwatchValuePill(
                    label = "HSL",
                    value = "$h° $s% $l%",
                    contentColor = contentColor,
                    backgroundColor = pillBackground,
                    onClick = { onCopy(swatch.hsl, "HSL") },
                )
            }
        }
    }
}

@Composable
private fun SwatchValuePill(
    label: String,
    value: String,
    contentColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = PalettezTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = typography.caption.copy(
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.8f),
            ),
        )
        Text(
            text = value,
            style = typography.value.copy(
                color = contentColor,
            ),
        )
    }
}
