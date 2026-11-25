package app.palettez.palette

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.ModeSwitch
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.theme.PalettezTheme
import app.palettez.util.LocalMessenger
import app.palettez.util.shareBitmap
import app.palettez.util.writePng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ColorPresentationDialog(
    swatches: List<Swatch>,
    thumbnailBitmap: Bitmap? = null,
    defaultTitle: String = "Brand Palette",
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    var aspectRatio by remember { mutableStateOf(PresentationAspectRatio.Ratio16x9) }
    var isDarkCard by remember { mutableStateOf(colors.isDark) }
    var projectTitle by remember { mutableStateOf(defaultTitle) }
    var showCmykAndOklab by remember { mutableStateOf(true) }
    var showContrastBadge by remember { mutableStateOf(true) }

    // Rendered high-res bitmap state
    val renderedBitmap by produceState<Bitmap?>(initialValue = null, aspectRatio, isDarkCard, projectTitle, showCmykAndOklab, showContrastBadge, swatches) {
        value = withContext(Dispatchers.Default) {
            ColorPresentationRenderer.render(
                context = context,
                swatches = swatches,
                aspectRatio = aspectRatio,
                projectName = projectTitle.ifBlank { "Brand Palette" },
                isDark = isDarkCard,
                showCmykAndOklab = showCmykAndOklab,
                showContrastBadge = showContrastBadge,
                thumbnailBitmap = thumbnailBitmap,
            )
        }
    }

    // SAF file saver
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null && renderedBitmap != null) {
            scope.launch {
                val success = context.writePng(uri, renderedBitmap!!)
                message(if (success) "High-res presentation card saved" else "Could not save image")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ground)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BRAND PRESENTATION",
                            style = typography.display.copy(fontSize = 32.sp, lineHeight = 36.sp),
                            color = colors.ink
                        )
                        Text(
                            text = "Export 16:9 slide or 1:1 brand card",
                            style = typography.caption,
                            color = colors.inkMuted
                        )
                    }
                    IconCircleButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Close presentation export",
                        onClick = onDismiss
                    )
                }

                // 2. Aspect Ratio Selector
                ModeSwitch(
                    options = listOf("16:9 Slide", "1:1 Square"),
                    selectedIndex = if (aspectRatio == PresentationAspectRatio.Ratio16x9) 0 else 1,
                    onSelect = {
                        aspectRatio = if (it == 0) PresentationAspectRatio.Ratio16x9 else PresentationAspectRatio.Ratio1x1
                    }
                )

                // 3. Live Card Preview
                val previewRatio = if (aspectRatio == PresentationAspectRatio.Ratio16x9) 16f / 9f else 1f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(previewRatio)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = renderedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Presentation card preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = colors.lime, modifier = Modifier.size(36.dp))
                    }
                }

                // 4. Customization Controls Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "CUSTOMIZE SPECIMEN",
                            style = typography.label.copy(fontWeight = FontWeight.Bold),
                            color = colors.ink
                        )

                        // Project Title Input
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Project Title",
                                style = typography.caption,
                                color = colors.inkMuted
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.ground)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = projectTitle,
                                    onValueChange = { projectTitle = it },
                                    textStyle = typography.body.copy(color = colors.ink, fontWeight = FontWeight.SemiBold),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colors.ink),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Theme Mode Pills: Light Ground vs Dark Ground
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ground Theme:",
                                style = typography.caption,
                                color = colors.inkMuted,
                                modifier = Modifier.width(90.dp)
                            )
                            ChoiceChip(
                                label = "Cream Light",
                                selected = !isDarkCard,
                                onClick = { isDarkCard = false }
                            )
                            ChoiceChip(
                                label = "Deep Dark",
                                selected = isDarkCard,
                                onClick = { isDarkCard = true }
                            )
                        }

                        // Toggle Chips: CMYK & OKLab, Contrast Badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ChoiceChip(
                                label = if (showCmykAndOklab) "✓ CMYK & OKLab" else "+ CMYK & OKLab",
                                selected = showCmykAndOklab,
                                onClick = { showCmykAndOklab = !showCmykAndOklab },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceChip(
                                label = if (showContrastBadge) "✓ WCAG Score" else "+ WCAG Score",
                                selected = showContrastBadge,
                                onClick = { showContrastBadge = !showContrastBadge },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. Action Buttons: Save High-Res PNG & Share
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        label = "SAVE ${aspectRatio.width}×${aspectRatio.height} PNG",
                        onClick = {
                            saveLauncher.launch("${projectTitle.lowercase().replace(" ", "_")}_brand_card.png")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    IconCircleButton(
                        icon = Icons.Filled.Share,
                        contentDescription = "Share brand card",
                        onClick = {
                            val bmp = renderedBitmap
                            if (bmp != null) {
                                scope.launch {
                                    val shared = context.shareBitmap(bmp, "palette_presentation.png")
                                    if (!shared) message("Could not share image")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
