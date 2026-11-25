package app.palettez.palette

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.color.ColorEngine
import app.palettez.color.PresetPalette
import app.palettez.color.PresetPalettes
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.ScreenPadding
import app.palettez.ui.theme.contrastColor
import app.palettez.util.LocalMessenger
import app.palettez.util.copyText

@Composable
fun ColorSystemScreen(
    initialPalette: PresetPalette,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    var currentPalette by remember { mutableStateOf(initialPalette) }
    var currentRoles by remember(currentPalette) { mutableStateOf(currentPalette.roles) }
    var selectedColorForScale by remember(currentPalette) { mutableStateOf(currentPalette.colors[0]) }
    var showPresentationDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    fun copy(text: String, what: String = text) {
        context.copyText(text)
        message("Copied $what")
    }

    // Role changer: cycle through available colors + ink/white for text and background
    fun cycleRole(roleKey: String) {
        val options = when (roleKey) {
            "background", "text" -> currentPalette.colors + listOf("#151515", "#FFFFFF")
            else -> currentPalette.colors
        }
        val currentHex = currentRoles[roleKey]
        val currentIndex = options.indexOf(currentHex)
        val nextHex = options[(currentIndex + 1) % options.size]
        currentRoles = when (roleKey) {
            "background" -> currentRoles.copy(background = nextHex)
            "text" -> currentRoles.copy(text = nextHex)
            "primary" -> currentRoles.copy(primary = nextHex)
            "secondary" -> currentRoles.copy(secondary = nextHex)
            "accent" -> currentRoles.copy(accent = nextHex)
            else -> currentRoles
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ---- Top Header with Back Button ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconCircleButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentPalette.name.uppercase(),
                    style = typography.display.copy(fontSize = 36.sp),
                    color = colors.ink,
                )
                Text(
                    text = "${currentPalette.mood} · ${currentPalette.colors.size} colors · tap Change to set roles",
                    style = typography.caption,
                    color = colors.inkMuted,
                )
            }
        }

        // ---- Quick Palette Switcher ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetPalettes.ALL.forEach { p ->
                val isSelected = p.name == currentPalette.name
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) colors.ink else colors.surface)
                        .clickable { currentPalette = p }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Mini 2-dot color preview
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        p.colors.take(2).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex))),
                            )
                        }
                    }
                    Text(
                        text = p.name,
                        style = typography.label.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.ink,
                        ),
                    )
                }
            }
        }

        // ---- LIVE PREVIEW CARD (Open Studio mockup) ----
        val previewBg = Color(android.graphics.Color.parseColor(currentRoles.background))
        val previewText = Color(android.graphics.Color.parseColor(currentRoles.text))
        val primaryBtnColor = Color(android.graphics.Color.parseColor(currentRoles.primary))
        val secondaryBtnColor = Color(android.graphics.Color.parseColor(currentRoles.secondary))
        val accentColor = Color(android.graphics.Color.parseColor(currentRoles.accent))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(previewBg)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top metadata line + Accent badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Saturday · 10:00",
                    style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = previewText,
                )

                // Accent badge ("New")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "New",
                        style = typography.caption.copy(
                            fontWeight = FontWeight.Bold,
                            color = contrastColor(accentColor),
                        ),
                    )
                }
            }

            // Headline
            Text(
                text = "OPEN STUDIO",
                style = typography.headline.copy(fontSize = 38.sp),
                color = previewText,
            )

            // Body
            Text(
                text = "A morning of color, type and layout. Bring a sketchbook.",
                style = typography.body,
                color = previewText.copy(alpha = 0.9f),
            )

            // Buttons: Primary & Secondary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Primary button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(primaryBtnColor)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Book a spot",
                        style = typography.label.copy(fontWeight = FontWeight.Bold),
                        color = contrastColor(primaryBtnColor),
                    )
                }

                // Secondary button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(secondaryBtnColor)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Details",
                        style = typography.label.copy(fontWeight = FontWeight.Bold),
                        color = contrastColor(secondaryBtnColor),
                    )
                }
            }
        }

        // ---- SECTION: ROLES ----
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ROLES",
                    style = typography.section,
                    color = colors.ink,
                )
                Text(
                    text = "Auto-assign",
                    style = typography.label.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.ink,
                    ),
                    modifier = Modifier.clickable {
                        currentRoles = ColorEngine.autoRoles(currentPalette.colors)
                        message("Auto-assigned roles")
                    },
                )
            }

            // Roles Card containing 5 rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(colors.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorEngine.ROLE_ORDER.forEach { roleKey ->
                    val hex = currentRoles[roleKey]
                    val roleLabel = ColorEngine.ROLE_NAMES[roleKey] ?: roleKey.replaceFirstChar { it.uppercase() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Swatch box
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(1.dp, colors.line, RoundedCornerShape(12.dp)),
                            )

                            Column {
                                Text(
                                    text = roleLabel,
                                    style = typography.label.copy(fontWeight = FontWeight.Bold),
                                    color = colors.ink,
                                )
                                Text(
                                    text = hex,
                                    style = typography.value.copy(fontSize = 11.sp),
                                    color = colors.inkMuted,
                                )
                            }
                        }

                        // Change button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.line.copy(alpha = 0.35f))
                                .clickable { cycleRole(roleKey) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "Change",
                                style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.ink,
                            )
                        }
                    }
                }
            }
        }

        // ---- SECTION: 10-STEP SCALE ----
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "SCALE",
                style = typography.section,
                color = colors.ink,
            )

            // Color selector circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                currentPalette.colors.forEach { hex ->
                    val isSelected = hex == selectedColorForScale
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .then(
                                if (isSelected) Modifier.border(3.dp, colors.ink, CircleShape) else Modifier
                            )
                            .clickable { selectedColorForScale = hex },
                    )
                }
            }

            // 10-step scale strip
            val scaleSteps = remember(selectedColorForScale) { ColorEngine.scaleOf(selectedColorForScale) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(colors.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scaleSteps.forEach { step ->
                    val stepColor = Color(android.graphics.Color.parseColor(step.hex))
                    val stepTextColor = contrastColor(stepColor)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(stepColor)
                            .clickable { copy(step.hex, "step ${step.step} (${step.hex})") }
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${step.step}",
                            style = typography.label.copy(fontWeight = FontWeight.Bold, color = stepTextColor),
                        )
                        Text(
                            text = step.hex,
                            style = typography.value.copy(fontWeight = FontWeight.Bold, color = stepTextColor),
                        )
                    }
                }
            }
        }

        // ---- SECTION: HARMONIES ----
        val harmonyList = remember(selectedColorForScale) { ColorEngine.harmonies(selectedColorForScale) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "HARMONIES",
                style = typography.section,
                color = colors.ink,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(colors.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                harmonyList.forEach { harm ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = harm.name,
                            style = typography.label.copy(fontWeight = FontWeight.Bold),
                            color = colors.ink,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        ) {
                            harm.colors.forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { copy(hex) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = hex.removePrefix("#"),
                                        style = typography.value.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contrastColor(Color(android.graphics.Color.parseColor(hex))),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- SECTION: CONTRAST MATRIX ----
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "CONTRAST",
                style = typography.section,
                color = colors.ink,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(colors.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val pairings = listOf(
                    Pair("Text on Background", Pair(currentRoles.text, currentRoles.background)),
                    Pair("Primary on Background", Pair(currentRoles.primary, currentRoles.background)),
                    Pair("Secondary on Background", Pair(currentRoles.secondary, currentRoles.background)),
                    Pair("Accent on Background", Pair(currentRoles.accent, currentRoles.background)),
                )

                pairings.forEach { (label, pair) ->
                    val ratio = ColorEngine.contrast(pair.first, pair.second)
                    val grade = ColorEngine.grade(ratio)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = typography.body,
                            color = colors.ink,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "%.1f:1".format(ratio),
                                style = typography.value.copy(fontWeight = FontWeight.Bold),
                                color = colors.ink,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (grade == "Fail") colors.coral else colors.lime)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = grade,
                                    style = typography.caption.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (grade == "Fail") Color.White else colors.ink,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- SECTION: CODE EXPORT ----
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "EXPORT",
                style = typography.section,
                color = colors.ink,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChoiceChip(
                    label = "Brand Card",
                    selected = false,
                    onClick = { showPresentationDialog = true },
                )
                ChoiceChip(
                    label = "Copy CSS",
                    selected = false,
                    onClick = {
                        val css = ColorEngine.exportCode("css", currentPalette.name, currentPalette.colors, currentRoles)
                        copy(css, "CSS variables")
                    },
                )
                ChoiceChip(
                    label = "Tailwind",
                    selected = false,
                    onClick = {
                        val tw = ColorEngine.exportCode("tailwind", currentPalette.name, currentPalette.colors, currentRoles)
                        copy(tw, "Tailwind config")
                    },
                )
                ChoiceChip(
                    label = "JSON",
                    selected = false,
                    onClick = {
                        val js = ColorEngine.exportCode("json", currentPalette.name, currentPalette.colors, currentRoles)
                        copy(js, "JSON tokens")
                    },
                )
            }
        }
    }

    if (showPresentationDialog && currentPalette.colors.isNotEmpty()) {
        val presentationSwatches = currentPalette.colors.map { hex ->
            val (r, g, b) = ColorEngine.hexToRgb(hex)
            Swatch(
                r = r,
                g = g,
                b = b,
                share = 1f / currentPalette.colors.size
            )
        }

        ColorPresentationDialog(
            swatches = presentationSwatches,
            defaultTitle = currentPalette.name,
            onDismiss = { showPresentationDialog = false }
        )
    }
}
