package app.palettez.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.pm.PackageInfoCompat
import app.palettez.fonts.FontsScreen
import app.palettez.fonts.FontsViewModel
import app.palettez.grid.GridScreen
import app.palettez.grid.GridViewModel
import app.palettez.onboarding.OnboardingScreen
import app.palettez.palette.PaletteScreen
import app.palettez.palette.PaletteViewModel
import app.palettez.settings.SettingsScreen
import app.palettez.settings.SettingsViewModel
import app.palettez.ui.components.FloatingTabBar
import app.palettez.ui.components.PalettezTab
import app.palettez.ui.components.PalettezToastHost
import app.palettez.ui.components.PalettezToastHostState
import app.palettez.ui.theme.PalettezTheme
import app.palettez.update.RemoteReleaseInfo
import app.palettez.update.UpdateAvailableDialog
import app.palettez.update.UpdateChecker
import app.palettez.update.WhatsNewDialog
import app.palettez.update.WhatsNewRegistry
import app.palettez.util.LocalMessenger
import kotlinx.coroutines.launch

@Composable
fun PalettezApp(
    paletteViewModel: PaletteViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val gridViewModel: GridViewModel = viewModel()
    val fontsViewModel: FontsViewModel = viewModel()
    val settings by settingsViewModel.settings.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(PalettezTab.Palette) }
    var isSubscreenActive by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showReplayIntro by rememberSaveable { mutableStateOf(false) }
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<RemoteReleaseInfo?>(null) }
    val toastState = remember { PalettezToastHostState() }
    val scope = rememberCoroutineScope()

    fun getCurrentVersionCode(): Int = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        PackageInfoCompat.getLongVersionCode(pInfo).toInt()
    } catch (_: Exception) {
        WhatsNewRegistry.getLatestRelease().versionCode
    }

    // Automatic local changelog + remote homepage update detection (WN-1, WN-7)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("palettez_updates", Context.MODE_PRIVATE)
        val lastSeenVersion = prefs.getInt("last_seen_version_code", 0)
        val currentVersionCode = getCurrentVersionCode()

        if (lastSeenVersion < currentVersionCode) {
            showWhatsNew = true
        }

        val dismissedRemote = prefs.getInt("dismissed_remote_version_code", 0)
        val remote = UpdateChecker.checkForUpdate(currentVersionCode)
        if (remote != null && remote.versionCode > dismissedRemote) {
            availableUpdate = remote
        }
    }

    val messenger: (String) -> Unit = { msg ->
        scope.launch {
            toastState.show(msg)
        }
    }

    // Shared images jump to the Palette tab
    LaunchedEffect(paletteViewModel.shareCounter) {
        if (paletteViewModel.shareCounter > 0) {
            currentTab = PalettezTab.Palette
            showSettings = false
            showReplayIntro = false
        }
    }

    // Sync settings with fontsViewModel
    LaunchedEffect(settings.refreshFonts, settings.newWindowDays) {
        fontsViewModel.applySettings(settings.refreshFonts, settings.newWindowDays)
    }

    CompositionLocalProvider(LocalMessenger provides messenger) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PalettezTheme.colors.ground)
        ) {
            if (!settings.onboardingDone) {
                // Initial Onboarding Flow (ON-1, ON-2, ON-3, ON-4)
                OnboardingScreen(
                    currentTheme = settings.theme,
                    onThemeSelected = { settingsViewModel.setTheme(it) },
                    onComplete = {
                        scope.launch { settingsViewModel.repository.setOnboardingDone(true) }
                    }
                )
            } else if (showReplayIntro) {
                // Replaying intro from Settings (ON-5: Replay ends back on Settings)
                OnboardingScreen(
                    isReplay = true,
                    currentTheme = settings.theme,
                    onThemeSelected = { settingsViewModel.setTheme(it) },
                    onComplete = {
                        showReplayIntro = false
                        showSettings = true
                    }
                )
            } else if (showSettings) {
                // Settings Screen (SE-1 .. SE-7)
                SettingsScreen(
                    vm = settingsViewModel,
                    onBack = { showSettings = false },
                    onReplayIntro = {
                        showSettings = false
                        showReplayIntro = true
                    },
                    onOpenWhatsNew = {
                        showWhatsNew = true
                    },
                    onCheckForUpdates = {
                        scope.launch {
                            toastState.show("Checking for updates…")
                            val remote = UpdateChecker.checkForUpdate(getCurrentVersionCode())
                            if (remote != null) {
                                availableUpdate = remote
                            } else {
                                toastState.show("You're on the latest build!")
                            }
                        }
                    }
                )
            } else {
                // Normal 3-Tab Main App
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentTab) {
                        PalettezTab.Palette -> PaletteScreen(
                            paletteViewModel,
                            copyFormat = settings.copyFormat,
                            onOpenSettings = { showSettings = true },
                            onSubscreenChanged = { isSubscreenActive = it }
                        )
                        PalettezTab.Grid -> GridScreen(
                            gridViewModel,
                            paletteColors = paletteViewModel.swatches.map { it.argb },
                            onSubscreenChanged = { isSubscreenActive = it }
                        )
                        PalettezTab.Fonts -> FontsScreen(
                            fontsViewModel,
                            onSubscreenChanged = { isSubscreenActive = it }
                        )
                    }
                }

                // Floating Pill Tab Bar (only shown on root screens)
                if (!isSubscreenActive) {
                    FloatingTabBar(
                        selectedTab = currentTab,
                        onTabSelect = { currentTab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Toast floating above bottom area
            PalettezToastHost(
                hostState = toastState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isSubscreenActive || showSettings || showReplayIntro || !settings.onboardingDone) 28.dp else 100.dp)
            )

            // What's New Dialog (WN-1 .. WN-6)
            if (showWhatsNew) {
                WhatsNewDialog(
                    releaseNote = WhatsNewRegistry.getLatestRelease(),
                    onDismiss = {
                        showWhatsNew = false
                        val prefs = context.getSharedPreferences("palettez_updates", Context.MODE_PRIVATE)
                        prefs.edit().putInt("last_seen_version_code", getCurrentVersionCode()).apply()
                    }
                )
            } else if (availableUpdate != null && settings.onboardingDone) {
                val remote = availableUpdate!!
                UpdateAvailableDialog(
                    remote = remote,
                    onDownloadClick = {
                        val prefs = context.getSharedPreferences("palettez_updates", Context.MODE_PRIVATE)
                        prefs.edit().putInt("dismissed_remote_version_code", remote.versionCode).apply()
                        availableUpdate = null
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(remote.homepageUrl)))
                        }
                    },
                    onDismiss = {
                        val prefs = context.getSharedPreferences("palettez_updates", Context.MODE_PRIVATE)
                        prefs.edit().putInt("dismissed_remote_version_code", remote.versionCode).apply()
                        availableUpdate = null
                    }
                )
            }
        }
    }
}
