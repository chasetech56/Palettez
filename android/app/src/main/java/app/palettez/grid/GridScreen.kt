@file:OptIn(ExperimentalLayoutApi::class)

package app.palettez.grid

import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.layout.LayoutEngine
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.ModeSwitch
import app.palettez.ui.components.ScreenHeader
import app.palettez.ui.components.StepperTile
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.PreviewRadius
import app.palettez.util.LocalMessenger
import app.palettez.util.copyText
import app.palettez.util.decodeBitmap
import app.palettez.util.writeBytes
import app.palettez.util.writePng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class CanvasPreset(val name: String, val width: Int, val height: Int)

val CANVAS_PRESETS = listOf(
    CanvasPreset("Square", 1080, 1080),
    CanvasPreset("Portrait", 1080, 1350),
    CanvasPreset("Story", 1080, 1920),
    CanvasPreset("Slide", 1920, 1080),
    CanvasPreset("A4", 2480, 3508),
    CanvasPreset("Phone", 390, 844),
)

val DEFAULT_GRID_COLORS = listOf(
    "#FF3B6B" to "Pink",
    "#2979FF" to "Blue",
    "#1B998B" to "Teal",
    "#151515" to "Black",
)

@Composable
fun GridScreen(
    vm: GridViewModel,
    paletteColors: List<Int>,
    onSubscreenChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    // Mode switch: 0 = Columns, 1 = Layouts
    var modeIndex by remember { mutableIntStateOf(0) }

    // Sub-navigation within Layouts:
    var editingTemplate by remember { mutableStateOf<LayoutEngine.LayoutTemplate?>(null) }
    var editingSeed by remember { mutableStateOf<Int?>(null) }
    var isEditingLayout by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(isEditingLayout, modeIndex) {
        onSubscreenChanged(modeIndex == 1 && isEditingLayout)
    }

    // Palette hex colors
    val paletteHexes = remember(paletteColors) {
        paletteColors.take(4).map { argb ->
            String.format("#%06X", 0xFFFFFF and argb)
        }
    }

    if (modeIndex == 1) {
        if (isEditingLayout) {
            LayoutEditorScreen(
                initialTemplate = editingTemplate,
                initialSeed = editingSeed,
                onBack = { isEditingLayout = false },
                customPaletteHexes = paletteHexes,
                modifier = modifier
            )
        } else {
            GridLayoutsScreen(
                onModeSelect = { modeIndex = it },
                onOpenTemplate = { tpl ->
                    editingTemplate = tpl
                    editingSeed = null
                    isEditingLayout = true
                },
                onOpenShuffle = {
                    editingTemplate = null
                    editingSeed = Random.nextInt(1, 100000)
                    isEditingLayout = true
                },
                customPaletteHexes = paletteHexes,
                modifier = modifier
            )
        }
        return
    }

    // --- Columns Mode ---
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    val currentPreset = CANVAS_PRESETS[selectedPresetIndex]
    val canvasW = currentPreset.width
    val canvasH = currentPreset.height

    // Grid configuration steppers
    var cols by remember { mutableIntStateOf(12) }
    var rows by remember { mutableIntStateOf(0) }
    var gutter by remember { mutableIntStateOf(20) }
    var margin by remember { mutableIntStateOf(60) }
    var baseline by remember { mutableIntStateOf(0) }
    var fillOpacity by remember { mutableIntStateOf(15) } // %

    // Guides
    var showCenterLines by remember { mutableStateOf(false) }
    var showThirds by remember { mutableStateOf(false) }
    var transparentBg by remember { mutableStateOf(false) }

    // Grid Color
    val selectableColors = remember(paletteHexes) {
        val list = DEFAULT_GRID_COLORS.toMutableList()
        paletteHexes.take(2).forEachIndexed { i, hex ->
            list.add(hex to "Palette color ${i + 1}")
        }
        list
    }
    var selectedColorHex by remember { mutableStateOf(DEFAULT_GRID_COLORS[0].first) }

    // Summary calculation
    val innerW = canvasW - 2 * margin
    val innerH = canvasH - 2 * margin
    val colW = if (cols > 0) (innerW - gutter * (cols - 1)).toDouble() / cols else 0.0
    val rowH = if (rows > 0) (innerH - gutter * (rows - 1)).toDouble() / rows else 0.0
    val isInvalid = (cols > 0 && colW <= 0) || (rows > 0 && rowH <= 0) || innerW <= 0 || innerH <= 0

    val summaryText = buildString {
        append("$canvasW × $canvasH px")
        if (cols > 0) append(" · column %.1f px".format(colW))
        if (rows > 0) append(" · row %.1f px".format(rowH))
        if (isInvalid) append(" · margins or gutters too large")
    }

    val cssCode = buildString {
        append(".container {\n")
        append("  max-width: ${canvasW}px;\n")
        append("  padding: ${margin}px;\n")
        append("  display: grid;\n")
        if (cols > 0) append("  grid-template-columns: repeat($cols, 1fr);\n")
        if (rows > 0) append("  grid-template-rows: repeat($rows, 1fr);\n")
        append("  gap: ${gutter}px;\n")
        append("}")
    }

    // Export Launchers
    val savePngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.Default) {
                    renderColumnsBitmap(
                        w = canvasW,
                        h = canvasH,
                        cols = cols,
                        rows = rows,
                        gutter = gutter,
                        margin = margin,
                        baseline = baseline,
                        fillOpacity = fillOpacity,
                        centerLines = showCenterLines,
                        thirds = showThirds,
                        transparent = transparentBg,
                        colorHex = selectedColorHex
                    )
                }
                val success = context.writePng(uri, bmp)
                message(if (success) "Grid saved as PNG" else "Failed to save PNG")
            }
        }
    }

    val saveSvgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
        if (uri != null) {
            scope.launch {
                val svg = withContext(Dispatchers.Default) {
                    renderColumnsSvg(
                        w = canvasW,
                        h = canvasH,
                        cols = cols,
                        rows = rows,
                        gutter = gutter,
                        margin = margin,
                        baseline = baseline,
                        fillOpacity = fillOpacity,
                        centerLines = showCenterLines,
                        thirds = showThirds,
                        transparent = transparentBg,
                        colorHex = selectedColorHex
                    )
                }
                val success = context.writeBytes(uri, svg.toByteArray())
                message(if (success) "Grid saved as SVG" else "Failed to save SVG")
            }
        }
    }

    // On-device Grid Analysis (GA-1 .. GA-6)
    var analysisBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val analyzePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = context.decodeBitmap(uri, maxSide = 1200)
                if (bmp != null) {
                    analysisBitmap = bmp
                } else {
                    message("Could not load image for grid analysis")
                }
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
        // Header
        ScreenHeader(
            subtitle = "Layout grids",
            title = "GRID",
            actions = {
                IconCircleButton(
                    icon = Icons.Outlined.DocumentScanner,
                    contentDescription = "Analyze poster layout",
                    onClick = { analyzePickerLauncher.launch("image/*") }
                )
                IconCircleButton(
                    icon = Icons.Outlined.Code,
                    contentDescription = "Copy CSS grid",
                    onClick = {
                        context.copyText(cssCode)
                        message("CSS grid copied")
                    }
                )
            }
        )

        // Mode switch
        ModeSwitch(
            options = listOf("Columns", "Layouts"),
            selectedIndex = 0,
            onSelect = { modeIndex = it }
        )

        // Canvas presets horizontal strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CANVAS_PRESETS.forEachIndexed { index, p ->
                val isSelected = selectedPresetIndex == index
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) colors.ink else colors.surface)
                        .clickable { selectedPresetIndex = index }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = p.name,
                            style = typography.label.copy(
                                color = if (isSelected) colors.onInk else colors.ink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${p.width}×${p.height}",
                            style = typography.caption.copy(
                                color = if (isSelected) colors.onInk.copy(alpha = 0.7f) else colors.inkMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // Preview Card
        val scale = min(314.0 / canvasW, 300.0 / canvasH)
        val previewW = (canvasW * scale).toFloat()
        val previewH = (canvasH * scale).toFloat()
        val baseGridColor = parseHexColor(selectedColorHex)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PreviewRadius))
                .background(colors.sky)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive canvas
                Box(
                    modifier = Modifier
                        .size(width = previewW.dp, height = previewH.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (transparentBg) Color(0xFFE8EEF5) else Color.White)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val s = size.width / canvasW.toFloat()
                        val m = margin.toFloat() * s
                        val g = gutter.toFloat() * s
                        val cw = colW.toFloat() * s
                        val rh = rowH.toFloat() * s
                        val iw = max(0f, size.width - 2 * m)
                        val ih = max(0f, size.height - 2 * m)

                        val fillCol = baseGridColor.copy(alpha = fillOpacity / 100f)
                        val strokeCol = baseGridColor.copy(alpha = 0.9f)

                        // Draw cells
                        if (cw > 0f || rh > 0f) {
                            if (cols > 0 && rows == 0) {
                                for (i in 0 until cols) {
                                    val left = m + i * (cw + g)
                                    drawRect(fillCol, Offset(left, m), Size(cw, ih))
                                }
                            } else if (rows > 0 && cols == 0) {
                                for (j in 0 until rows) {
                                    val top = m + j * (rh + g)
                                    drawRect(fillCol, Offset(m, top), Size(iw, rh))
                                }
                            } else if (rows > 0 && cols > 0) {
                                for (a in 0 until rows) {
                                    for (b in 0 until cols) {
                                        val left = m + b * (cw + g)
                                        val top = m + a * (rh + g)
                                        drawRect(fillCol, Offset(left, top), Size(cw, rh))
                                    }
                                }
                            }
                        }

                        // Baseline grid
                        if (baseline > 0) {
                            val b = baseline.toFloat() * s
                            var y = m + b
                            var count = 0
                            val lineCol = baseGridColor.copy(alpha = 0.35f)
                            while (y < size.height - m && count < 200) {
                                drawLine(lineCol, Offset(m, y), Offset(m + iw, y), strokeWidth = 1f)
                                y += b
                                count++
                            }
                        }

                        // Center lines
                        if (showCenterLines) {
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            drawLine(
                                strokeCol,
                                Offset(size.width / 2f, 0f),
                                Offset(size.width / 2f, size.height),
                                strokeWidth = 1.5f,
                                pathEffect = dashEffect
                            )
                            drawLine(
                                strokeCol,
                                Offset(0f, size.height / 2f),
                                Offset(size.width, size.height / 2f),
                                strokeWidth = 1.5f,
                                pathEffect = dashEffect
                            )
                        }

                        // Rule of thirds
                        if (showThirds) {
                            val blue = Color(0xFF2979FF)
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            for (k in 1..2) {
                                val x = size.width * k / 3f
                                val y = size.height * k / 3f
                                drawLine(blue, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f, pathEffect = dashEffect)
                                drawLine(blue, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f, pathEffect = dashEffect)
                            }
                        }

                        // Margin bounding box
                        drawRect(
                            color = strokeCol,
                            topLeft = Offset(m, m),
                            size = Size(iw, ih),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                // Summary pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (colors.isDark) Color(0xFF1D2320).copy(alpha = 0.88f) else Color(0xFFFBF8F3).copy(alpha = 0.88f))
                        .then(
                            if (colors.isDark) Modifier.border(1.dp, colors.line, RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summaryText,
                        style = typography.caption.copy(
                            color = if (isInvalid) Color(0xFFB84423) else (if (colors.isDark) Color.White else Color(0xFF151515)),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        // 6 Steppers in 2 columns
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperTile(
                    label = "Columns",
                    value = cols,
                    onDecrement = { if (cols > 0) cols-- },
                    onIncrement = { if (cols < 24) cols++ },
                    modifier = Modifier.weight(1f)
                )
                StepperTile(
                    label = "Rows",
                    value = rows,
                    onDecrement = { if (rows > 0) rows-- },
                    onIncrement = { if (rows < 24) rows++ },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperTile(
                    label = "Gutter",
                    value = gutter,
                    onDecrement = { if (gutter >= 4) gutter -= 4 },
                    onIncrement = { if (gutter <= 196) gutter += 4 },
                    modifier = Modifier.weight(1f)
                )
                StepperTile(
                    label = "Margins",
                    value = margin,
                    onDecrement = { if (margin >= 10) margin -= 10 },
                    onIncrement = { if (margin <= 390) margin += 10 },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperTile(
                    label = "Baseline",
                    value = baseline,
                    onDecrement = { if (baseline >= 4) baseline -= 4 },
                    onIncrement = { if (baseline <= 60) baseline += 4 },
                    modifier = Modifier.weight(1f)
                )
                StepperTile(
                    label = "Fill",
                    value = fillOpacity,
                    valueText = "$fillOpacity%",
                    onDecrement = { if (fillOpacity >= 5) fillOpacity -= 5 },
                    onIncrement = { if (fillOpacity <= 55) fillOpacity += 5 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Guides and Color card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(colors.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "GUIDES",
                style = typography.display.copy(fontSize = 22.sp, letterSpacing = 0.3.sp, color = colors.ink)
            )

            // Guide toggles
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoiceChip(
                    label = "Center lines",
                    selected = showCenterLines,
                    onClick = { showCenterLines = !showCenterLines }
                )
                ChoiceChip(
                    label = "Rule of thirds",
                    selected = showThirds,
                    onClick = { showThirds = !showThirds }
                )
                ChoiceChip(
                    label = "Transparent",
                    selected = transparentBg,
                    onClick = { transparentBg = !transparentBg }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "GRID COLOR",
                style = typography.display.copy(fontSize = 22.sp, letterSpacing = 0.3.sp, color = colors.ink)
            )

            // Color circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectableColors.forEach { (hex, name) ->
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    val c = parseHexColor(hex)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, colors.ink, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, colors.line, CircleShape)
                            )
                        }
                    }
                }
            }

            Text(
                text = "The last two come from your palette.",
                style = typography.caption.copy(color = colors.inkMuted, fontSize = 12.sp)
            )
        }

        // Analyze Poster discovery card (GA-1 .. GA-6)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(CardRadius))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "ANALYZE POSTER OR FLYER",
                            style = typography.label.copy(fontWeight = FontWeight.Bold),
                            color = colors.ink
                        )
                        Text(
                            text = "Detect columns, gutters, and layout style on-device",
                            style = typography.caption.copy(color = colors.inkMuted)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.lime)
                            .clickable { analyzePickerLauncher.launch("image/*") }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFF151515),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "UPLOAD",
                                style = typography.label.copy(color = Color(0xFF151515), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Export CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFA7D86E))
                    .clickable { savePngLauncher.launch("grid-${canvasW}x$canvasH.png") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE PNG",
                    style = typography.display.copy(color = colors.ink, fontSize = 22.sp)
                )
            }

            Box(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.ink)
                    .clickable { saveSvgLauncher.launch("grid-${canvasW}x$canvasH.svg") }
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SVG",
                    style = typography.display.copy(color = Color.White, fontSize = 22.sp)
                )
            }
        }
    }

    val currentAnalysisBitmap = analysisBitmap
    if (currentAnalysisBitmap != null) {
        GridAnalysisDialog(
            bitmap = currentAnalysisBitmap,
            onDismiss = { analysisBitmap = null },
            onApplyToColumns = { detectedSpec ->
                cols = detectedSpec.columns
                gutter = detectedSpec.gutter
                margin = detectedSpec.marginLeft
                analysisBitmap = null
                message("Applied detected ${detectedSpec.columns}-column grid (${detectedSpec.gutter}px gutter)")
            },
            onOpenInLayouts = { tpl ->
                analysisBitmap = null
                editingTemplate = tpl
                editingSeed = null
                isEditingLayout = true
                modeIndex = 1
                message("Opened ${tpl.name} panel layout")
            }
        )
    }
}

