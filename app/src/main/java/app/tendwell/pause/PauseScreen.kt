package app.tendwell.pause

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tendwell.apps.AppEntry
import app.tendwell.data.Prefs
import app.tendwell.day.rememberTicker
import app.tendwell.ui.components.ReadableWidth
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.components.WideMaxWidth
import app.tendwell.ui.components.isWide
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * The 10-second pause before a paused app opens: a breathing circle, the
 * user's task, "Back to my task" as the main action and "Open anyway" once
 * the pause is over. Kind friction, never a block.
 */
@Composable
fun PauseScreen(
    app: AppEntry,
    openNumber: Int?,
    task: String?,
    onBack: () -> Unit,
    onOpenAnyway: () -> Unit,
    /** Usually 10; longer during focus hours. */
    seconds: Int = Prefs.PAUSE_SECONDS,
    /** "Focus hours until 17:00" while focus hours make the pause longer. */
    focusNote: String? = null,
) {
    val c = Tendwell.colors
    val context = LocalContext.current
    val startedAt = rememberSaveable(app.key) { System.currentTimeMillis() }
    val now by rememberTicker(active = true, periodMs = 250L)
    val elapsedS = ((now - startedAt) / 1000).toInt().coerceAtLeast(0)
    val left = (seconds - elapsedS).coerceAtLeast(0)
    val done = left == 0
    val inhale = (elapsedS / 4) % 2 == 0

    val animationsOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val breath = if (animationsOn) {
        val transition = rememberInfiniteTransition(label = "breathe")
        val s by transition.animateFloat(
            initialValue = 0.78f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breatheScale",
        )
        s
    } else 1f

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        if (isWide(maxWidth, maxHeight)) {
            // Tablet held sideways: the breathing circle on the left, the app,
            // the task and the choices on the right.
            val circle = (maxHeight - 200.dp).coerceIn(160.dp, 280.dp)
            Row(
                Modifier
                    .widthIn(max = WideMaxWidth)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BreathingCircle(circle, breath, left, done)
                    BreathLabel(done, inhale, Modifier.padding(top = 28.dp))
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PauseHeader(app, openNumber, focusNote)
                    if (!task.isNullOrBlank()) TaskCard(task, Modifier.padding(top = 36.dp))
                    PauseActions(app, task, left, done, onBack, onOpenAnyway, Modifier.padding(top = 40.dp).widthIn(max = 440.dp))
                }
            }
        } else {
            Column(
                Modifier
                    .widthIn(max = ReadableWidth)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PauseHeader(app, openNumber, focusNote)
                Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
                BreathingCircle(280.dp, breath, left, done)
                BreathLabel(done, inhale, Modifier.padding(top = 28.dp))
                if (!task.isNullOrBlank()) TaskCard(task, Modifier.padding(top = 24.dp))
                Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                PauseActions(app, task, left, done, onBack, onOpenAnyway)
            }
        }
    }
}

@Composable
private fun PauseHeader(app: AppEntry, openNumber: Int?, focusNote: String?) {
    val c = Tendwell.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(TwIcons.Hourglass, null, tint = c.ink2, modifier = Modifier.size(20.dp))
            Text(app.label, style = TendwellType.rowLabel.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (focusNote != null) {
            Text(
                focusNote,
                style = TendwellType.caption.copy(fontWeight = FontWeight.Bold),
                color = c.onTimeSoft,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.timeSoft)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        if (openNumber != null) {
            Text(
                "${ordinal(openNumber)} open today",
                style = TendwellType.caption.copy(fontWeight = FontWeight.Bold),
                color = c.ink2,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.surface2)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/** The breathing rings with the seconds left in the middle; [size] is the outer ring (280 dp on phones). */
@Composable
private fun BreathingCircle(size: Dp, breath: Float, left: Int, done: Boolean) {
    val c = Tendwell.colors
    val k = size / 280.dp
    Box(
        Modifier
            .size(size)
            .clearAndSetSemantics {
                contentDescription = if (done) "Your pause is over" else (if (left == 1) "1 second" else "$left seconds") + " left in your pause"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).scale(breath).alpha(if (c.isDark) 0.55f else 0.5f).clip(CircleShape).background(c.primarySoft))
        Box(Modifier.size(236.dp * k).scale(breath).clip(CircleShape).background(c.primarySoft))
        Box(Modifier.size(192.dp * k).scale(breath).alpha(if (c.isDark) 0.8f else 0.75f).clip(CircleShape).background(c.primaryLine))
        Column(
            Modifier
                .size(150.dp * k)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.cardLine, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val digits = 64.sp * k
            Text("$left", style = TendwellType.clock.copy(fontSize = digits, letterSpacing = (-1.5).sp, lineHeight = digits, fontFeatureSettings = "tnum"), color = c.ink)
            Text("seconds", style = TendwellType.eyebrow.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp), color = c.ink3)
        }
    }
}

@Composable
private fun BreathLabel(done: Boolean, inhale: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.height(34.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
        Text(
            when {
                done -> "Still want to go in?"
                inhale -> "Breathe in"
                else -> "Breathe out"
            },
            style = TendwellType.cardTitle.copy(fontSize = 26.sp),
            color = Tendwell.colors.ink,
        )
    }
}

@Composable
private fun TaskCard(task: String, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Column(
        modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.primarySoft)
            .border(1.5.dp, c.primaryLine, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(c.primary))
            Text("YOUR TASK", style = TendwellType.eyebrow, color = c.onPrimarySoft)
        }
        Text(task, style = TendwellType.rowLabel.copy(fontWeight = FontWeight.Bold), color = c.ink, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PauseActions(
    app: AppEntry,
    task: String?,
    left: Int,
    done: Boolean,
    onBack: () -> Unit,
    onOpenAnyway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tendwell.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.primary)
                .clickable(role = Role.Button, onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            Icon(TwIcons.Undo, null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
            Text(if (task.isNullOrBlank()) "Back to Home" else "Back to my task", style = TendwellType.button.copy(fontSize = 16.sp), color = c.onPrimary)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (done) c.bg else c.surface2)
                .border(1.5.dp, if (done) c.line else c.surface2, RoundedCornerShape(999.dp))
                .clickable(enabled = done, role = Role.Button, onClick = onOpenAnyway)
                .semantics { if (!done) disabled() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (done) "Open ${app.label} anyway" else "Open anyway in $left s",
                style = TendwellType.button.copy(fontFeatureSettings = "tnum"),
                color = if (done) c.ink else c.ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}
