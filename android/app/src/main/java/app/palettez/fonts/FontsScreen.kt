package app.palettez.fonts

import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.ScreenHeader
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme
import app.palettez.util.LocalMessenger

// Color blocks for font cards in Bold Edition
val FONT_CARD_BLOCKS = listOf(
    Pair(Color(0xFF6A58CC), Color(0xFFFFFFFF)), // Grape
    Pair(Color(0xFFF4C542), Color(0xFF151515)), // Mustard
    Pair(Color(0xFF4E7A5B), Color(0xFFFFFFFF)), // Sage
    Pair(Color(0xFFE2603F), Color(0xFF151515)), // Coral
    Pair(Color(0xFFAFCBEA), Color(0xFF151515)), // Sky
    Pair(Color(0xFFA7D86E), Color(0xFF151515)), // Lime
)

fun fontCardColors(family: String): Pair<Color, Color> {
    val index = kotlin.math.abs(family.hashCode()) % FONT_CARD_BLOCKS.size
    return FONT_CARD_BLOCKS[index]
}

// Categories for the top card strip
data class CategoryCardItem(
    val category: FontCategory,
    val label: String,
    val bgColor: Color,
    val textColor: Color,
    val glyphFamily: FontFamily,
)

val CATEGORY_ITEMS = listOf(
    CategoryCardItem(FontCategory.All, "All", Color(0xFF151515), Color(0xFFFFFFFF), FontFamily.Default),
    CategoryCardItem(FontCategory.SansSerif, "Sans", Color(0xFF6A58CC), Color(0xFFFFFFFF), FontFamily.SansSerif),
    CategoryCardItem(FontCategory.Serif, "Serif", Color(0xFFF4C542), Color(0xFF151515), FontFamily.Serif),
    CategoryCardItem(FontCategory.Display, "Display", Color(0xFFE2603F), Color(0xFF151515), FontFamily.Default),
    CategoryCardItem(FontCategory.Handwriting, "Handwriting", Color(0xFF4E7A5B), Color(0xFFFFFFFF), FontFamily.Cursive),
    CategoryCardItem(FontCategory.Monospace, "Mono", Color(0xFFAFCBEA), Color(0xFF151515), FontFamily.Monospace),
)

