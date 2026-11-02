package app.tendwell.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * The current time, truncated to the minute. Updated by the system's
 * minute tick (which also covers time and time-zone changes) and whenever
 * Home comes back to the foreground, so it is never stale after sleep.
 */
@Composable
fun rememberCurrentMinute(): State<LocalDateTime> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val now = remember { mutableStateOf(nowMinute()) }

    DisposableEffect(context, lifecycleOwner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                now.value = nowMinute()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) now.value = nowMinute()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return now
}

private fun nowMinute(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
