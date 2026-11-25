package app.palettez.fonts

import android.graphics.Typeface
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle as ComposeFontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.ModeSwitch
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.util.LocalMessenger
import app.palettez.util.copyText
import app.palettez.util.openUrl
import app.palettez.util.writeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val WeightNames = mapOf(
    100 to "Thin",
    200 to "ExtraLight",
    300 to "Light",
    400 to "Regular",
    500 to "Medium",
    600 to "SemiBold",
    700 to "Bold",
    800 to "ExtraBold",
    900 to "Black"
)

@Composable
fun FontDetailScreen(
    font: FontInfo,
    loader: FontLoader,
    allFonts: List<FontInfo> = emptyList(),
    cardBgColor: Color = Color(0xFFA7D86E),
    cardTextColor: Color = Color(0xFF151515),
    previewText: String = "The quick brown fox jumps over the lazy dog",
    onSelectFont: ((FontInfo) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val typography = PalettezTheme.typography

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Specimen, 1 = Pairings & Studio
    var customPreview by remember { mutableStateOf(previewText) }
    var previewSize by remember { mutableFloatStateOf(28f) }

    val fallbackFamily = remember(font.category) {
        when (font.category) {
            FontCategory.Serif -> FontFamily.Serif
            FontCategory.Monospace -> FontFamily.Monospace
            FontCategory.Handwriting -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        }
    }

    // Hero preview typeface (Regular 400 or closest available)
    var heroTypeface by remember(font) { mutableStateOf(loader.peek(font.family, font.previewStyle)) }
    LaunchedEffect(font) {
        if (heroTypeface == null) heroTypeface = loader.load(font.family, font.previewStyle)
    }
    val heroFontFamily = remember(heroTypeface) {
        heroTypeface?.let { FontFamily(it) } ?: fallbackFamily
    }

    // Pairings calculations (FP-1 .. FP-6)
    val pairings = remember(font, allFonts) {
        FontPairingEngine.getPairings(font, allFonts)
    }
    var activePairingIndex by remember { mutableIntStateOf(0) }
    val activePairing = pairings.getOrNull(activePairingIndex)
    val companionFont = activePairing?.companionFont ?: remember(activePairing) {
        activePairing?.let {
            FontInfo(it.companionFamily, FontCategory.SansSerif, listOf(FontStyle(400, false)), 100, "", false)
        }
    }

    var isRoleSwapped by remember { mutableStateOf(false) }
    var studioHeadlineText by remember { mutableStateOf("Design Systems That Scale") }
    var studioHeadlineSize by remember { mutableFloatStateOf(34f) }
    var studioBodyText by remember {
        mutableStateOf("A balanced typographic hierarchy harmonizes distinct structural personalities with consistent proportion, rhythm, and clarity across every digital viewport.")
    }
    var studioBodySize by remember { mutableFloatStateOf(15f) }

    val headlineFont = if (!isRoleSwapped) font else (companionFont ?: font)
    val bodyFont = if (!isRoleSwapped) (companionFont ?: font) else font

    var headlineTypeface by remember(headlineFont) { mutableStateOf(loader.peek(headlineFont.family, headlineFont.previewStyle)) }
    LaunchedEffect(headlineFont) {
        if (headlineTypeface == null) headlineTypeface = loader.load(headlineFont.family, headlineFont.previewStyle)
    }
    val headlineFontFamily = remember(headlineTypeface) {
        headlineTypeface?.let { FontFamily(it) } ?: fallbackFamily
    }

    var bodyTypeface by remember(bodyFont) { mutableStateOf(loader.peek(bodyFont.family, bodyFont.previewStyle)) }
    LaunchedEffect(bodyFont) {
        if (bodyTypeface == null) bodyTypeface = loader.load(bodyFont.family, bodyFont.previewStyle)
    }
    val bodyFontFamily = remember(bodyTypeface) {
        bodyTypeface?.let { FontFamily(it) } ?: fallbackFamily
    }

    // Save individual style launcher
    var pendingSaveStyle by remember { mutableStateOf<FontStyle?>(null) }
    val saveSingleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("font/ttf")) { uri ->
        val styleToSave = pendingSaveStyle
        if (uri != null && styleToSave != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    val file = loader.fileFor(font.family, styleToSave)
                    if (file.exists() && file.length() > 0L) file.readBytes()
                    else {
                        loader.load(font.family, styleToSave)
                        val freshFile = loader.fileFor(font.family, styleToSave)
                        if (freshFile.exists()) freshFile.readBytes() else null
                    }
                }
                if (bytes != null && context.writeBytes(uri, bytes)) {
                    val name = WeightNames[styleToSave.weight] ?: "${styleToSave.weight}"
                    val itTag = if (styleToSave.italic) "Italic" else ""
                    message("${font.family}-$name$itTag.ttf saved")
                } else {
                    message("Failed to save font")
                }
            }
        }
        pendingSaveStyle = null
    }

    // Save all styles bundle (ZIP) launcher
    val saveAllLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                val zipBytes = withContext(Dispatchers.IO) {
                    val bos = ByteArrayOutputStream()
                    ZipOutputStream(bos).use { zos ->
                        for (style in font.styles) {
                            loader.load(font.family, style)
                            val f = loader.fileFor(font.family, style)
                            if (f.exists() && f.length() > 0L) {
                                val name = WeightNames[style.weight] ?: "${style.weight}"
                                val itTag = if (style.italic) "Italic" else ""
                                val entryName = "${font.family}-$name$itTag.ttf"
                                zos.putNextEntry(ZipEntry(entryName))
                                zos.write(f.readBytes())
                                zos.closeEntry()
                            }
                        }
                    }
                    bos.toByteArray()
                }
                if (zipBytes != null && context.writeBytes(uri, zipBytes)) {
                    message("${font.family} (${font.styles.size} styles) bundle saved")
                } else {
                    message("Failed to save font bundle")
                }
            }
        }
    }

    // Dual font ZIP launcher (FP-5)
    val saveDualLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && companionFont != null) {
            scope.launch {
                val zipBytes = withContext(Dispatchers.IO) {
                    val bos = ByteArrayOutputStream()
                    ZipOutputStream(bos).use { zos ->
                        val pairFonts = listOf(font, companionFont)
                        for (fInfo in pairFonts) {
                            for (style in fInfo.styles) {
                                loader.load(fInfo.family, style)
                                val f = loader.fileFor(fInfo.family, style)
                                if (f.exists() && f.length() > 0L) {
                                    val name = WeightNames[style.weight] ?: "${style.weight}"
                                    val itTag = if (style.italic) "Italic" else ""
                                    val entryName = "${fInfo.family}/$name$itTag.ttf"
                                    zos.putNextEntry(ZipEntry(entryName))
                                    zos.write(f.readBytes())
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                    bos.toByteArray()
                }
                if (zipBytes != null && context.writeBytes(uri, zipBytes)) {
                    message("Dual pairing bundle (${font.family} + ${companionFont.family}) saved")
                } else {
                    message("Failed to save dual font bundle")
                }
            }
        }
    }

    // Full screen with dark ground #1D2320 as specified in the Bold design
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1D2320))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Specimen Hero Banner — Matches Card Color from Fonts Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(cardBgColor)
            ) {
                // Ghosted family name in background rendered in font's own typeface
                Text(
                    text = font.family,
                    fontFamily = heroFontFamily,
                    fontSize = 72.sp,
                    lineHeight = 70.sp,
                    color = cardTextColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 56.dp)
                )

                // Large center glyphs "Aa" and "g&" showcasing the font's character
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aa",
                        fontFamily = heroFontFamily,
                        fontSize = 110.sp,
                        lineHeight = 110.sp,
                        color = cardTextColor
                    )
                    Text(
                        text = "g&",
                        fontFamily = heroFontFamily,
                        fontSize = 62.sp,
                        lineHeight = 62.sp,
                        color = cardTextColor.copy(alpha = 0.85f)
                    )
                }

                // Top navigation bar over banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircleButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to fonts",
                        onClick = onBack
                    )
                    IconCircleButton(
                        icon = Icons.Outlined.Link,
                        contentDescription = "Copy embed link",
                        onClick = {
                            context.copyText(font.embedLink)
                            message("HTML link copied")
                        }
                    )
                }
            }

            // Body content section on dark ground
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Google Fonts tag & Family Name
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(cardBgColor)
                        )
                        Text(
                            text = "Google Fonts",
                            style = typography.caption.copy(color = Color(0xFFC9CEC9), fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = font.family.uppercase(),
                            fontFamily = heroFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "FREE",
                            style = typography.display.copy(fontSize = 28.sp, color = cardBgColor)
                        )
                    }

                    Text(
                        text = "${font.category.label} · open source font family",
                        style = typography.body.copy(color = Color(0xFFC9CEC9), fontSize = 14.sp)
                    )
                }

                // Info tiles (Styles count & SIL Open License)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(CardRadius))
                            .background(Color(0xFF2A312D))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "${font.styles.size} styles",
                                style = typography.section.copy(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            )
                            val weights = font.styles.map { it.weight }.distinct().sorted()
                            val weightRange = if (weights.size > 1) "${weights.first()}–${weights.last()}" else "${weights.firstOrNull() ?: 400}"
                            val hasItalics = font.styles.any { it.italic }
                            Text(
                                text = "$weightRange${if (hasItalics) ", with italics" else ""}",
                                style = typography.caption.copy(color = Color(0xFFC9CEC9), fontSize = 12.sp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(CardRadius))
                            .background(Color(0xFF2A312D))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Open source",
                                style = typography.section.copy(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "SIL Open Font License",
                                style = typography.caption.copy(color = Color(0xFFC9CEC9), fontSize = 12.sp)
                            )
                        }
                    }
                }

                // Mode switch: Specimen vs Pairings & Studio (FP-2, FP-3)
                ModeSwitch(
                    options = listOf("Specimen", "Pairings & Studio"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )

                if (selectedTab == 0) {
                    // TAB 0: SPECIMEN VIEW
                    // Action pills: Copy <link>, Copy CSS, Copy name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val actions = listOf(
                            "Copy <link>" to {
                                context.copyText(font.embedLink)
                                message("HTML link copied")
                            },
                            "Copy CSS" to {
                                context.copyText(font.cssRule)
                                message("CSS copied")
                            },
                            "Copy name" to {
                                context.copyText(font.family)
                                message("Name copied")
                            }
                        )
                        actions.forEach { (label, onClick) ->
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0xFF2A312D))
                                    .clickable { onClick() }
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = typography.label.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                )
                            }
                        }
                    }

                    // Interactive Custom Preview & Size Slider Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(Color(0xFF2A312D))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUSTOM PREVIEW",
                                    style = typography.caption.copy(
                                        color = Color(0xFFC9CEC9),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "${previewSize.toInt()} px",
                                    style = typography.value.copy(color = cardBgColor, fontSize = 12.sp)
                                )
                            }

                            BasicTextField(
                                value = customPreview,
                                onValueChange = { customPreview = it },
                                textStyle = TextStyle(
                                    fontFamily = heroFontFamily,
                                    fontSize = previewSize.sp,
                                    lineHeight = (previewSize * 1.25f).sp,
                                    color = Color.White
                                ),
                                cursorBrush = SolidColor(cardBgColor),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Slider(
                                value = previewSize,
                                onValueChange = { previewSize = it },
                                valueRange = 16f..56f,
                                colors = SliderDefaults.colors(
                                    thumbColor = cardBgColor,
                                    activeTrackColor = cardBgColor,
                                    inactiveTrackColor = Color(0xFF3E4741)
                                )
                            )
                        }
                    }

                    // Full Alphabet & Numerals Specimen Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(Color(0xFF242A26))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "GLYPH SET",
                                style = typography.caption.copy(
                                    color = Color(0xFFC9CEC9),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z",
                                fontFamily = heroFontFamily,
                                fontSize = 17.sp,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                            Text(
                                text = "a b c d e f g h i j k l m n o p q r s t u v w x y z",
                                fontFamily = heroFontFamily,
                                fontSize = 17.sp,
                                color = Color(0xFFE2E7E2),
                                lineHeight = 24.sp
                            )
                            Text(
                                text = "0 1 2 3 4 5 6 7 8 9   ! @ # $ % & * ( ) , . ?",
                                fontFamily = heroFontFamily,
                                fontSize = 16.sp,
                                color = cardBgColor,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // STYLES Section Header
                    Text(
                        text = "STYLES (${font.styles.size})",
                        style = typography.display.copy(fontSize = 24.sp, color = Color.White)
                    )

                    // List of styles with live previews
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        font.styles.forEach { style ->
                            StyleSpecimenRow(
                                font = font,
                                style = style,
                                previewText = customPreview.ifEmpty { "Sphinx of black quartz, judge my vow" },
                                fontSize = previewSize,
                                loader = loader,
                                onSaveStyle = {
                                    pendingSaveStyle = style
                                    val wName = WeightNames[style.weight] ?: "${style.weight}"
                                    val itTag = if (style.italic) "Italic" else ""
                                    saveSingleLauncher.launch("${font.family}-$wName$itTag.ttf")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // External link
                    Text(
                        text = "Open on Google Fonts ↗",
                        style = typography.body.copy(color = cardBgColor, fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable { context.openUrl(font.specimenUrl) }
                    )

                } else {
                    // TAB 1: PAIRINGS & STUDIO (FP-1 .. FP-6)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SUGGESTED COMPANIONS",
                                    style = typography.display.copy(fontSize = 22.sp, color = Color.White)
                                )
                                Text(
                                    text = "Archetype-based typographic harmony",
                                    style = typography.caption.copy(color = Color(0xFFC9CEC9))
                                )
                            }
                            Text(
                                text = "${pairings.size} matches",
                                style = typography.value.copy(color = cardBgColor, fontSize = 13.sp)
                            )
                        }

                        // Companion cards list
                        pairings.forEachIndexed { idx, pair ->
                            val isSelected = idx == activePairingIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(CardRadius))
                                    .background(if (isSelected) Color(0xFF2C3630) else Color(0xFF222824))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) cardBgColor else Color(0xFF343C37),
                                        shape = RoundedCornerShape(CardRadius)
                                    )
                                    .clickable { activePairingIndex = idx }
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = pair.companionFamily.uppercase(),
                                                style = typography.display.copy(fontSize = 20.sp, color = Color.White)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF343C37))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = pair.role,
                                                    style = typography.caption.copy(color = Color(0xFFE2E7E2), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(cardBgColor.copy(alpha = 0.18f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${pair.matchScore}% MATCH",
                                                style = typography.value.copy(color = cardBgColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    Text(
                                        text = pair.rationale,
                                        style = typography.caption.copy(color = Color(0xFFC9CEC9), fontSize = 13.sp, lineHeight = 18.sp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isSelected) "Active in Studio below" else "Tap to preview in Studio",
                                            style = typography.caption.copy(
                                                color = if (isSelected) cardBgColor else Color(0xFF8E958E),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )

                                        if (pair.companionFont != null && onSelectFont != null) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { onSelectFont(pair.companionFont) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "View family",
                                                    style = typography.caption.copy(color = cardBgColor, fontWeight = FontWeight.Bold)
                                                )
                                                Icon(
                                                    imageVector = Icons.Outlined.NorthEast,
                                                    contentDescription = null,
                                                    tint = cardBgColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // INTERACTIVE SPLIT SPECIMEN STUDIO (FP-3)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF151515))
                                .border(1.5.dp, Color(0xFF343C37), RoundedCornerShape(22.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "PAIRING STUDIO",
                                            style = typography.display.copy(fontSize = 24.sp, color = Color.White)
                                        )
                                        Text(
                                            text = "${headlineFont.family} + ${bodyFont.family}",
                                            style = typography.caption.copy(color = cardBgColor, fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // Role Swap Button (FP-3)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF2A312D))
                                            .clickable { isRoleSwapped = !isRoleSwapped }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SwapVert,
                                                contentDescription = "Swap Headline and Body roles",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "SWAP ROLES",
                                                style = typography.label.copy(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                // 1. Headline Specimen Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222824))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "HEADLINE · ${headlineFont.family.uppercase()}",
                                                style = typography.caption.copy(
                                                    color = cardBgColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Text(
                                                text = "${studioHeadlineSize.toInt()} px",
                                                style = typography.value.copy(color = Color(0xFFC9CEC9), fontSize = 12.sp)
                                            )
                                        }

                                        BasicTextField(
                                            value = studioHeadlineText,
                                            onValueChange = { studioHeadlineText = it },
                                            textStyle = TextStyle(
                                                fontFamily = headlineFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = studioHeadlineSize.sp,
                                                lineHeight = (studioHeadlineSize * 1.15f).sp,
                                                color = Color.White
                                            ),
                                            cursorBrush = SolidColor(cardBgColor),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Slider(
                                            value = studioHeadlineSize,
                                            onValueChange = { studioHeadlineSize = it },
                                            valueRange = 24f..60f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = cardBgColor,
                                                activeTrackColor = cardBgColor,
                                                inactiveTrackColor = Color(0xFF3E4741)
                                            )
                                        )
                                    }
                                }

                                // 2. Body Specimen Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222824))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "BODY COPY · ${bodyFont.family.uppercase()}",
                                                style = typography.caption.copy(
                                                    color = cardBgColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Text(
                                                text = "${studioBodySize.toInt()} px",
                                                style = typography.value.copy(color = Color(0xFFC9CEC9), fontSize = 12.sp)
                                            )
                                        }

                                        BasicTextField(
                                            value = studioBodyText,
                                            onValueChange = { studioBodyText = it },
                                            textStyle = TextStyle(
                                                fontFamily = bodyFontFamily,
                                                fontSize = studioBodySize.sp,
                                                lineHeight = (studioBodySize * 1.4f).sp,
                                                color = Color(0xFFE2E7E2)
                                            ),
                                            cursorBrush = SolidColor(cardBgColor),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Slider(
                                            value = studioBodySize,
                                            onValueChange = { studioBodySize = it },
                                            valueRange = 12f..24f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = cardBgColor,
                                                activeTrackColor = cardBgColor,
                                                inactiveTrackColor = Color(0xFF3E4741)
                                            )
                                        )
                                    }
                                }

                                // Studio Export Actions (FP-5)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Copy Dual Embed
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFF2A312D))
                                            .clickable {
                                                if (companionFont != null) {
                                                    val snippet = FontPairingEngine.getDualEmbedSnippet(font, companionFont)
                                                    context.copyText(snippet)
                                                    message("Dual font embed & CSS copied")
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Code,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "COPY DUAL EMBED & CSS",
                                                style = typography.label.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            )
                                        }
                                    }

                                    // Save Dual Bundle (.zip)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(cardBgColor)
                                            .clickable {
                                                if (companionFont != null) {
                                                    val cleanA = font.family.replace(" ", "_")
                                                    val cleanB = companionFont.family.replace(" ", "_")
                                                    saveDualLauncher.launch("$cleanA-$cleanB-pairing.zip")
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Download,
                                                contentDescription = null,
                                                tint = cardTextColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "SAVE DUAL PAIRING (.ZIP)",
                                                style = typography.display.copy(
                                                    color = cardTextColor,
                                                    fontSize = 18.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom CTA: SAVE .TTF FILES (Shown when in Specimen tab)
        if (selectedTab == 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(cardBgColor)
                        .clickable { saveAllLauncher.launch("${font.family}-fonts.zip") }
                        .padding(horizontal = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SAVE .TTF FILES",
                        style = typography.display.copy(
                            color = cardTextColor,
                            fontSize = 22.sp,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleSpecimenRow(
    font: FontInfo,
    style: FontStyle,
    previewText: String,
    fontSize: Float,
    loader: FontLoader,
    onSaveStyle: () -> Unit,
) {
    var styleTypeface by remember(font, style) { mutableStateOf(loader.peek(font.family, style)) }
    LaunchedEffect(font, style) {
        if (styleTypeface == null) styleTypeface = loader.load(font.family, style)
    }
    val fallbackFamily = remember(font.category) {
        when (font.category) {
            FontCategory.Serif -> FontFamily.Serif
            FontCategory.Monospace -> FontFamily.Monospace
            FontCategory.Handwriting -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        }
    }
    val styleFontFamily = remember(styleTypeface) {
        styleTypeface?.let { FontFamily(it) } ?: fallbackFamily
    }

    val weightName = WeightNames[style.weight] ?: "${style.weight}"
    val label = "$weightName${if (style.italic) " Italic" else ""} · ${style.weight}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        HorizontalDivider(color = Color(0xFF343C37), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC9CEC9)
                )
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A312D))
                    .clickable { onSaveStyle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Save $label as .ttf",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = previewText,
            fontFamily = styleFontFamily,
            fontWeight = if (styleTypeface != null) FontWeight.Normal else FontWeight(style.weight),
            fontStyle = if (styleTypeface != null) ComposeFontStyle.Normal else (if (style.italic) ComposeFontStyle.Italic else ComposeFontStyle.Normal),
            fontSize = fontSize.coerceIn(16f, 38f).sp,
            lineHeight = (fontSize.coerceIn(16f, 38f) * 1.25f).sp,
            color = Color.White
        )
    }
}
