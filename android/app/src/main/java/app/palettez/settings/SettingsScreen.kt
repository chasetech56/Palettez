package app.palettez.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.theme.PalettezTheme
import app.palettez.ui.theme.ScreenPadding
import app.palettez.util.LocalMessenger

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onReplayIntro: () -> Unit,
    onOpenWhatsNew: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onBackupRestored: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = LocalMessenger.current
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var showLicensesDialog by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = LocalBackupManager.exportBackupJson(context, settings)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: return@runCatching false
                    true
                }.getOrDefault(false)
            }
            message(if (ok) "Backup saved to device" else "Couldn't write backup file")
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val summary = withContext(Dispatchers.IO) {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: return@runCatching null
                    LocalBackupManager.restoreBackupJson(context, vm.repository, json)
                }.getOrNull()
            }
            if (summary != null) {
                onBackupRestored()
                val msg = if (summary.palettesRestored > 0) {
                    "Restored settings & ${summary.palettesRestored} palette(s)"
                } else {
                    "Restored settings & grid"
                }
                message(msg)
            } else {
                message("Invalid or unreadable Palettez backup file")
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.refreshCacheSize()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        IconCircleButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )

        Text(
            text = "SETTINGS",
            style = typography.display.copy(fontSize = 52.sp, lineHeight = 56.sp),
            color = colors.ink,
        )

        // 1. APPEARANCE
        Text(
            text = "APPEARANCE",
            style = typography.section,
            color = colors.ink,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Theme",
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    color = colors.ink,
                )
                // Segmented Theme Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.ground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppTheme.entries.forEach { themeOption ->
                        val selected = settings.theme == themeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (selected) colors.ink else Color.Transparent)
                                .clickable { vm.setTheme(themeOption) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = themeOption.label,
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = if (selected) colors.onInk else colors.ink,
                            )
                        }
                    }
                }
            }
        }

        // 2. PALETTE
        Text(
            text = "PALETTE",
            style = typography.section,
            color = colors.ink,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column {
                // Colors to find stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Colors to find",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                            color = colors.ink,
                        )
                        Text(
                            text = "Starting number for each image",
                            style = typography.caption.copy(fontSize = 13.sp),
                            color = colors.inkMuted,
                        )
                    }

                    // Minus button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, colors.line, CircleShape)
                            .clickable {
                                if (settings.defaultColorCount > 2) {
                                    vm.setDefaultColorCount(settings.defaultColorCount - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Fewer colors",
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = settings.defaultColorCount.toString(),
                        style = typography.section.copy(fontSize = 26.sp),
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )

                    // Plus button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.ink)
                            .clickable {
                                if (settings.defaultColorCount < 12) {
                                    vm.setDefaultColorCount(settings.defaultColorCount + 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "More colors",
                            tint = colors.surface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Tapping a swatch copies
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Tapping a swatch copies",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CopyFormat.entries.forEach { format ->
                            val selected = settings.copyFormat == format
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (selected) colors.ink else Color.Transparent)
                                    .border(
                                        width = if (selected) 0.dp else 1.5.dp,
                                        color = if (selected) Color.Transparent else colors.line,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .clickable { vm.setCopyFormat(format) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = format.label,
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                    color = if (selected) colors.onInk else colors.ink,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. GRID & FONTS
        Text(
            text = "GRID & FONTS",
            style = typography.section,
            color = colors.ink,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column {
                // Toggle 1: Remember my last grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Remember my last grid",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                            color = colors.ink,
                        )
                        Text(
                            text = "Reopen the Grid tab with your last settings",
                            style = typography.caption.copy(fontSize = 13.sp),
                            color = colors.inkMuted,
                        )
                    }
                    PalettezSwitch(
                        checked = settings.rememberGrid,
                        onCheckedChange = { vm.setRememberGrid(it) }
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Toggle 2: Update font list on open
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update font list on open",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                            color = colors.ink,
                        )
                        Text(
                            text = "Checks Google Fonts for new families",
                            style = typography.caption.copy(fontSize = 13.sp),
                            color = colors.inkMuted,
                        )
                    }
                    PalettezSwitch(
                        checked = settings.refreshFonts,
                        onCheckedChange = { vm.setRefreshFonts(it) }
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Show the NEW tag for
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Show the NEW tag for",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 90).forEach { days ->
                            val selected = settings.newWindowDays == days
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (selected) colors.ink else Color.Transparent)
                                    .border(
                                        width = if (selected) 0.dp else 1.5.dp,
                                        color = if (selected) Color.Transparent else colors.line,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .clickable { vm.setNewWindowDays(days) }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$days days",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                    color = if (selected) colors.onInk else colors.ink,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Saved fonts row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved fonts",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                            color = colors.ink,
                        )
                        Text(
                            text = vm.cacheText,
                            style = typography.caption.copy(fontSize = 13.sp),
                            color = colors.inkMuted,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.5.dp, colors.line, RoundedCornerShape(22.dp))
                            .clickable {
                                vm.clearCache()
                                message("Saved fonts cleared")
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = colors.ink,
                        )
                    }
                }
            }
        }

        // 3.5 BACKUP & RESTORE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "BACKUP & RESTORE",
                style = typography.section.copy(fontSize = 20.sp),
                color = colors.ink,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export your saved palettes, grid settings, and preferences as a local JSON file, or restore from an existing backup.",
                    style = typography.caption.copy(color = colors.inkSecondary, fontSize = 13.sp, lineHeight = 18.sp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.ink)
                            .clickable {
                                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                exportBackupLauncher.launch("palettez-backup-$stamp.json")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Export backup",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = colors.onInk,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.5.dp, colors.line, RoundedCornerShape(24.dp))
                            .clickable {
                                restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Restore",
                            style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = colors.ink,
                        )
                    }
                }
            }
        }

        // 4. PRIVACY CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.deep)
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.lime),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.deep,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PRIVACY",
                        style = typography.section,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your images stay on this phone. The app only connects to Google Fonts, to get the font list and font files. No account, no tracking.",
                        style = typography.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = Color(0xFFDADFDA),
                    )
                }
            }
        }

        // 5. ABOUT
        Text(
            text = "ABOUT",
            style = typography.section,
            color = colors.ink,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column {
                // Replay the intro
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onReplayIntro() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replay the intro",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.ink
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Font licenses
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fonts.google.com/attribution"))
                            context.startActivity(intent)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Font licenses",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Open-source licenses
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { showLicensesDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open-source licenses",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.ink
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // What's new in Palettez (WN-5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onOpenWhatsNew() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What's new in Palettez",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.lime)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v1.2",
                            style = typography.caption.copy(color = Color(0xFF151515), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.ink
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Check for updates (WN-7)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onCheckForUpdates() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Check for updates",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "chasetech56.github.io",
                        style = typography.caption.copy(color = colors.inkSecondary, fontSize = 12.sp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.ink
                    )
                }

                HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                // Version
                val appVersion = remember {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val code = PackageInfoCompat.getLongVersionCode(pInfo)
                        "${pInfo.versionName} (Build $code)"
                    } catch (e: Exception) {
                        "1.1.0 (Build 2)"
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = appVersion,
                        style = typography.body.copy(fontSize = 15.sp),
                        color = colors.inkMuted
                    )
                }
            }
        }
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@Composable
fun PalettezSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PalettezTheme.colors
    val offset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (checked) colors.ink else colors.line)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = offset - 4.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) colors.lime else Color.White)
        )
    }
}

@Composable
private fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "OPEN-SOURCE LICENSES",
                    style = typography.section.copy(fontSize = 24.sp),
                    color = colors.ink,
                )

                Text(
                    text = """
Palettez is open source under the Apache License 2.0.

Bundled Typefaces:
• Anton — SIL Open Font License 1.1 (Vernon Adams)
• Figtree — SIL Open Font License 1.1 (Erik Kennedy)
• JetBrains Mono — SIL Open Font License 1.1 (JetBrains s.r.o.)

Open-Source Libraries:
• AndroidX Jetpack (Compose, Material 3, Lifecycle, Navigation, Room, DataStore) — Apache License 2.0
• OkHttp — Apache License 2.0 (Square, Inc.)
• Coil — Apache License 2.0 (Coil Contributors)
• Kotlin & Kotlinx Coroutines — Apache License 2.0 (JetBrains s.r.o.)
                    """.trimIndent(),
                    style = typography.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = colors.inkMuted
                )

                PrimaryButton(
                    label = "CLOSE",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
