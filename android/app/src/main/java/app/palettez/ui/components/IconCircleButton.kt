package app.palettez.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.palettez.ui.theme.LocalPalettezColors

/**
 * 52dp circular icon button with surface fill.
 *
 * Used throughout the app: settings and camera icons in Main.png header,
 * back arrow in Settings.png, code icon in Grid.png.
 */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalPalettezColors.current.ink,
) {
    val colors = LocalPalettezColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .then(
                if (colors.isDark) {
                    Modifier.border(1.dp, colors.line, CircleShape)
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