// Helpers for bitmap & SVG rendering
fun renderColumnsBitmap(
    w: Int,
    h: Int,
    cols: Int,
    rows: Int,
    gutter: Int,
    margin: Int,
    baseline: Int,
    fillOpacity: Int,
    centerLines: Boolean,
    thirds: Boolean,
    transparent: Boolean,
    colorHex: String,
): Bitmap {
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    if (!transparent) {
        val bgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
    }

    val baseColor = android.graphics.Color.parseColor(colorHex)
    val alphaInt = ((fillOpacity / 100.0) * 255).roundToInt()
    val cellPaint = Paint().apply {
        isAntiAlias = true
        color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint().apply {
        isAntiAlias = true
        color = (baseColor and 0x00FFFFFF) or (0xE6 shl 24)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    val innerW = w - 2 * margin
    val innerH = h - 2 * margin
    val colW = if (cols > 0) (innerW - gutter * (cols - 1)).toFloat() / cols else 0f
    val rowH = if (rows > 0) (innerH - gutter * (rows - 1)).toFloat() / rows else 0f

    // Draw cells
    if (colW > 0f || rowH > 0f) {
        if (cols > 0 && rows == 0) {
            for (i in 0 until cols) {
                val left = margin + i * (colW + gutter)
                canvas.drawRect(left, margin.toFloat(), left + colW, (margin + innerH).toFloat(), cellPaint)
            }
        } else if (rows > 0 && cols == 0) {
            for (j in 0 until rows) {
                val top = margin + j * (rowH + gutter)
                canvas.drawRect(margin.toFloat(), top, (margin + innerW).toFloat(), top + rowH, cellPaint)
            }
        } else if (rows > 0 && cols > 0) {
            for (a in 0 until rows) {
                for (b in 0 until cols) {
                    val left = margin + b * (colW + gutter)
                    val top = margin + a * (rowH + gutter)
                    canvas.drawRect(left, top, left + colW, top + rowH, cellPaint)
                }
            }
        }
    }

    // Baselines
    if (baseline > 0) {
        val baseLinePaint = Paint().apply {
            color = (baseColor and 0x00FFFFFF) or (0x59 shl 24)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        var y = margin + baseline
        while (y < h - margin) {
            canvas.drawLine(margin.toFloat(), y.toFloat(), (w - margin).toFloat(), y.toFloat(), baseLinePaint)
            y += baseline
        }
    }

    // Center lines
    if (centerLines) {
        val dashPaint = Paint().apply {
            color = strokePaint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
        }
        canvas.drawLine(w / 2f, 0f, w / 2f, h.toFloat(), dashPaint)
        canvas.drawLine(0f, h / 2f, w.toFloat(), h / 2f, dashPaint)
    }

    // Rule of thirds
    if (thirds) {
        val bluePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#2979FF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
        }
        for (k in 1..2) {
            canvas.drawLine(w * k / 3f, 0f, w * k / 3f, h.toFloat(), bluePaint)
            canvas.drawLine(0f, h * k / 3f, w.toFloat(), h * k / 3f, bluePaint)
        }
    }

    // Margin border
    canvas.drawRect(margin.toFloat(), margin.toFloat(), (w - margin).toFloat(), (h - margin).toFloat(), strokePaint)

    return bitmap
}

fun renderColumnsSvg(
    w: Int,
    h: Int,
    cols: Int,
    rows: Int,
    gutter: Int,
    margin: Int,
    baseline: Int,
    fillOpacity: Int,
    centerLines: Boolean,
    thirds: Boolean,
    transparent: Boolean,
    colorHex: String,
): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    sb.append("<svg width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\" xmlns=\"http://www.w3.org/2000/svg\">\n")
    if (!transparent) {
        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\" />\n")
    }
    val op = fillOpacity / 100.0
    val innerW = w - 2 * margin
    val innerH = h - 2 * margin
    val colW = if (cols > 0) (innerW - gutter * (cols - 1)).toDouble() / cols else 0.0
    val rowH = if (rows > 0) (innerH - gutter * (rows - 1)).toDouble() / rows else 0.0

    sb.append("  <g id=\"grid-cells\" fill=\"$colorHex\" fill-opacity=\"$op\">\n")
    if (colW > 0.0 || rowH > 0.0) {
        if (cols > 0 && rows == 0) {
            for (i in 0 until cols) {
                val left = margin + i * (colW + gutter)
                sb.append("    <rect x=\"%.1f\" y=\"$margin\" width=\"%.1f\" height=\"$innerH\" />\n".format(left, colW))
            }
        } else if (rows > 0 && cols == 0) {
            for (j in 0 until rows) {
                val top = margin + j * (rowH + gutter)
                sb.append("    <rect x=\"$margin\" y=\"%.1f\" width=\"$innerW\" height=\"%.1f\" />\n".format(top, rowH))
            }
        } else if (rows > 0 && cols > 0) {
            for (a in 0 until rows) {
                for (b in 0 until cols) {
                    val left = margin + b * (colW + gutter)
                    val top = margin + a * (rowH + gutter)
                    sb.append("    <rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" />\n".format(left, top, colW, rowH))
                }
            }
        }
    }
    sb.append("  </g>\n")
    sb.append("  <rect x=\"$margin\" y=\"$margin\" width=\"$innerW\" height=\"$innerH\" fill=\"none\" stroke=\"$colorHex\" stroke-width=\"1.5\" />\n")
    sb.append("</svg>")
    return sb.toString()
}
