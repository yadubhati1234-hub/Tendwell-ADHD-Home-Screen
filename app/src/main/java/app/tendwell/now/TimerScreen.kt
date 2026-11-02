package app.tendwell.now

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import app.tendwell.day.rememberTicker
import app.tendwell.timebar.DayEvent
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/** Full-screen focus timer for the Now task or a break, and the "Done" moment after it. */
@Composable
fun TimerScreen(
    task: NowTask,
    nextEvent: DayEvent?,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddFive: () -> Unit,
    onDone: () -> Unit,
    onStartBreak: () -> Unit,
) {
    val c = Tendwell.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TwIconButton(TwIcons.ChevronDown, contentDescription = "Back to Home", onClick = onBack, modifier = Modifier.offset(x = (-10).dp))
            Spacer(Modifier.weight(1f))
            if (task.phase != NowPhase.Done) {
                val isBreak = task.phase == NowPhase.Break
                Eyebrow(
                    if (isBreak) "Break" else "Now",
                    dot = if (isBreak) c.time else c.primary,
                    ink = if (isBreak) c.onTimeSoft else c.onPrimarySoft,
                    pulsing = task.running,
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(34.dp))
        }

        if (task.phase == NowPhase.Done) {
            DoneMoment(task, nextEvent, onBack, onStartBreak)
        } else {
            Counting(task, nextEvent, onPause, onResume, onAddFive, onDone)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Counting(
    task: NowTask,
    nextEvent: DayEvent?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddFive: () -> Unit,
    onDone: () -> Unit,
) {
    val c = Tendwell.colors
    val now by rememberTicker(active = task.running)
    val isBreak = task.phase == NowPhase.Break
    val accent = if (isBreak) c.time else c.primary
    val left = task.remainingMs(now)
    val totalMin = (task.totalMs / 60_000).toInt()
    val secs = (left + 999) / 1000
    val clock = "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"
    val up = task.timeIsUp(now)

    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (isBreak) "Break" else task.title,
            style = TendwellType.h1,
            color = c.ink,
            maxLines = 3,
            modifier = Modifier.semantics { heading() },
        )
        val range = if (task.running || up) {
            "${clockAt(task.startedAtMs)} → ${clockAt(task.endAtMs)}"
        } else {
            "Paused · ends ${clockAt(task.projectedEndMs(now))} if you resume now"
        }
        Text(range, style = TendwellType.secondary, color = c.ink2)
    }

    Spacer(Modifier.weight(1f).heightIn(min = 16.dp))

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val ringSize = min(maxWidth, 288.dp)
        Box(
            Modifier
                .size(ringSize)
                .clearAndSetSemantics {
                    contentDescription = when {
                        up -> if (isBreak) "Break's over" else "Time's up"
                        task.running -> "${secs / 60} minutes ${secs % 60} seconds left of $totalMin minutes"
                        else -> "Paused, ${secs / 60} minutes left"
                    }
                    liveRegion = LiveRegionMode.Polite
                },
            contentAlignment = Alignment.Center,
        ) {
            TimerRing(
                fractionLeft = task.fractionLeft(now),
                track = if (isBreak) c.time else c.primaryLine,
                trackAlpha = if (isBreak) 0.24f else 1f,
                progress = accent,
                progressAlpha = if (task.running) 1f else 0.55f,
                strokeWidth = 15.dp,
                modifier = Modifier.size(ringSize),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (up) "0:00" else clock,
                    style = TendwellType.body.copy(fontSize = 60.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", lineHeight = 64.sp),
                    color = c.ink,
                )
                Text(
                    when {
                        up -> if (isBreak) "Break's over" else "Time's up"
                        task.running -> "of $totalMin min"
                        else -> "Paused"
                    },
                    style = TendwellType.secondary,
                    color = c.ink2,
                )
            }
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        PillButton("+5 min", null, onAddFive, Modifier.weight(1f))
        val playing = task.running && !up
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable(role = Role.Button, onClickLabel = if (playing) "Pause timer" else "Resume timer", onClick = if (playing) onPause else if (up) onAddFive else onResume),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) TwIcons.Pause else TwIcons.Play,
                contentDescription = if (playing) "Pause timer" else "Resume timer",
                tint = if (isBreak) c.bg else c.onPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
        PillButton(if (isBreak) "End" else "Done", TwIcons.Check, onDone, Modifier.weight(1f))
    }

    Spacer(Modifier.weight(1f).heightIn(min = 16.dp))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isBreak) {
            InfoLine(TwIcons.Sprout, "Step away from the screen. Back to it at ${clockAt(task.projectedEndMs(now))}.")
        } else {
            nextEvent?.let { InfoLine(TwIcons.Calendar, "Next: ${it.title} at ${TimeBarModel.formatClock(it.startMin)}") }
            InfoLine(TwIcons.Bell, "A gentle nudge at ${clockAt(task.projectedEndMs(now))}")
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DoneMoment(
    task: NowTask,
    nextEvent: DayEvent?,
    onHome: () -> Unit,
    onStartBreak: () -> Unit,
) {
    val c = Tendwell.colors
    val context = LocalContext.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        val animationsOn = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        if (animationsOn) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, keyframes { durationMillis = 400; 1.06f at 280 })
        }
    }

    Spacer(Modifier.weight(1f))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp).scale(scale.value).clip(CircleShape).background(c.primarySoft))
            Box(Modifier.size(132.dp).scale(scale.value).clip(CircleShape).background(c.primaryLine))
            Box(
                Modifier.size(96.dp).scale(scale.value).clip(CircleShape).background(c.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(TwIcons.Check, null, tint = c.onPrimary, modifier = Modifier.size(48.dp)) }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text("Done. That counts.", style = TendwellType.h1, color = c.ink, textAlign = TextAlign.Center)
            val line = if (task.spentMinutes > 0) "${task.spentMinutes} minutes on ${task.title}." else "${task.title}: done."
            Text(line, style = TendwellType.body, color = c.ink2, textAlign = TextAlign.Center)
            nextEvent?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(TwIcons.Calendar, null, tint = c.time, modifier = Modifier.size(16.dp))
                    Text("Next: ${it.title} at ${TimeBarModel.formatClock(it.startMin)}", style = TendwellType.small, color = c.ink2)
                }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.primary)
                .clickable(role = Role.Button, onClick = onHome),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            Icon(TwIcons.Home, null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
            Text("Back to Home", style = TendwellType.button.copy(fontSize = 16.sp), color = c.onPrimary)
        }
        PillButton("Take a ${NowTask.BREAK_MINUTES} min break", TwIcons.Hourglass, onStartBreak, Modifier.fillMaxWidth().height(52.dp))
    }
}

@Composable
private fun PillButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Text(text, style = TendwellType.button, color = c.ink)
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    val c = Tendwell.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.lilac), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(19.dp))
        }
        Text(text, style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
    }
}
