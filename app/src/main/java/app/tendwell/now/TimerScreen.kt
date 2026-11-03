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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import app.tendwell.day.rememberTicker
import app.tendwell.timebar.DayEvent
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.ReadableWidth
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.components.WideMaxWidth
import app.tendwell.ui.components.isWide
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * Full-screen focus timer for the Now task or a break, and the "Done" moment after it.
 * On a wide window ([isWide]) the ring and its buttons sit on the left and the
 * task, its times and the next event on the right.
 */
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
    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        val wide = isWide(maxWidth, maxHeight)
        // Room for the ring once the top bar and the buttons under it are placed.
        val wideRing = (maxHeight - 260.dp).coerceIn(140.dp, 300.dp)
        Column(
            Modifier
                .widthIn(max = if (wide) WideMaxWidth else ReadableWidth)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = if (wide) 20.dp else 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TwIconButton(TwIcons.ChevronDown, contentDescription = "Back to Home", onClick = onBack, modifier = Modifier.offset(x = (-10).dp))
                if (!wide) {
                    Spacer(Modifier.weight(1f))
                    if (task.phase != NowPhase.Done) PhaseEyebrow(task)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(34.dp))
                }
            }

            when {
                task.phase == NowPhase.Done && wide -> DoneMomentWide(task, nextEvent, onBack, onStartBreak)
                task.phase == NowPhase.Done -> DoneMoment(task, nextEvent, onBack, onStartBreak)
                wide -> CountingWide(task, nextEvent, wideRing, onPause, onResume, onAddFive, onDone)
                else -> Counting(task, nextEvent, onPause, onResume, onAddFive, onDone)
            }
        }
    }
}

@Composable
private fun PhaseEyebrow(task: NowTask) {
    val c = Tendwell.colors
    val isBreak = task.phase == NowPhase.Break
    Eyebrow(
        if (isBreak) "Break" else "Now",
        dot = if (isBreak) c.time else c.primary,
        ink = if (isBreak) c.onTimeSoft else c.onPrimarySoft,
        pulsing = task.running,
    )
}

/** "10:17 → 10:42", or when it would end if resumed now. */
private fun rangeText(task: NowTask, now: Long): String =
    if (task.running || task.timeIsUp(now)) {
        "${clockAt(task.startedAtMs)} → ${clockAt(task.endAtMs)}"
    } else {
        "Paused · ends ${clockAt(task.projectedEndMs(now))} if you resume now"
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

    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (isBreak) "Break" else task.title,
            style = TendwellType.h1,
            color = c.ink,
            maxLines = 3,
            modifier = Modifier.semantics { heading() },
        )
        Text(rangeText(task, now), style = TendwellType.secondary, color = c.ink2)
    }

    Spacer(Modifier.weight(1f).heightIn(min = 16.dp))

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TimerDial(task, now, min(maxWidth, 288.dp))
    }

    TimerControls(task, now, onPause, onResume, onAddFive, onDone, Modifier.fillMaxWidth().padding(top = 28.dp))

    Spacer(Modifier.weight(1f).heightIn(min = 16.dp))

    TimerInfo(task, now, nextEvent, Modifier.fillMaxWidth())
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CountingWide(
    task: NowTask,
    nextEvent: DayEvent?,
    ringSize: Dp,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddFive: () -> Unit,
    onDone: () -> Unit,
) {
    val c = Tendwell.colors
    val now by rememberTicker(active = task.running)
    val isBreak = task.phase == NowPhase.Break

    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            TimerDial(task, now, ringSize)
            TimerControls(task, now, onPause, onResume, onAddFive, onDone, Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(top = 28.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhaseEyebrow(task)
            Text(
                if (isBreak) "Break" else task.title,
                style = TendwellType.h1,
                color = c.ink,
                maxLines = 3,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).semantics { heading() },
            )
            Text(rangeText(task, now), style = TendwellType.secondary, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            TimerInfo(task, now, nextEvent, Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(top = 32.dp))
        }
    }
}

