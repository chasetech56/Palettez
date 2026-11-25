package app.palettez.fonts

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class FontSort(val label: String) { Newest("Newest"), Popular("Popular"), Alpha("A–Z") }

class FontsViewModel(app: Application) : AndroidViewModel(app) {

    private val catalog = FontCatalog(app)
    val loader = FontLoader(app)

    var fonts by mutableStateOf<List<FontInfo>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var status by mutableStateOf("Loading font list…"); private set

    var query by mutableStateOf("")
    var category by mutableStateOf(FontCategory.All)
    var sort by mutableStateOf(FontSort.Newest)
    var previewText by mutableStateOf("The quick brown fox jumps over the lazy dog")
    var previewSize by mutableFloatStateOf(30f)
    var currentWindowDays by mutableStateOf(60)
    private var initialRefreshChecked = false

    init {
        viewModelScope.launch {
            catalog.cached(currentWindowDays)?.let {
                fonts = it
                updateStatus(fromCache = true, windowDays = currentWindowDays)
            }
        }
    }

    fun applySettings(refreshOnOpen: Boolean, windowDays: Int) {
        currentWindowDays = windowDays
        val today = LocalDate.now()
        if (fonts.isNotEmpty()) {
            fonts = fonts.map { font ->
                val isNew = runCatching {
                    ChronoUnit.DAYS.between(LocalDate.parse(font.dateAdded.take(10)), today) <= windowDays
                }.getOrDefault(false)
                font.copy(isNew = isNew)
            }
            updateStatus(fromCache = true, windowDays = windowDays)
        }
        // SE-4: Respect "Update font list on open"; only hit network automatically if enabled
        if (!initialRefreshChecked) {
            initialRefreshChecked = true
            if (refreshOnOpen) {
                refresh(force = true, windowDays = windowDays)
            } else if (fonts.isEmpty()) {
                status = "Offline cache empty — tap refresh to download Google Fonts."
            }
        }
    }

    fun refresh(force: Boolean = true, windowDays: Int = currentWindowDays) {
        if (loading || (!force && fonts.isNotEmpty())) return
        loader.retryFailed()
        viewModelScope.launch {
            loading = true
            runCatching { catalog.download(windowDays) }
                .onSuccess { fonts = it; updateStatus(fromCache = false, windowDays = windowDays) }
                .onFailure {
                    status = if (fonts.isEmpty()) "Couldn't reach Google Fonts. Check your connection and tap refresh."
                    else "Offline — showing the list saved ${relative(catalog.cachedAt)}."
                }
            loading = false
        }
    }

    private fun updateStatus(fromCache: Boolean, windowDays: Int = currentWindowDays) {
        val newCount = fonts.count { it.isNew }
        status = buildString {
            append("%,d families".format(fonts.size))
            if (newCount > 0) append(" · $newCount added in the last $windowDays days")
            append(if (fromCache) " · saved ${relative(catalog.cachedAt)}" else " · up to date")
        }
    }

    private fun relative(time: Long): String =
        if (time == 0L) "earlier" else DateUtils.getRelativeTimeSpanString(time).toString().lowercase()

    fun visible(): List<FontInfo> {
        val q = query.trim()
        val list = fonts.filter {
            (category == FontCategory.All || it.category == category) &&
                (q.isEmpty() || it.family.contains(q, ignoreCase = true) || it.category.label.contains(q, ignoreCase = true))
        }
        return when (sort) {
            FontSort.Newest -> list.sortedWith(compareByDescending<FontInfo> { it.dateAdded }.thenBy { it.popularity })
            FontSort.Popular -> list.sortedBy { it.popularity }
            FontSort.Alpha -> list.sortedBy { it.family.lowercase() }
        }
    }
}
