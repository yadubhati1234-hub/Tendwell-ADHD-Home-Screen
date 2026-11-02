package app.tendwell.now

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tendwell.day.rememberTicker
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

/** Minutes from midnight, local time, for an epoch time. */
fun minuteOfDay(epochMs: Long): Int {
    val t = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return t.hour * 60 + t.minute
}

fun clockAt(epochMs: Long): String = TimeBarModel.formatClock(minuteOfDay(epochMs))

/** Whole minutes left, rounded up, so "1 min" shows until the very end. */
fun minutesLeft(ms: Long): Int = ceil(ms / 60_000.0).toInt()

/**
 * The Now card on Home. Shows the current task and its timer, a break, the
 * small celebration after Done, or an invitation to set a task.
 */
@Composable
fun NowCard(
    task: NowTask?,
    onOpenTimer: () -> Unit,
    onSetTask: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDone: () -> Unit,
    onStartTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tendwell.colors
    val running = task?.running == true
    val now by rememberTicker(active = running)
    val isBreak = task?.phase == NowPhase.Break
    val accent = if (isBreak) c.time else c.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isBreak) c.timeSoft else c.primarySoft)
            .border(1.5.dp, if (isBreak) c.time.copy(alpha = 0.28f) else c.primaryLine, RoundedCornerShape(24.dp))
            .padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(if (isBreak) "Break" else "Now", dot = accent, ink = if (isBreak) c.onTimeSoft else c.onPrimarySoft, pulsing = running, modifier = Modifier.weight(1f))
            if (task != null && task.phase != NowPhase.Done) {
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClick = onSetTask)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Change", style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold), color = c.onPrimarySoft)
                }
            } else {
                Box(Modifier.height(44.dp))
            }
        }

        when {
            task == null -> {
                Text("What are you doing now?", style = TendwellType.cardTitle, color = c.ink)
                CardButton("Set a Now task", TwIcons.Plus, onSetTask, primary = true)
            }
            task.phase == NowPhase.Done -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(c.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(TwIcons.Check, null, tint = c.onPrimary, modifier = Modifier.size(24.dp)) }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Done. That counts.", style = TendwellType.cardTitle.copy(fontSize = 23.sp), color = c.ink)
                        Text("Pick what's next when you're ready.", style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold), color = c.onPrimarySoft)
                    }
                }
                CardButton("Set a new Now task", TwIcons.Plus, onSetTask, primary = true)
            }
            else -> {
                val left = task.remainingMs(now)
                val status = when {
                    !task.hasTimer -> "No timer"
                    task.timeIsUp(now) -> if (isBreak) "Break's over" else "Time's up"
                    !task.running -> "Paused, ${minutesLeft(left)} min left"
                    isBreak -> "Back to it at ${clockAt(task.endAtMs)}"
                    else -> "${minutesLeft(left)} min left, ends ${clockAt(task.endAtMs)}"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button, onClickLabel = "Open the focus timer", onClick = if (task.hasTimer) onOpenTimer else onStartTimer),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (task.hasTimer) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            TimerRing(
                                fractionLeft = task.fractionLeft(now),
                                track = if (isBreak) c.time.copy(alpha = 0.24f) else c.primaryLine,
                                progress = accent,
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(56.dp),
                            )
                            Text(
                                "${minutesLeft(left)}",
                                style = TendwellType.small.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
                                color = if (isBreak) c.onTimeSoft else c.onPrimarySoft,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            if (isBreak) "Step away from the screen" else task.title,
                            style = TendwellType.cardTitle.copy(fontSize = 25.sp),
                            color = c.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(status, style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold), color = if (isBreak) c.onTimeSoft else c.onPrimarySoft)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        !task.hasTimer -> CardButton("Start timer", TwIcons.Play, onStartTimer, Modifier.weight(1f))
                        task.timeIsUp(now) -> CardButton("+5 min", null, onOpenTimer, Modifier.weight(1f))
                        task.running -> CardButton("Pause", TwIcons.Pause, onPause, Modifier.weight(1f))
                        else -> CardButton("Resume", TwIcons.Play, onResume, Modifier.weight(1f))
                    }
                    CardButton(if (isBreak) "End break" else "Done", TwIcons.Check, onDone, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun Eyebrow(text: String, dot: Color, ink: Color, pulsing: Boolean, modifier: Modifier = Modifier) {
    val pulse = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulseAlpha")
        a
    } else 1f
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(7.dp).alpha(pulse).clip(CircleShape).background(dot))
        Text(text.uppercase(), style = TendwellType.eyebrow, color = ink)
    }
}

@Composable
private fun CardButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val c = Tendwell.colors
    Row(
        modifier = modifier
            .then(if (primary) Modifier.fillMaxWidth() else Modifier)
            .height(if (primary) 48.dp else 44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (primary) c.primary else c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = if (primary) c.onPrimary else c.ink, modifier = Modifier.size(18.dp))
        Text(text, style = TendwellType.button, color = if (primary) c.onPrimary else c.ink)
    }
}

