package app.palettez.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Shows a short message in the app's snackbar. Provided by DesignKitApp. */
val LocalMessenger = staticCompositionLocalOf<(String) -> Unit> { {} }

fun Context.copyText(text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Design Kit", text))
}

fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Decodes an image Uri into a software bitmap whose longest side is at most [maxSide].
 * Software (not HARDWARE) so we can read pixels for palette extraction.
 */
suspend fun Context.decodeBitmap(uri: Uri, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val scale = maxSide.toFloat() / max(w, h)
                if (scale < 1f) {
                    decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?.let { it.scaledToMax(maxSide) }
        }
    }.getOrNull()
}

fun Bitmap.scaledToMax(maxSide: Int): Bitmap {
    val longest = max(width, height)
    if (longest <= maxSide) return this
    val s = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * s).roundToInt().coerceAtLeast(1), (height * s).roundToInt().coerceAtLeast(1), true)
}

suspend fun Context.writePng(uri: Uri, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        contentResolver.openOutputStream(uri, "wt")?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
    }.getOrDefault(false)
}

suspend fun Context.writeBytes(uri: Uri, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes); true } ?: false
    }.getOrDefault(false)
}

suspend fun Context.shareBitmap(bitmap: Bitmap, filename: String = "palette_brand_card.png"): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val sharedDir = java.io.File(cacheDir, "shared_images").apply { mkdirs() }
        val file = java.io.File(sharedDir, filename)
        java.io.FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(this@shareBitmap, "$packageName.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Brand Palette").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
