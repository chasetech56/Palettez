@file:OptIn(ExperimentalLayoutApi::class)

package app.palettez.palette

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.settings.CopyFormat
import app.palettez.ui.components.ChoiceChip
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.ModeSwitch
import app.palettez.ui.components.ScreenHeader
import app.palettez.ui.components.SwatchCard
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.PreviewRadius
import app.palettez.ui.theme.ScreenPadding
import app.palettez.util.LocalMessenger
import app.palettez.util.copyText
import app.palettez.util.writePng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PaletteScreen(
    vm: PaletteViewModel,
    copyFormat: CopyFormat = CopyFormat.HEX,
    onOpenSettings: () -> Unit = {},
    onSubscreenChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    var modeIndex by remember { mutableIntStateOf(0) } // 0 = From image, 1 = Presets
    var selectedPreset by remember { mutableStateOf<app.palettez.color.PresetPalette?>(null) }
    var showPresentationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPreset) {
        onSubscreenChanged(selectedPreset != null)
    }

    val activePreset = selectedPreset
    if (activePreset != null) {
        ColorSystemScreen(
            initialPalette = activePreset,
            onBack = { selectedPreset = null },
            modifier = modifier,
        )
        return
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.load(it) }
    }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraUri?.let { vm.load(it) }
        }
    }
    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) scope.launch {
            val bmp = withContext(Dispatchers.Default) { vm.renderPng() }
            message(if (context.writePng(uri, bmp)) "Palette saved" else "Couldn't save the file")
        }
    }

    LaunchedEffect(vm.error) { vm.error?.let(message) }

    fun copy(text: String, what: String = text) {
        context.copyText(text)
        message("Copied $what")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(top = 16.dp, bottom = 100.dp), // Clear floating tab bar
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Screen Header
        ScreenHeader(
            title = "PALETTE",
            subtitle = if (modeIndex == 0) "Palettez" else "Color systems",
            actions = {
                IconCircleButton(
                    icon = Icons.Outlined.Tune,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                )
                IconCircleButton(
                    icon = Icons.Outlined.PhotoCamera,
                    contentDescription = "Take photo",
                    onClick = {
                        try {
                            val cameraFile = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                cameraFile
                            )
                            cameraUri = uri
                            takePhoto.launch(uri)
                        } catch (e: ActivityNotFoundException) {
                            message("No camera app found")
                        } catch (e: Exception) {
                            message("Could not launch camera: ${e.message}")
                        }
                    },
                )
            },
        )

        // 2. Mode Switch: "From image" | "Presets"
        ModeSwitch(
            options = listOf("From image", "Presets"),
            selectedIndex = modeIndex,
            onSelect = { modeIndex = it },
        )

        if (modeIndex == 0) {
            // ---- Mode 0: From Image ----

            // 3. Image Stage Card
            val img = vm.image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(PreviewRadius))
                    .background(colors.surface),
            ) {
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colors.ink,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Pick a photo or mood image",
                            style = typography.headline.copy(fontSize = 22.sp),
                            color = colors.ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Images stay on this device",
                            style = typography.caption,
                            color = colors.inkMuted,
                        )
                    }
                }

                if (vm.busy) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = colors.lime,
                    )
                }

                // Bottom overlay pill inside the stage card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (colors.isDark) Color(0xFF1D2320).copy(alpha = 0.92f) else colors.surface.copy(alpha = 0.92f))
                            .then(
                                if (colors.isDark) Modifier.border(1.dp, colors.line, RoundedCornerShape(50))
                                else Modifier
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = if (img != null) "image.jpg · ${vm.swatches.size} colors" else "No image selected",
                            style = typography.label,
                            color = colors.ink,
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.ink)
                            .clickable {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Pick image",
                            tint = colors.onInk,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // 4. Color Strip Bar (proportional swatches)
            if (vm.swatches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    vm.swatches.forEach { s ->
                        Box(
                            modifier = Modifier
                                .weight(s.share.coerceAtLeast(0.04f))
                                .fillMaxSize()
                                .background(Color(s.argb)),
                        )
                    }
                }
            }

            // 5. Sort Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SwatchOrder.entries.forEach { order ->
                    ChoiceChip(
                        label = order.label,
                        selected = vm.order == order,
                        onClick = { vm.updateOrder(order) },
                    )
                }
            }

            // 6. Section Header: "SWATCHES" + "Copy all HEX"
            if (vm.swatches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SWATCHES",
                        style = typography.section,
                        color = colors.ink,
                    )
                    Text(
                        text = "Copy all HEX",
                        style = typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.ink,
                        ),
                        modifier = Modifier.clickable {
                            copy(vm.hexList(), "all HEX codes")
                        },
                    )
                }

                // 7. Swatch Cards
                vm.swatches.forEach { swatch ->
                    SwatchCard(
                        swatch = swatch,
                        onCopy = { value, label -> copy(value, label) },
                        onCardClick = {
                            val value = when (copyFormat) {
                                CopyFormat.HEX -> swatch.hex
                                CopyFormat.RGB -> swatch.rgb
                                CopyFormat.CMYK -> swatch.cmyk
                                CopyFormat.HSL -> swatch.hsl
                            }
                            copy(value, "${copyFormat.label} ($value)")
                        },
                    )
                }

                // Quick export action chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    ChoiceChip(
                        label = "Color system",
                        selected = false,
                        onClick = {
                            selectedPreset = app.palettez.color.PresetPalette(
                                name = "Extracted Image",
                                mood = "Custom",
                                colors = vm.swatches.map { it.hex },
                                isMine = true,
                            )
                        },
                    )
                    ChoiceChip(
                        label = "CSS vars",
                        selected = false,
                        onClick = { copy(vm.cssVars(), "CSS variables") },
                    )
                    ChoiceChip(
                        label = "Full list",
                        selected = false,
                        onClick = { copy(vm.fullList(), "full list") },
                    )
                    ChoiceChip(
                        label = "Save PNG",
                        selected = false,
                        onClick = { showPresentationDialog = true },
                    )
                }

                Text(
                    text = "CMYK uses the standard device-independent formula. For exact print color, check against your printer's profile.",
                    style = typography.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // 8. Recents Section
            if (vm.recents.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "RECENTS",
                        style = typography.section,
                        color = colors.ink,
                    )
                    Text(
                        text = "Clear all",
                        style = typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.inkMuted,
                        ),
                        modifier = Modifier.clickable {
                            vm.clearRecents()
                            message("Recents cleared")
                        },
                    )
                }

                vm.recents.forEach { recent ->
                    RecentPaletteCard(
                        recent = recent,
                        onClick = {
                            vm.selectRecent(recent)
                            message("Restored ${recent.name} (${recent.swatches.size} colors)")
                        },
                        onDelete = {
                            vm.deleteRecent(recent.id)
                            message("Removed from recents")
                        },
                        onOpenColorSystem = {
                            selectedPreset = app.palettez.color.PresetPalette(
                                name = recent.name,
                                mood = "Custom",
                                colors = recent.swatches.map { it.hex },
                                isMine = true,
                            )
                        },
                    )
                }
            }
        } else {
            // ---- Mode 1: Presets ----
            PresetsSection(
                onSelectPalette = { selectedPreset = it },
                onFromPhotoClick = { modeIndex = 0 },
            )
        }
    }

    if (showPresentationDialog && vm.swatches.isNotEmpty()) {
        ColorPresentationDialog(
            swatches = vm.swatches,
            thumbnailBitmap = vm.thumbnailBitmap,
            defaultTitle = if (vm.image != null) "Photo Palette" else "Brand Palette",
            onDismiss = { showPresentationDialog = false }
        )
    }
}