@Composable
fun FontsScreen(
    vm: FontsViewModel,
    onSubscreenChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val message = LocalMessenger.current
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    var selectedFont by remember { mutableStateOf<FontInfo?>(null) }

    BackHandler(enabled = selectedFont != null) {
        selectedFont = null
    }

    LaunchedEffect(selectedFont) {
        onSubscreenChanged(selectedFont != null)
    }

    if (selectedFont != null) {
        val (cardBg, cardText) = remember(selectedFont!!.family) {
            fontCardColors(selectedFont!!.family)
        }
        FontDetailScreen(
            font = selectedFont!!,
            loader = vm.loader,
            allFonts = vm.fonts,
            cardBgColor = cardBg,
            cardTextColor = cardText,
            previewText = vm.previewText,
            onSelectFont = { selectedFont = it },
            onBack = { selectedFont = null },
            modifier = modifier
        )
        return
    }

    val visibleFonts = remember(vm.fonts, vm.query, vm.category, vm.sort) {
        vm.visible()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Refresh button
        item {
            ScreenHeader(
                subtitle = "Google Fonts, free & open source",
                title = "FONTS",
                actions = {
                    IconCircleButton(
                        icon = Icons.Outlined.Refresh,
                        contentDescription = "Refresh font list",
                        onClick = {
                            vm.refresh(force = true)
                            message("Refreshing font list…")
                        }
                    )
                }
            )
        }

        // 2. Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surface)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = colors.inkMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (vm.query.isEmpty()) {
                            Text(
                                text = "Search 'serif', 'mono', a name…",
                                style = typography.body.copy(color = colors.inkMuted, fontSize = 15.sp)
                            )
                        }
                        BasicTextField(
                            value = vm.query,
                            onValueChange = { vm.query = it },
                            textStyle = typography.body.copy(color = colors.ink, fontSize = 15.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(colors.ink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Category Hero Cards Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CATEGORY_ITEMS.forEach { item ->
                    val isSelected = vm.category == item.category
                    Box(
                        modifier = Modifier
                            .size(width = 104.dp, height = 112.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(item.bgColor)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = colors.ink,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { vm.category = item.category }
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Aa",
                                fontFamily = item.glyphFamily,
                                fontSize = 34.sp,
                                lineHeight = 38.sp,
                                color = item.textColor
                            )
                            Text(
                                text = item.label,
                                style = typography.label.copy(
                                    color = item.textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Category Title & Sorting Pill Row
        item {
            val headingText = when (vm.category) {
                FontCategory.All -> "ALL FONTS"
                FontCategory.SansSerif -> "SANS SERIF"
                FontCategory.Serif -> "SERIF"
                FontCategory.Display -> "DISPLAY"
                FontCategory.Handwriting -> "HANDWRITING"
                FontCategory.Monospace -> "MONOSPACE"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headingText,
                    style = typography.display.copy(fontSize = 26.sp, letterSpacing = 0.3.sp, color = colors.ink)
                )

                // Sort switcher pill
                Row(
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(22.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sorts = listOf(
                        Triple(FontSort.Newest, "New", "New sorts by release date from live catalog"),
                        Triple(FontSort.Popular, "Popular", "Sorted by popularity"),
                        Triple(FontSort.Alpha, "A–Z", "Sorted alphabetically")
                    )
                    sorts.forEach { (sortOption, label, hint) ->
                        val isSelected = vm.sort == sortOption
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) colors.ink else Color.Transparent)
                                .clickable {
                                    vm.sort = sortOption
                                    message(hint)
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = typography.label.copy(
                                    color = if (isSelected) colors.onInk else colors.ink,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Preview Text Input
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, colors.line, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PREVIEW TEXT",
                        style = typography.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.inkMuted,
                            letterSpacing = 0.6.sp
                        )
                    )
                    BasicTextField(
                        value = vm.previewText,
                        onValueChange = { vm.previewText = it },
                        textStyle = typography.body.copy(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 6. Status Line
        item {
            Text(
                text = vm.status,
                style = typography.caption.copy(color = colors.inkMuted, fontSize = 12.sp),
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        // 7. Loading indicator or Empty State
        if (vm.loading && visibleFonts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.ink)
                }
            }
        } else if (visibleFonts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fonts match.",
                        style = typography.body.copy(color = colors.inkMuted, fontSize = 15.sp)
                    )
                }
            }
        } else {
            // 8. Font Cards List
            items(visibleFonts, key = { it.family }) { font ->
                val cardIndex = remember(font.family) {
                    kotlin.math.abs(font.family.hashCode()) % FONT_CARD_BLOCKS.size
                }
                val (bgColor, textColor) = FONT_CARD_BLOCKS[cardIndex]

                FontPreviewCard(
                    font = font,
                    previewText = vm.previewText.ifEmpty { "Sphinx of black quartz, judge my vow" },
                    bgColor = bgColor,
                    textColor = textColor,
                    loader = vm.loader,
                    onClick = { selectedFont = font }
                )
            }
        }
    }
}

@Composable
private fun FontPreviewCard(
    font: FontInfo,
    previewText: String,
    bgColor: Color,
    textColor: Color,
    loader: FontLoader,
    onClick: () -> Unit,
) {
    val typeface by produceState<Typeface?>(initialValue = loader.peek(font.family, font.previewStyle), font) {
        value = loader.load(font.family, font.previewStyle)
    }
    val fallbackFamily = remember(font.category) {
        when (font.category) {
            FontCategory.Serif -> FontFamily.Serif
            FontCategory.Monospace -> FontFamily.Monospace
            FontCategory.Handwriting -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        }
    }
    val fontFamily = remember(typeface) {
        typeface?.let { FontFamily(it) } ?: fallbackFamily
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(bgColor)
            .clickable { onClick() }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = font.family,
                fontFamily = fontFamily,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(textColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = font.category.label,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    )
                }

                // NEW tag if recent
                if (font.isNew) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFA7D86E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NEW",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF151515)
                            )
                        )
                    }
                }

                // Diagonal arrow icon button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NorthEast,
                        contentDescription = "View ${font.family}",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Live Font Specimen Preview Text
        Text(
            text = previewText,
            fontFamily = fontFamily,
            fontWeight = if (typeface != null) FontWeight.Normal else FontWeight(font.previewStyle.weight),
            fontStyle = if (typeface != null) androidx.compose.ui.text.font.FontStyle.Normal else (if (font.previewStyle.italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal),
            fontSize = 26.sp,
            lineHeight = 32.sp,
            color = textColor
        )
    }
}
