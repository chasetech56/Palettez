package app.palettez.grid

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.palettez.util.decodeBitmap
import kotlinx.coroutines.launch

class GridViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("grid", Context.MODE_PRIVATE)

    var spec by mutableStateOf(GridSpec.fromJson(prefs.getString("spec", null))); private set
    var underlay by mutableStateOf<Bitmap?>(null); private set

    fun update(transform: (GridSpec) -> GridSpec) {
        spec = transform(spec)
        prefs.edit().putString("spec", spec.toJson()).apply()
    }

    fun loadUnderlay(uri: Uri, matchSize: Boolean) {
        viewModelScope.launch {
            val bmp = getApplication<Application>().decodeBitmap(uri, maxSide = MAX_SIDE) ?: return@launch
            underlay = bmp
            if (matchSize) matchUnderlaySize()
        }
    }

    fun matchUnderlaySize() {
        underlay?.let { b -> update { it.copy(width = b.width, height = b.height) } }
    }

    fun clearUnderlay() { underlay = null }
}
