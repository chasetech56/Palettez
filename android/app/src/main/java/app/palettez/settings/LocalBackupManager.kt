package app.palettez.settings

import android.content.Context
import app.palettez.palette.RecentPalette
import app.palettez.palette.RecentPalettesRepository
import app.palettez.palette.Swatch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersion: String,
    val settings: AppSettings,
    val gridSpecJson: String?,
    val recentPalettes: List<RecentPalette>,
)

data class RestoreSummary(
    val palettesRestored: Int,
    val settingsRestored: Boolean,
    val gridRestored: Boolean,
)

/**
 * Local-first JSON backup & restore engine (zero cloud, zero storage permissions via SAF).
 */
object LocalBackupManager {

    const val FORMAT_ID = "palettez-backup"
    const val SCHEMA_VERSION = 1

    fun serializeBackup(
        settings: AppSettings,
        recentPalettes: List<RecentPalette>,
        gridSpecJson: String?,
        appVersion: String = "1.1.0",
        exportedAt: Long = System.currentTimeMillis(),
    ): String {
        val root = JSONObject().apply {
            put("format", FORMAT_ID)
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", exportedAt)
            put("appVersion", appVersion)

            put("settings", JSONObject().apply {
                put("theme", settings.theme.id)
                put("defaultColorCount", settings.defaultColorCount)
                put("copyFormat", settings.copyFormat.label)
                put("rememberGrid", settings.rememberGrid)
                put("refreshFonts", settings.refreshFonts)
                put("newWindowDays", settings.newWindowDays)
                put("onboardingDone", settings.onboardingDone)
            })

            if (!gridSpecJson.isNullOrBlank()) {
                runCatching { JSONObject(gridSpecJson) }.getOrNull()?.let {
                    put("gridSpec", it)
                }
            }

            put("recentPalettes", JSONArray().apply {
                recentPalettes.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("timestamp", p.timestamp)
                        put("swatches", JSONArray().apply {
                            p.swatches.forEach { s ->
                                put(JSONObject().apply {
                                    put("r", s.r)
                                    put("g", s.g)
                                    put("b", s.b)
                                    put("share", s.share.toDouble())
                                })
                            }
                        })
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun parseBackup(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("format") != FORMAT_ID) return null
            val schemaVersion = root.optInt("schemaVersion", 0)
            if (schemaVersion <= 0) return null

            val sObj = root.optJSONObject("settings")
            val settings = if (sObj != null) {
                AppSettings(
                    theme = AppTheme.fromId(sObj.optString("theme", AppTheme.System.id)),
                    defaultColorCount = sObj.optInt("defaultColorCount", 6).coerceIn(2, 12),
                    copyFormat = CopyFormat.fromLabel(sObj.optString("copyFormat", CopyFormat.HEX.label)),
                    rememberGrid = sObj.optBoolean("rememberGrid", true),
                    refreshFonts = sObj.optBoolean("refreshFonts", true),
                    newWindowDays = sObj.optInt("newWindowDays", 60),
                    onboardingDone = sObj.optBoolean("onboardingDone", true),
                )
            } else {
                AppSettings(onboardingDone = true)
            }

            val gridSpecJson = root.optJSONObject("gridSpec")?.toString()

            val palettes = buildList {
                val pArr = root.optJSONArray("recentPalettes")
                if (pArr != null) {
                    for (i in 0 until pArr.length()) {
                        val pObj = pArr.optJSONObject(i) ?: continue
                        val id = pObj.optString("id", "").trim()
                        if (id.isEmpty()) continue
                        val name = pObj.optString("name", "Image Palette")
                        val timestamp = pObj.optLong("timestamp", System.currentTimeMillis())
                        val swArr = pObj.optJSONArray("swatches") ?: continue
                        val swatches = buildList {
                            for (j in 0 until swArr.length()) {
                                val swObj = swArr.optJSONObject(j) ?: continue
                                add(
                                    Swatch(
                                        r = swObj.optInt("r", 0).coerceIn(0, 255),
                                        g = swObj.optInt("g", 0).coerceIn(0, 255),
                                        b = swObj.optInt("b", 0).coerceIn(0, 255),
                                        share = swObj.optDouble("share", 0.0).toFloat().coerceIn(0f, 1f),
                                    )
                                )
                            }
                        }
                        if (swatches.isNotEmpty()) {
                            add(RecentPalette(id = id, name = name, timestamp = timestamp, swatches = swatches))
                        }
                    }
                }
            }

            BackupPayload(
                schemaVersion = schemaVersion,
                exportedAt = root.optLong("exportedAt", 0L),
                appVersion = root.optString("appVersion", "1.0.0"),
                settings = settings,
                gridSpecJson = gridSpecJson,
                recentPalettes = palettes,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun exportBackupJson(context: Context, settings: AppSettings): String {
        val recentRepo = RecentPalettesRepository(context.filesDir)
        val gridPrefs = context.getSharedPreferences("grid", Context.MODE_PRIVATE)
        val gridJson = gridPrefs.getString("spec", null)
        return serializeBackup(
            settings = settings,
            recentPalettes = recentRepo.getAll(),
            gridSpecJson = gridJson,
        )
    }

    suspend fun restoreBackupJson(
        context: Context,
        settingsRepo: SettingsRepository,
        jsonString: String,
    ): RestoreSummary? {
        val payload = parseBackup(jsonString) ?: return null

        // 1. Restore settings
        settingsRepo.setTheme(payload.settings.theme)
        settingsRepo.setDefaultColorCount(payload.settings.defaultColorCount)
        settingsRepo.setCopyFormat(payload.settings.copyFormat)
        settingsRepo.setRememberGrid(payload.settings.rememberGrid)
        settingsRepo.setRefreshFonts(payload.settings.refreshFonts)
        settingsRepo.setNewWindowDays(payload.settings.newWindowDays)
        settingsRepo.setOnboardingDone(true)

        // 2. Restore grid spec if present
        var gridRestored = false
        if (!payload.gridSpecJson.isNullOrBlank()) {
            context.getSharedPreferences("grid", Context.MODE_PRIVATE)
                .edit()
                .putString("spec", payload.gridSpecJson)
                .apply()
            gridRestored = true
        }

        // 3. Merge recent palettes
        val recentRepo = RecentPalettesRepository(context.filesDir)
        val restoredCount = recentRepo.mergePalettes(payload.recentPalettes)

        return RestoreSummary(
            palettesRestored = restoredCount,
            settingsRestored = true,
            gridRestored = gridRestored,
        )
    }
}
