package app.palettez.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.ui.theme.LocalPalettezColors
import app.palettez.ui.theme.PalettezTypography

enum class PalettezTab { Palette, Grid, Fonts }

private val tabItems = listOf(
    Triple(PalettezTab.Palette, Icons.Filled.Palette, "Palette"),
    Triple(PalettezTab.Grid, Icons.Filled.GridOn, "Grid"),
    // Fonts uses a text "A" letterform, handled specially
    Triple(PalettezTab.Fonts, null, "Fonts"),
)

@Composable
fun FloatingTabBar(
    selectedTab: PalettezTab,
    onTabSelect: (PalettezTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPalettezColors.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier.padding(bottom = navBarPadding + 22.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF151515))
                .then(
                    if (colors.isDark) {
                        Modifier.border(1.dp, Color(0xFF343C37), RoundedCornerShape(50))
                    } else Modifier
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            tabItems.forEach { (tab, icon, label) ->
                val isSelected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }

                if (isSelected) {
                    // Active: cream pill with dark icon + label
                    Row(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFBF8F3))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) { onTabSelect(tab) }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TabIcon(tab = tab, icon = icon, tint = Color(0xFF151515))
                        Text(
                            text = label,
                            style = PalettezTypography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF151515),
                            ),
                        )
                    }
                } else {
                    // Inactive: 52dp circle with cream icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) { onTabSelect(tab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        TabIcon(tab = tab, icon = icon, tint = Color(0xFFFBF8F3).copy(alpha = 0.7f))
                    }
                }

                if (tab != PalettezTab.Fonts) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}

@Composable
private fun TabIcon(
    tab: PalettezTab,
    icon: ImageVector?,
    tint: Color,
) {
    if (tab == PalettezTab.Fonts) {
        // "A" letterform for Fonts tab
        Text(
            text = "A",
            style = PalettezTypography.titleLarge.copy(
                color = tint,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
            ),
        )
    } else if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
