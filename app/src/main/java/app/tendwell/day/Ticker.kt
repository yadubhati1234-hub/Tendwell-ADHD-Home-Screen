package app.tendwell.day

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Wall-clock millis, refreshed every [periodMs] while the screen is visible and
 * [active] is true. Ticks are aligned to whole seconds so a countdown never skips.
 */
@Composable
fun rememberTicker(active: Boolean, periodMs: Long = 1_000L): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(System.currentTimeMillis(), active, lifecycle) {
        value = System.currentTimeMillis()
        if (!active) return@produceState
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                value = now
                delay(periodMs - now % periodMs + 5)
            }
        }
    }
}
