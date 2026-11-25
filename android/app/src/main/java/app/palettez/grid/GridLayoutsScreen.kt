package app.palettez.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.layout.LayoutEngine
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.components.ModeSwitch
import app.palettez.ui.components.ScreenHeader
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme

@Composable
fun GridLayoutsScreen(
    onModeSelect: (Int) -> Unit,
    onOpenTemplate: (LayoutEngine.LayoutTemplate) -> Unit,
    onOpenShuffle: () -> Unit,
    customPaletteHexes: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    var selectedFamily by remember { mutableStateOf("all") }
    var selectedStyle by remember { mutableStateOf("paper") } // paper, ink, palette

    val families = remember {
        listOf("all" to "All") + LayoutEngine.FAMILIES.map { it.key to it.name }
    }

    val familyRule = remember(selectedFamily) {
        val cur = LayoutEngine.FAMILIES.firstOrNull { it.key == selectedFamily }
        cur?.rule ?: "${LayoutEngine.TEMPLATES.size} layouts in five families, or shuffle a new one."
    }

    val filteredTemplates = remember(selectedFamily) {
        if (selectedFamily == "all") LayoutEngine.TEMPLATES
        else LayoutEngine.TEMPLATES.filter { it.family == selectedFamily }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Sticky / top header area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                subtitle = "Panel layouts",
                title = "GRID",
            )

            ModeSwitch(
                options = listOf("Columns", "Layouts"),
                selectedIndex = 1,
                onSelect = onModeSelect,
            )

            // Family choice chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                families.forEach { (key, label) ->
                    ChoiceChip(
                        label = label,
                        selected = selectedFamily == key,
                        onClick = { selectedFamily = key }
                    )
                }
            }

            // Family rule & style switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = familyRule,
                    style = typography.caption.copy(color = colors.inkMuted, fontSize = 13.sp),
                    modifier = Modifier.weight(1f)
                )

                // Style preview dots pill
                Row(
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(22.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val styles = listOf(
                        Triple("paper", Color(0xFFFFFFFF), Color(0xFFD7CFC2)),
                        Triple("ink", Color(0xFFFBF8F3), Color(0xFF151515)),
                        Triple("palette", Color(0xFFE4572E), Color(0xFF151515))
                    )
                    styles.forEach { (styleKey, dotColor, ringColor) ->
                        val isSelected = selectedStyle == styleKey
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) colors.ink else Color.Transparent)
                                .clickable { selectedStyle = styleKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(
                                        width = 3.dp,
                                        color = ringColor,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of layouts + Shuffle card
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Item 1: Shuffle card
            item {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CardRadius))
                        .background(if (colors.isDark) colors.surface else colors.ink)
                        .clickable { onOpenShuffle() }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFA7D86E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color(0xFF151515),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "SHUFFLE",
                                style = typography.display.copy(color = Color(0xFF151515), fontSize = 24.sp)
                            )
                        }
                    }

                    Text(
                        text = "Make a new one",
                        style = typography.section.copy(color = colors.ink, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "From the same rules",
                        style = typography.caption.copy(color = colors.inkMuted, fontSize = 12.sp)
                    )
                }
            }

            // Templates
            items(filteredTemplates) { template ->
                val rendered = remember(template, selectedStyle, customPaletteHexes) {
                    LayoutEngine.renderLayout(
                        node = template.node,
                        width = 145.0,
                        height = 180.0,
                        options = LayoutEngine.RenderOptions(
                            gutter = 26.0,
                            margin = 36.0,
                            style = selectedStyle,
                            customColors = if (customPaletteHexes.isNotEmpty()) customPaletteHexes else null
                        )
                    )
                }

                val familyName = remember(template.family) {
                    LayoutEngine.FAMILIES.firstOrNull { it.key == template.family }?.name ?: template.family
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CardRadius))
                        .background(colors.surface)
                        .clickable { onOpenTemplate(template) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        LayoutPreviewCanvas(
                            rendered = rendered,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = template.name,
                        style = typography.section.copy(color = colors.ink, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$familyName · ${rendered.panelCount} panels",
                        style = typography.caption.copy(color = colors.inkMuted, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}
