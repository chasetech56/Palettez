package app.palettez.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.palettez.ui.theme.LocalPalettezColors
import app.palettez.ui.theme.PalettezTypography

/**
 * Top header present on every screen.
 *
 * Layout (from Main.png):
 *   [subtitle (Caption, optional)]
 *   [TITLE (Display/Anton 52sp)]          [actions row (52dp circle buttons)]
 *
 * Example: subtitle = "Palettez", title = "PALETTE", actions = { CameraButton + SettingsButton }
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalPalettezColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PalettezTypography.labelSmall.copy(color = colors.inkSecondary),
                )
            }
            Text(
                text = title,
                style = PalettezTypography.displayLarge.copy(color = colors.ink),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
