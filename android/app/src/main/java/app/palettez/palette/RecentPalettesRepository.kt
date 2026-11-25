package app.palettez.palette

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A palette previously extracted from an image, persisted so the user can always return to it.
 */
data class RecentPalette(
    val id: String,
    val name: String,
    val timestamp: Long,
    val swatches: List<Swatch>,
    val thumbnailPath: String? = null,
) {
    fun loadThumbnail(): Bitmap? {
        val path = thumbnailPath ?: return null
        val file = File(path)
        return if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    fun formattedTime(): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val minutes = diffMs / (60 * 1000)
        val hours = diffMs / (60 * 60 * 1000)
        val days = diffMs / (24 * 60 * 60 * 1000)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

/**
 * Local-first persistence for image-extracted palettes.
 * Uses atomic JSON writes and caches compressed image thumbnails locally.
 */
class RecentPalettesRepository(private val baseDir: File) {

    private val jsonFile: File get() = File(baseDir, "recent_palettes.json")
    private val thumbsDir: File get() = File(baseDir, "recent_palettes_thumbs").apply { mkdirs() }

    @Synchronized
    fun getAll(): List<RecentPalette> {
        val file = jsonFile
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            val array = JSONArray(content)
            val list = mutableListOf<RecentPalette>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.optString("name", "Image Palette")
                val timestamp = obj.getLong("timestamp")
                val thumb = obj.optString("thumbnailPath", "").takeIf { it.isNotEmpty() }
                val swatchesArray = obj.getJSONArray("swatches")
                val swatches = mutableListOf<Swatch>()
                for (j in 0 until swatchesArray.length()) {
                    val sObj = swatchesArray.getJSONObject(j)
                    swatches.add(
                        Swatch(
                            r = sObj.getInt("r"),
                            g = sObj.getInt("g"),
                            b = sObj.getInt("b"),
                            share = sObj.getDouble("share").toFloat(),
                        )
                    )
                }
                list.add(RecentPalette(id, name, timestamp, swatches, thumb))
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveOrUpdate(
        id: String,
        swatches: List<Swatch>,
        thumbnailBitmap: Bitmap? = null,
        name: String = "Image Palette",
    ): RecentPalette {
        if (swatches.isEmpty()) throw IllegalArgumentException("Cannot save empty palette")
        val current = getAll().toMutableList()
        var thumbPath: String? = null

        val existingIndex = current.indexOfFirst { it.id == id }
        if (existingIndex >= 0) {
            thumbPath = current[existingIndex].thumbnailPath
        }

        if (thumbnailBitmap != null) {
            try {
                val thumbFile = File(thumbsDir, "thumb_${id}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbPath = thumbFile.absolutePath
            } catch (_: Exception) {
                // Ignore thumbnail write error
            }
        }

        val item = RecentPalette(
            id = id,
            name = name,
            timestamp = System.currentTimeMillis(),
            swatches = swatches,
            thumbnailPath = thumbPath,
        )

        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        }
        current.add(0, item)

        // Keep maximum 20 items, evicting oldest
        while (current.size > 20) {
            val removed = current.removeAt(current.size - 1)
            removed.thumbnailPath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
        }

        writeAll(current)
        return item
    }

    @Synchronized
    fun delete(id: String) {
        val current = getAll().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val removed = current.removeAt(index)
            removed.thumbnailPath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
            writeAll(current)
        }
    }

    @Synchronized
    fun mergePalettes(imported: List<RecentPalette>): Int {
        if (imported.isEmpty()) return 0
        val current = getAll().toMutableList()
        val existingIds = current.map { it.id }.toMutableSet()
        var added = 0
        for (item in imported) {
            if (item.id !in existingIds && item.swatches.isNotEmpty()) {
                current.add(item)
                existingIds.add(item.id)
                added++
            }
        }
        val sorted = current.sortedByDescending { it.timestamp }.take(25)
        writeAll(sorted)
        return added
    }

    @Synchronized
    fun clearAll() {
        val current = getAll()
        current.forEach { item ->
            item.thumbnailPath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
        }
        try {
            if (jsonFile.exists()) jsonFile.delete()
            thumbsDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun writeAll(list: List<RecentPalette>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("timestamp", item.timestamp)
            obj.put("thumbnailPath", item.thumbnailPath ?: "")
            val swatchesArray = JSONArray()
            for (s in item.swatches) {
                val sObj = JSONObject()
                sObj.put("r", s.r)
                sObj.put("g", s.g)
                sObj.put("b", s.b)
                sObj.put("share", s.share.toDouble())
                swatchesArray.put(sObj)
            }
            obj.put("swatches", swatchesArray)
            array.put(obj)
        }

        val tmp = File(baseDir, "recent_palettes.json.tmp")
        tmp.writeText(array.toString(2))
        tmp.renameTo(jsonFile)
    }
}
