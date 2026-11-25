package app.palettez.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

enum class AppTheme(val id: String, val label: String) {
    Light("light", "Light"),
    Dark("dark", "Dark"),
    System("system", "System");

    companion object {
        fun fromId(id: String?): AppTheme = entries.firstOrNull { it.id == id } ?: System
    }
}

enum class CopyFormat(val label: String) {
    HEX("HEX"),
    RGB("RGB"),
    CMYK("CMYK"),
    HSL("HSL");

    companion object {
        fun fromLabel(label: String?): CopyFormat = entries.firstOrNull { it.label == label } ?: HEX
    }
}

data class AppSettings(
    val theme: AppTheme = AppTheme.System,
    val defaultColorCount: Int = 6,
    val copyFormat: CopyFormat = CopyFormat.HEX,
    val rememberGrid: Boolean = true,
    val refreshFonts: Boolean = true,
    val newWindowDays: Int = 60,
    val onboardingDone: Boolean = false,
)

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "palettez_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_COLOR_COUNT = intPreferencesKey("default_color_count")
        val COPY_FORMAT = stringPreferencesKey("copy_format")
        val REMEMBER_GRID = booleanPreferencesKey("remember_grid")
        val REFRESH_FONTS = booleanPreferencesKey("refresh_fonts")
        val NEW_WINDOW_DAYS = intPreferencesKey("new_window_days")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            theme = AppTheme.fromId(prefs[Keys.THEME]),
            defaultColorCount = (prefs[Keys.DEFAULT_COLOR_COUNT] ?: 6).coerceIn(2, 12),
            copyFormat = CopyFormat.fromLabel(prefs[Keys.COPY_FORMAT]),
            rememberGrid = prefs[Keys.REMEMBER_GRID] ?: true,
            refreshFonts = prefs[Keys.REFRESH_FONTS] ?: true,
            newWindowDays = prefs[Keys.NEW_WINDOW_DAYS] ?: 60,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { it[Keys.THEME] = theme.id }
    }

    suspend fun setDefaultColorCount(count: Int) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_COLOR_COUNT] = count.coerceIn(2, 12) }
    }

    suspend fun setCopyFormat(format: CopyFormat) {
        context.settingsDataStore.edit { it[Keys.COPY_FORMAT] = format.label }
    }

    suspend fun setRememberGrid(remember: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMEMBER_GRID] = remember }
    }

    suspend fun setRefreshFonts(refresh: Boolean) {
        context.settingsDataStore.edit { it[Keys.REFRESH_FONTS] = refresh }
    }

    suspend fun setNewWindowDays(days: Int) {
        context.settingsDataStore.edit { it[Keys.NEW_WINDOW_DAYS] = days }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    fun getFontCacheSizeBytes(): Long {
        val dir = File(context.cacheDir, "google-fonts")
        return if (!dir.exists()) 0L else dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearFontCache() {
        val dir = File(context.cacheDir, "google-fonts")
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
}
