package app.palettez.palette

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.palettez.util.decodeBitmap
import app.palettez.util.scaledToMax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class SwatchOrder(val label: String) { Share("Most used"), Light("Light → dark"), Hue("By hue") }

class PaletteViewModel(app: Application) : AndroidViewModel(app) {

    private val recentRepo = RecentPalettesRepository(app.filesDir)

    var image by mutableStateOf<ImageBitmap?>(null); private set
    var swatches by mutableStateOf<List<Swatch>>(emptyList()); private set
    var count by mutableIntStateOf(6); private set
    var order by mutableStateOf(SwatchOrder.Share); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    /** Increments each time an image arrives via the Android share sheet. */
    var shareCounter by mutableIntStateOf(0); private set

    var recents by mutableStateOf<List<RecentPalette>>(emptyList()); private set

    private var pixels: IntArray = IntArray(0)
    private var job: Job? = null
    private var currentSessionId: String = UUID.randomUUID().toString()
    private var currentThumbnail: Bitmap? = null
    val thumbnailBitmap: Bitmap? get() = currentThumbnail
    private var currentImageName: String = "Image Palette"

    init {
        loadRecents()
    }

    fun loadRecents() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { recentRepo.getAll() }
            recents = list
        }
    }

    fun load(uri: Uri, fromShare: Boolean = false) {
        if (fromShare) shareCounter++
        viewModelScope.launch {
            busy = true; error = null
            val bmp = getApplication<Application>().decodeBitmap(uri, maxSide = 1600)
            if (bmp == null) { busy = false; error = "Couldn't open that image"; return@launch }
            currentSessionId = UUID.randomUUID().toString()
            currentImageName = "Image Palette"
            currentThumbnail = withContext(Dispatchers.Default) { bmp.scaledToMax(320) }
            setBitmapInternal(bmp)
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            busy = true; error = null
            currentSessionId = UUID.randomUUID().toString()
            currentImageName = "Camera Palette"
            currentThumbnail = withContext(Dispatchers.Default) { bitmap.scaledToMax(320) }
            setBitmapInternal(bitmap)
        }
    }

    private suspend fun setBitmapInternal(bitmap: Bitmap) {
        val display = withContext(Dispatchers.Default) { bitmap.scaledToMax(1600) }
        image = display.asImageBitmap()
        pixels = withContext(Dispatchers.Default) { PaletteExtractor.samplePixels(display) }
        extract()
    }

    fun selectRecent(recent: RecentPalette) {
        currentSessionId = recent.id
        currentImageName = recent.name
        count = recent.swatches.size
        swatches = sort(recent.swatches)
        val thumb = recent.loadThumbnail()
        currentThumbnail = thumb
        if (thumb != null) {
            image = thumb.asImageBitmap()
            pixels = PaletteExtractor.samplePixels(thumb)
        } else {
            image = null
            pixels = IntArray(0)
        }
    }

    fun deleteRecent(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { recentRepo.delete(id) }
            loadRecents()
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { recentRepo.clearAll() }
            loadRecents()
        }
    }

    fun updateCount(value: Int) { if (value != count) { count = value; extract() } }
    fun updateOrder(value: SwatchOrder) { order = value; swatches = sort(swatches) }

    private fun extract() {
        if (pixels.isEmpty()) { busy = false; return }
        job?.cancel()
        val px = pixels; val k = count
        val sessionId = currentSessionId
        val thumb = currentThumbnail
        val name = currentImageName

        job = viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.Default) { PaletteExtractor.extract(px, k) }
            swatches = sort(result)
            busy = false
            if (result.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    recentRepo.saveOrUpdate(
                        id = sessionId,
                        swatches = result,
                        thumbnailBitmap = thumb,
                        name = name,
                    )
                }
                loadRecents()
            }
        }
    }

    private fun sort(list: List<Swatch>) = when (order) {
        SwatchOrder.Share -> list.sortedByDescending { it.share }
        SwatchOrder.Light -> list.sortedByDescending { it.luminance }
        SwatchOrder.Hue -> list.sortedBy { it.hue }
    }

    // ---- exports ----

    fun hexList() = swatches.joinToString(", ") { it.hex }

    fun cssVars() = swatches.mapIndexed { i, s -> "  --color-${i + 1}: ${s.hex};" }
        .joinToString("\n", prefix = ":root {\n", postfix = "\n}")

    fun fullList() = swatches.mapIndexed { i, s ->
        "Color ${i + 1}\n  HEX  ${s.hex}\n  RGB  ${s.rgb}\n  CMYK ${s.cmyk}\n  HSL  ${s.hsl}"
    }.joinToString("\n\n")

    fun renderPng(): Bitmap {
        val w = 1200; val rowH = 150
        val bmp = Bitmap.createBitmap(w, rowH * swatches.size.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val fill = Paint()
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 44f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 26f }
        swatches.forEachIndexed { i, s ->
            val top = (i * rowH).toFloat()
            fill.color = s.argb
            c.drawRect(0f, top, w.toFloat(), top + rowH, fill)
            val ink = if (s.luminance > 150) Color.BLACK else Color.WHITE
            big.color = ink; small.color = ink
            c.drawText(s.hex, 40f, top + 62f, big)
            c.drawText("${s.rgb}   ${s.cmyk}", 40f, top + 110f, small)
        }
        return bmp
    }
}
