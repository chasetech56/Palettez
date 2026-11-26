package app.palettez.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RemoteReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val homepageUrl: String,
    val releaseNotes: List<String>,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: RemoteReleaseInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val reason: String) : UpdateCheckResult()
}

/**
 * Checks the static GitHub Pages manifest (`version.json`) over HTTPS (WN-7, WN-8).
 * Uses cache-busting + FORCE_NETWORK so GitHub Pages CDN never serves stale versions.
 * Carries zero user or device identifiers.
 */
object UpdateChecker {

    const val HOMEPAGE_URL = "https://chasetech56.github.io/Palettez/"
    const val VERSION_MANIFEST_URL = "https://chasetech56.github.io/Palettez/version.json"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /** Pure JSON parser for unit testing and manifest decoding. */
    fun parseManifest(jsonString: String): RemoteReleaseInfo? {
        return try {
            val obj = JSONObject(jsonString)
            val code = obj.optInt("versionCode", 0)
            val name = obj.optString("versionName", "").trim()
            if (code <= 0 || name.isEmpty()) return null

            val downloadUrl = obj.optString("downloadUrl", HOMEPAGE_URL)
                .takeIf { it.startsWith("https://") } ?: HOMEPAGE_URL
            val homepageUrl = obj.optString("homepageUrl", HOMEPAGE_URL)
                .takeIf { it.startsWith("https://") } ?: HOMEPAGE_URL

            val notesArray = obj.optJSONArray("releaseNotes")
            val notes = buildList {
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        val note = notesArray.optString(i, "").trim()
                        if (note.isNotEmpty()) add(note)
                    }
                }
            }

            RemoteReleaseInfo(
                versionCode = code,
                versionName = name,
                downloadUrl = downloadUrl,
                homepageUrl = homepageUrl,
                releaseNotes = notes,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isUpdateAvailable(currentVersionCode: Int, remote: RemoteReleaseInfo?): Boolean {
        return remote != null && remote.versionCode > currentVersionCode
    }

    suspend fun checkDetailed(currentVersionCode: Int): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            // Append minute-bucket cache buster so GitHub Pages CDN edge nodes serve fresh JSON
            val bust = System.currentTimeMillis() / 60_000L
            val request = Request.Builder()
                .url("$VERSION_MANIFEST_URL?t=$bust")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store")
                .header("Pragma", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("Empty manifest")
                val parsed = parseManifest(body)
                    ?: return@withContext UpdateCheckResult.Error("Invalid manifest")
                if (isUpdateAvailable(currentVersionCode, parsed)) {
                    UpdateCheckResult.UpdateAvailable(parsed)
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Offline")
        }
    }

    /**
     * Convenience wrapper for background checks: returns [RemoteReleaseInfo] if newer, else null.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): RemoteReleaseInfo? {
        return when (val result = checkDetailed(currentVersionCode)) {
            is UpdateCheckResult.UpdateAvailable -> result.info
            else -> null
        }
    }
}
