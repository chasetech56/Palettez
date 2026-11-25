package app.palettez.grid

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.layout.LayoutEngine
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.components.CompactStepperTile
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.components.StepperTile
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.PreviewRadius
import app.palettez.util.LocalMessenger
import app.palettez.util.writeBytes
import app.palettez.util.writePng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class AspectRatioOption(val label: String, val width: Double, val height: Double)

val ASPECT_OPTIONS = listOf(
    AspectRatioOption("3:4", 3.0, 4.0),
    AspectRatioOption("4:5", 4.0, 5.0),
    AspectRatioOption("1:1", 1.0, 1.0),
    AspectRatioOption("9:16", 9.0, 16.0),
    AspectRatioOption("A4", 1.0, 1.414),
)

@Composable
fun LayoutEditorScreen(
    initialTemplate: LayoutEngine.LayoutTemplate?,
    initialSeed: Int?,
    onBack: () -> Unit,
    customPaletteHexes: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    // Layout node state: either a template or procedural shuffle node
    var selectedTemplate by remember { mutableStateOf(initialTemplate) }
    var currentSeed by remember { mutableIntStateOf(initialSeed ?: Random.nextInt(1, 10000)) }
    var isShuffleMode by remember { mutableStateOf(initialTemplate == null) }

    // Editor adjustments
    var selectedAspectIndex by remember { mutableIntStateOf(0) }
    var gutter by remember { mutableIntStateOf(24) }
    var margin by remember { mutableIntStateOf(40) }
    var slantStrength by remember { mutableIntStateOf(100) }
    var mirrorX by remember { mutableStateOf(false) }
    var mirrorY by remember { mutableStateOf(false) }
    var selectedStyle by remember { mutableStateOf("paper") } // "paper", "ink", "palette"

    val activeNode: LayoutEngine.LayoutNode = remember(isShuffleMode, selectedTemplate, currentSeed) {
        if (isShuffleMode) {
            LayoutEngine.generateLayout(currentSeed, 0.45)
        } else {
            selectedTemplate?.node ?: LayoutEngine.TEMPLATES[0].node
        }
    }

    val aspect = ASPECT_OPTIONS[selectedAspectIndex]
    val canvasWidth = 314.0
    val canvasHeight = canvasWidth * (aspect.height / aspect.width)

    val renderedLayout = remember(
        activeNode,
        canvasWidth,
        canvasHeight,
        gutter,
        margin,
        slantStrength,
        mirrorX,
        mirrorY,
        selectedStyle,
        customPaletteHexes
    ) {
        LayoutEngine.renderLayout(
            node = activeNode,
            width = canvasWidth,
            height = canvasHeight,
            options = LayoutEngine.RenderOptions(
                gutter = gutter.toDouble(),
                margin = margin.toDouble(),
                slant = slantStrength / 100.0,
                mirrorX = mirrorX,
                mirrorY = mirrorY,
                style = selectedStyle,
                customColors = if (customPaletteHexes.isNotEmpty()) customPaletteHexes else null
            )
        )
    }

    val title = if (isShuffleMode) "SHUFFLE" else (selectedTemplate?.name ?: "LAYOUT")
    val familyName = if (isShuffleMode) "Generated" else {
        LayoutEngine.FAMILIES.firstOrNull { it.key == selectedTemplate?.family }?.name ?: "Layout"
    }
    val subtitle = "$familyName · ${renderedLayout.panelCount} panels"

    // Document exporters
    val savePngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.Default) {
                    val fullRender = LayoutEngine.renderLayout(
                        node = activeNode,
                        width = 1080.0,
                        height = 1080.0 * (aspect.height / aspect.width),
                        options = LayoutEngine.RenderOptions(
                            gutter = gutter.toDouble(),
                            margin = margin.toDouble(),
                            slant = slantStrength / 100.0,
                            mirrorX = mirrorX,
                            mirrorY = mirrorY,
                            style = selectedStyle,
                            customColors = if (customPaletteHexes.isNotEmpty()) customPaletteHexes else null
                        )
                    )
                    renderLayoutToBitmap(fullRender, 1080)
                }
                val success = context.writePng(uri, bmp)
                message(if (success) "Layout saved as PNG" else "Failed to save PNG")
            }
        }
    }

    val saveSvgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
        if (uri != null) {
            scope.launch {
                val svg = withContext(Dispatchers.Default) {
                    LayoutEngine.exportSvg(
                        node = activeNode,
                        width = 1080.0,
                        height = 1080.0 * (aspect.height / aspect.width),
                        options = LayoutEngine.RenderOptions(
                            gutter = gutter.toDouble(),
                            margin = margin.toDouble(),
                            slant = slantStrength / 100.0,
                            mirrorX = mirrorX,
                            mirrorY = mirrorY,
                            style = selectedStyle,
                            customColors = if (customPaletteHexes.isNotEmpty()) customPaletteHexes else null
                        )
                    )
                }
                val success = context.writeBytes(uri, svg.toByteArray())
                message(if (success) "Layout saved as SVG" else "Failed to save SVG")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconCircleButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to layouts",
                onClick = onBack
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.display.copy(fontSize = 34.sp)
                )
                Text(
                    text = subtitle,
                    style = typography.caption.copy(color = colors.inkMuted, fontSize = 13.sp)
                )
            }
        }

        // Preview canvas card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PreviewRadius))
                .background(colors.sky)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(canvasWidth.dp)
                    .height(canvasHeight.dp)
            ) {
                LayoutPreviewCanvas(
                    rendered = renderedLayout,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Shuffle + Mirror buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lime green shuffle button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFA7D86E))
                    .clickable {
                        isShuffleMode = true
                        currentSeed = Random.nextInt(1, 100000)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle",
                        tint = colors.ink,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "SHUFFLE",
                        style = typography.display.copy(color = colors.ink, fontSize = 20.sp)
                    )
                }
            }

            // Mirror X
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (mirrorX) colors.ink else colors.surface)
                    .border(
                        width = if (mirrorX) 0.dp else 1.5.dp,
                        color = colors.line,
                        shape = CircleShape
                    )
                    .clickable { mirrorX = !mirrorX },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = "Mirror left to right",
                    tint = if (mirrorX) colors.onInk else colors.ink,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Mirror Y
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (mirrorY) colors.ink else colors.surface)
                    .border(
                        width = if (mirrorY) 0.dp else 1.5.dp,
                        color = colors.line,
                        shape = CircleShape
                    )
                    .clickable { mirrorY = !mirrorY },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = "Mirror top to bottom",
                    tint = if (mirrorY) colors.onInk else colors.ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Layouts strip selector
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "LAYOUTS",
                style = typography.headline.copy(fontSize = 22.sp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LayoutEngine.TEMPLATES.forEach { template ->
                    val isSelected = !isShuffleMode && selectedTemplate?.name == template.name
                    val thumbRender = remember(template, selectedStyle) {
                        LayoutEngine.renderLayout(
                            node = template.node,
                            width = 50.0,
                            height = 65.0,
                            options = LayoutEngine.RenderOptions(
                                gutter = 16.0,
                                margin = 20.0,
                                style = selectedStyle
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) colors.ink else colors.line,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                isShuffleMode = false
                                selectedTemplate = template
                            }
                            .padding(4.dp)
                    ) {
                        LayoutPreviewCanvas(
                            rendered = thumbRender,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Page aspect ratio chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ASPECT_OPTIONS.forEachIndexed { index, opt ->
                ChoiceChip(
                    label = opt.label,
                    selected = selectedAspectIndex == index,
                    onClick = { selectedAspectIndex = index }
                )
            }
        }

        // 3 Steppers in a row: Gutter, Margin, Slant
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactStepperTile(
                label = "Gutter",
                value = gutter,
                onDecrement = { if (gutter > 4) gutter -= 4 },
                onIncrement = { if (gutter < 64) gutter += 4 },
                modifier = Modifier.weight(1f)
            )
            CompactStepperTile(
                label = "Margin",
                value = margin,
                onDecrement = { if (margin > 0) margin -= 10 },
                onIncrement = { if (margin < 120) margin += 10 },
                modifier = Modifier.weight(1f)
            )
            CompactStepperTile(
                label = "Slant",
                value = slantStrength,
                valueText = "$slantStrength%",
                onDecrement = { if (slantStrength > 0) slantStrength -= 20 },
                onIncrement = { if (slantStrength < 200) slantStrength += 20 },
                modifier = Modifier.weight(1f)
            )
        }

        // Style switcher pill (Paper, Ink, Palette)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(28.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val styleItems = listOf(
                Triple("paper", "Paper", Color(0xFFD7CFC2)),
                Triple("ink", "Ink", Color(0xFF151515)),
                Triple("palette", "Palette", Color(0xFFE4572E)),
            )
            styleItems.forEach { (key, label, dotColor) ->
                val isSelected = selectedStyle == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) colors.ink else Color.Transparent)
                        .clickable { selectedStyle = key },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = label,
                            style = typography.label.copy(
                                color = if (isSelected) colors.onInk else colors.ink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Bottom CTA buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                label = "SAVE PNG",
                onClick = { savePngLauncher.launch("layout-${title.lowercase()}.png") },
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, colors.ink, RoundedCornerShape(50))
                    .clickable { saveSvgLauncher.launch("layout-${title.lowercase()}.svg") }
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SVG",
                    style = typography.headline.copy(color = colors.ink, fontSize = 20.sp)
                )
            }
        }
    }
}
