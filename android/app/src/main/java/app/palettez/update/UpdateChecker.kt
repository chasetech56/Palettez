package app.palettez.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/**
 * Checks the static GitHub Pages manifest (`version.json`) over HTTPS (WN-7, WN-8).
 * Carries zero user or device identifiers and fails silently when offline.
 */
object UpdateChecker {

    const val HOMEPAGE_URL = "https://chasetech56.github.io/Palettez/"
    const val VERSION_MANIFEST_URL = "https://chasetech56.github.io/Palettez/version.json"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
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

    /**
     * Fetches the remote manifest on [Dispatchers.IO].
     * Returns [RemoteReleaseInfo] if a newer build is available, or null otherwise.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): RemoteReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(VERSION_MANIFEST_URL)
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val parsed = parseManifest(body)
                if (isUpdateAvailable(currentVersionCode, parsed)) parsed else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