/** The ring with the time left inside; [size] is 288 dp on most phones. */
@Composable
private fun TimerDial(task: NowTask, now: Long, size: Dp) {
    val c = Tendwell.colors
    val isBreak = task.phase == NowPhase.Break
    val left = task.remainingMs(now)
    val totalMin = (task.totalMs / 60_000).toInt()
    val secs = (left + 999) / 1000
    val clock = "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"
    val up = task.timeIsUp(now)
    // The digits shrink with a small ring so "18:40" still fits inside it.
    val k = (size / 288.dp).coerceAtMost(1f)
    Box(
        Modifier
            .size(size)
            .clearAndSetSemantics {
                contentDescription = when {
                    up -> if (isBreak) "Break's over" else "Time's up"
                    task.running -> "${count(secs / 60, "minute")} ${count(secs % 60, "second")} left of ${count(totalMin, "minute")}"
                    else -> "Paused, ${count(secs / 60, "minute")} left"
                }
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        TimerRing(
            fractionLeft = task.fractionLeft(now),
            track = if (isBreak) c.time else c.primaryLine,
            trackAlpha = if (isBreak) 0.24f else 1f,
            progress = if (isBreak) c.time else c.primary,
            progressAlpha = if (task.running) 1f else 0.55f,
            strokeWidth = 15.dp,
            modifier = Modifier.size(size),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (up) "0:00" else clock,
                style = TendwellType.body.copy(fontSize = 60.sp * k, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", lineHeight = 64.sp * k),
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

/** +5 min, pause or resume, and Done (End for a break). */
@Composable
private fun TimerControls(
    task: NowTask,
    now: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddFive: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tendwell.colors
    val isBreak = task.phase == NowPhase.Break
    val up = task.timeIsUp(now)
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        PillButton("+5 min", null, onAddFive, Modifier.weight(1f))
        val playing = task.running && !up
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(if (isBreak) c.time else c.primary)
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
}

/** The card with the next event and when the nudge comes, or the break note. */
@Composable
private fun TimerInfo(task: NowTask, now: Long, nextEvent: DayEvent?, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (task.phase == NowPhase.Break) {
            InfoLine(TwIcons.Sprout, "Step away from the screen. Back to it at ${clockAt(task.projectedEndMs(now))}.")
        } else {
            nextEvent?.let { InfoLine(TwIcons.Calendar, "Next: ${it.title} at ${TimeBarModel.formatClock(it.startMin)}") }
            InfoLine(
                TwIcons.Bell,
                when {
                    task.timeIsUp(now) -> "Time was up at ${clockAt(task.endAtMs)}"
                    !task.running -> "No nudge while the timer is paused"
                    else -> "A gentle nudge at ${clockAt(task.projectedEndMs(now))}"
                },
            )
        }
    }
}

/** The check that pops in when a task is done, unless animations are off. */
@Composable
private fun DoneBadge() {
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
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(200.dp).scale(scale.value).clip(CircleShape).background(c.primarySoft))
        Box(Modifier.size(132.dp).scale(scale.value).clip(CircleShape).background(c.primaryLine))
        Box(
            Modifier.size(96.dp).scale(scale.value).clip(CircleShape).background(c.primary),
            contentAlignment = Alignment.Center,
        ) { Icon(TwIcons.Check, null, tint = c.onPrimary, modifier = Modifier.size(48.dp)) }
    }
}

@Composable
private fun DoneText(task: NowTask, nextEvent: DayEvent?) {
    val c = Tendwell.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Text("Done. That counts.", style = TendwellType.h1, color = c.ink, textAlign = TextAlign.Center)
        val line = if (task.spentMinutes > 0) "${count(task.spentMinutes, "minute")} on ${task.title}." else "${task.title}: done."
        Text(line, style = TendwellType.body, color = c.ink2, textAlign = TextAlign.Center)
        nextEvent?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(TwIcons.Calendar, null, tint = c.time, modifier = Modifier.size(16.dp))
                Text("Next: ${it.title} at ${TimeBarModel.formatClock(it.startMin)}", style = TendwellType.small, color = c.ink2)
            }
        }
    }
}

@Composable
private fun DoneButtons(onHome: () -> Unit, onStartBreak: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun androidx.compose.foundation.layout.ColumnScope.DoneMoment(
    task: NowTask,
    nextEvent: DayEvent?,
    onHome: () -> Unit,
    onStartBreak: () -> Unit,
) {
    Spacer(Modifier.weight(1f))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        DoneBadge()
        DoneText(task, nextEvent)
    }
    Spacer(Modifier.weight(1f))
    DoneButtons(onHome, onStartBreak, Modifier.fillMaxWidth())
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DoneMomentWide(
    task: NowTask,
    nextEvent: DayEvent?,
    onHome: () -> Unit,
    onStartBreak: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { DoneBadge() }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            DoneText(task, nextEvent)
            DoneButtons(onHome, onStartBreak, Modifier.widthIn(max = 440.dp).fillMaxWidth())
        }
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

/** "1 minute", "2 minutes". */
private fun count(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
