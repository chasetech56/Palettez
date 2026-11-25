package app.palettez.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import app.palettez.color.PresetPalette
import app.palettez.color.PresetPalettes
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.contrastColor

@Composable
fun PresetsSection(
    onSelectPalette: (PresetPalette) -> Unit,
    onFromPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    var selectedMood by remember { mutableStateOf("All") }

    val filteredPresets = remember(selectedMood) {
        if (selectedMood == "All") {
            PresetPalettes.ALL.filter { !it.isMine }
        } else {
            PresetPalettes.ALL.filter { it.mood.equals(selectedMood, ignoreCase = true) && !it.isMine }
        }
    }

    val myPalettes = remember {
        PresetPalettes.ALL.filter { it.isMine }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ---- SECTION: YOUR PALETTES ----
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "YOUR PALETTES",
                style = typography.section,
                color = colors.ink,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Saved palettes
                myPalettes.forEach { palette ->
                    YourPaletteCard(
                        palette = palette,
                        onClick = { onSelectPalette(palette) },
                    )
                }

                // "From a photo" dashed card
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 150.dp)
                        .clip(RoundedCornerShape(CardRadius))
                        .background(colors.surface.copy(alpha = 0.5f))
                        .border(
                            width = 1.5.dp,
                            color = colors.line,
                            shape = RoundedCornerShape(CardRadius),
                        )
                        .clickable(onClick = onFromPhotoClick)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "From a photo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "From a photo",
                            style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink,
                        )
                    }
                }
            }
        }

        // ---- SECTION: PRESETS ----
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PRESETS",
                    style = typography.section,
                    color = colors.ink,
                )
                Text(
                    text = "${filteredPresets.size} palettes",
                    style = typography.caption,
                    color = colors.inkMuted,
                )
            }

            // Mood Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetPalettes.MOODS.forEach { mood ->
                    ChoiceChip(
                        label = mood,
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                    )
                }
            }

            // Preset Cards List
            filteredPresets.forEach { preset ->
                PresetCard(
                    preset = preset,
                    onClick = { onSelectPalette(preset) },
                )
            }
        }
    }
}

@Composable
private fun YourPaletteCard(
    palette: PresetPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Column(
        modifier = modifier
            .size(width = 220.dp, height = 150.dp)
            .clip(RoundedCornerShape(CardRadius))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Multi-color strip at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            palette.colors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor(hex))),
                )
            }
        }

        Column {
            Text(
                text = palette.name,
                style = typography.label.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = colors.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "From ${palette.name.lowercase()}.jpg · ${palette.colors.size} colors",
                style = typography.caption,
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
fun PresetCard(
    preset: PresetPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 5 rounded tall color blocks with HEX labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            preset.colors.forEach { hex ->
                val swatchColor = Color(android.graphics.Color.parseColor(hex))
                val textColor = contrastColor(swatchColor)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(swatchColor)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = hex.removePrefix("#"),
                        style = typography.value.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        ),
                    )
                }
            }
        }

        // Title and metadata badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = preset.name,
                style = typography.headline.copy(fontSize = 20.sp),
                color = colors.ink,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Mood Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.line.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = preset.mood,
                        style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                }

                // Contrast Grade Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.ink)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "Text ${preset.contrastGrade}",
                        style = typography.caption.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onInk,
                        ),
                    )
                }
            }
        }
    }
}
