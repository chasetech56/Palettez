package app.palettez.fonts

import android.content.Context
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

data class FontStyle(val weight: Int, val italic: Boolean) {
    val label: String get() = "$weight${if (italic) " italic" else ""}"
    val fileTag: String get() = "$weight${if (italic) "i" else ""}"
}

enum class FontCategory(val label: String) {
    All("All"), SansSerif("Sans"), Serif("Serif"), Display("Display"), Handwriting("Handwriting"), Monospace("Mono");

    companion object {
        fun from(raw: String): FontCategory = when (raw.lowercase().replace(" ", "").replace("-", "")) {
            "sansserif" -> SansSerif
            "serif" -> Serif
            "handwriting" -> Handwriting
            "monospace" -> Monospace
            else -> Display
        }
    }
}

data class FontInfo(
    val family: String,
    val category: FontCategory,
    val styles: List<FontStyle>,
    val popularity: Int,
    val dateAdded: String, // yyyy-MM-dd, may be empty
    val isNew: Boolean,
) {
    /** The style used for list previews: regular 400 if present, else the closest weight. */
    val previewStyle: FontStyle
        get() = styles.firstOrNull { it.weight == 400 && !it.italic }
            ?: styles.filter { !it.italic }.minByOrNull { kotlin.math.abs(it.weight - 400) }
            ?: styles.firstOrNull() ?: FontStyle(400, false)

    val genericFallback: String
        get() = when (category) {
            FontCategory.Serif -> "serif"
            FontCategory.Monospace -> "monospace"
            FontCategory.Handwriting -> "cursive"
            else -> "sans-serif"
        }

    private val urlFamily get() = URLEncoder.encode(family, "UTF-8")

    /** css2 axis spec for embedding every style, e.g. ":ital,wght@0,400;0,700;1,400". */
    val embedSpec: String
        get() {
            val sorted = styles.sortedWith(compareBy({ it.italic }, { it.weight }))
            if (sorted.none { it.italic }) {
                val ws = sorted.map { it.weight }.distinct()
                return if (ws == listOf(400)) "" else ":wght@" + ws.joinToString(";")
            }
            return ":ital,wght@" + sorted.joinToString(";") { "${if (it.italic) 1 else 0},${it.weight}" }
        }

    val embedLink: String
        get() = """<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=$urlFamily$embedSpec&display=swap" rel="stylesheet">"""

    val cssRule: String get() = "font-family: \"$family\", $genericFallback;"
    val specimenUrl: String get() = "https://fonts.google.com/specimen/$urlFamily"
}

private fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 15_000
    conn.readTimeout = 30_000
    try {
        if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
        return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

/**
 * The full Google Fonts catalog, read from fonts.google.com so newly released families
 * appear without an app update. The last good copy is cached for offline use.
 */
class FontCatalog(context: Context) {
    private val file = File(context.filesDir, "google-fonts-catalog.json")

    val cachedAt: Long get() = if (file.exists()) file.lastModified() else 0L

    suspend fun cached(windowDays: Int = 60): List<FontInfo>? = withContext(Dispatchers.IO) {
        if (!file.exists()) null else runCatching { parse(file.readText(), windowDays) }.getOrNull()
    }

    suspend fun download(windowDays: Int = 60): List<FontInfo> = withContext(Dispatchers.IO) {
        val raw = httpGet("https://fonts.google.com/metadata/fonts").removePrefix(")]}'").trim()
        val list = parse(raw, windowDays) // validate before replacing the cache
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(raw)
        tmp.renameTo(file)
        list
    }

    fun parse(json: String, windowDays: Int = 60): List<FontInfo> {
        val today = LocalDate.now()
        val arr = JSONObject(json).getJSONArray("familyMetadataList")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val fonts = o.optJSONObject("fonts")
            val styles = fonts?.keys()?.asSequence()?.map { key ->
                FontStyle(key.trimEnd('i').toIntOrNull() ?: 400, key.endsWith("i"))
            }?.toList()?.distinct()?.sortedWith(compareBy({ it.italic }, { it.weight }))
                ?.takeIf { it.isNotEmpty() } ?: listOf(FontStyle(400, false))
            val date = o.optString("dateAdded", "")
            val isNew = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(date.take(10)), today) <= windowDays }.getOrDefault(false)
            FontInfo(
                family = o.getString("family"),
                category = FontCategory.from(o.optString("category", "Display")),
                styles = styles,
                popularity = o.optInt("popularity", 99_999),
                dateAdded = date.take(10),
                isNew = isNew,
            )
        }
    }
}

/**
 * Downloads individual font files straight from Google's servers (css2 API → fonts.gstatic.com)
 * and caches them on the device. Requests without a browser user agent receive plain TTF files,
 * which Android can load directly.
 */
class FontLoader(context: Context) {
    private val dir = File(context.cacheDir, "google-fonts").apply { mkdirs() }
    private val memory = ConcurrentHashMap<String, Typeface>()
    private val failed = ConcurrentHashMap.newKeySet<String>()
    private val limit = Semaphore(4)

    fun getCacheSizeBytes(): Long =
        dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clearCache() {
        memory.clear()
        failed.clear()
        dir.listFiles()?.forEach { it.delete() }
    }

    fun retryFailed() {
        failed.clear()
    }

    fun fileFor(family: String, style: FontStyle): File =
        File(dir, family.replace(Regex("[^A-Za-z0-9]"), "_") + "-" + style.fileTag + ".ttf")

    fun peek(family: String, style: FontStyle): Typeface? = memory[key(family, style)]

    suspend fun load(family: String, style: FontStyle): Typeface? {
        val key = key(family, style)
        memory[key]?.let { return it }
        return limit.withPermit {
            withContext(Dispatchers.IO) {
                memory[key] ?: runCatching {
                    val f = fileFor(family, style)
                    if (!f.exists() || f.length() == 0L) download(family, style, f)
                    Typeface.createFromFile(f).also { memory[key] = it }
                }.onFailure {
                    fileFor(family, style).delete()
                }.getOrNull()
            }
        }
    }

    private fun download(family: String, style: FontStyle, target: File) {
        val axis = if (style.italic) ":ital,wght@1,${style.weight}" else ":wght@${style.weight}"
        val css = httpGet("https://fonts.googleapis.com/css2?family=${URLEncoder.encode(family, "UTF-8")}$axis&display=swap")
        val url = pickFontUrl(css) ?: error("No font URL in CSS")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000; conn.readTimeout = 30_000
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val tmp = File(target.parentFile, target.name + ".part")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
        } finally {
            conn.disconnect()
        }
    }

    /** If the CSS is split by script subsets, prefer the "latin" block. */
    private fun pickFontUrl(css: String): String? {
        val urlRe = Regex("""url\((https://[^)]+)\)""")
        val chunks = css.split("@font-face")
        for (i in 1 until chunks.size) {
            if (chunks[i - 1].contains("/* latin */")) urlRe.find(chunks[i])?.let { return it.groupValues[1] }
        }
        return urlRe.find(css)?.groupValues?.get(1)
    }

    private fun key(family: String, style: FontStyle) = "$family|${style.fileTag}"
}
