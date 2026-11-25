package app.palettez.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.palettez.ui.theme.LocalPalettezColors
import app.palettez.ui.theme.PalettezTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Toast state holder. Call [show] from a coroutine to display a message for 1.8 seconds.
 *
 * Usage:
 * ```
 * val toastState = remember { PalettezToastHostState() }
 * PalettezToastHost(hostState = toastState)
 * // elsewhere:
 * scope.launch { toastState.show("Copied #E4572E") }
 * ```
 */
class PalettezToastHostState {
    // The currently visible message, or null
    var currentMessage by mutableStateOf<String?>(null)
        private set

    private val mutex = Mutex()

    /**
     * Shows [message] for 1 800 ms. Concurrent calls are serialised.
     */
    suspend fun show(message: String) {
        mutex.withLock {
            currentMessage = message
            delay(1_800)
            currentMessage = null
        }
    }
}

/**
 * Host composable — place it above the [FloatingTabBar] in your Scaffold/Box.
 *
 * From Onboarding2.png: dark pill ("Copied #E4572E" with a checkmark) floating in the lower area.
 * On cream/surface screens we use ink pill; on dark screens the pill stays ink (darkBackground = true).
 */
@Composable
fun PalettezToastHost(
    hostState: PalettezToastHostState,
    modifier: Modifier = Modifier,
    darkBackground: Boolean = false,
) {
    val colors = LocalPalettezColors.current
    val message = hostState.currentMessage

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.ink)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.lime,
                    modifier = Modifier.padding(end = 2.dp),
                )
                if (message != null) {
                    Text(
                        text = message,
                        style = PalettezTypography.labelLarge.copy(color = colors.onInk),
                    )
                }
            }
        }
    }
}
