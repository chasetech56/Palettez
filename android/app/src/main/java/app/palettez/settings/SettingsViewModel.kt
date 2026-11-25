package app.palettez.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    val repository = SettingsRepository(app)

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    var cacheText by mutableStateOf(formatCacheSize(repository.getFontCacheSizeBytes()))
        private set

    fun refreshCacheSize() {
        cacheText = formatCacheSize(repository.getFontCacheSizeBytes())
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setDefaultColorCount(count: Int) {
        viewModelScope.launch { repository.setDefaultColorCount(count) }
    }

    fun setCopyFormat(format: CopyFormat) {
        viewModelScope.launch { repository.setCopyFormat(format) }
    }

    fun setRememberGrid(remember: Boolean) {
        viewModelScope.launch { repository.setRememberGrid(remember) }
    }

    fun setRefreshFonts(refresh: Boolean) {
        viewModelScope.launch { repository.setRefreshFonts(refresh) }
    }

    fun setNewWindowDays(days: Int) {
        viewModelScope.launch { repository.setNewWindowDays(days) }
    }

    fun clearCache() {
        repository.clearFontCache()
        cacheText = "No fonts saved"
    }

    private fun formatCacheSize(bytes: Long): String {
        if (bytes <= 0L) return "No fonts saved"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB on this phone", mb)
        } else {
            String.format(Locale.US, "%.0f KB on this phone", kb)
        }
    }
}
