package app.tendwell.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tendwell.ui.components.ProBadge
import app.tendwell.ui.components.ReadableWidth
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.components.WideMaxWidth
import app.tendwell.ui.components.isWide
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Weekly insights (Pro): what happened at the pause screen this week.
 * On a wide window ([isWide]) the cards sit in two columns.
 */
@Composable
fun InsightsScreen(
    weekStart: LocalDate,
    today: LocalDate,
    stats: WeekStats?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    appLabel: (String) -> String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSetFocusHours: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Tendwell.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        val wide = isWide(maxWidth, maxHeight)
        Column(
            Modifier
                .widthIn(max = if (wide) WideMaxWidth else ReadableWidth)
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        InsightsTitle(onBack)
                        when {
                            stats == null -> Unit
                            stats.isEmpty -> EmptyWeekCard()
                            else -> {
                                TurnedBackCard(stats)
                                OpensCard(stats, today)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        WeekSwitcher(weekStart, today, canGoBack, canGoForward, onPrevWeek, onNextWeek)
                        if (stats != null && !stats.isEmpty) {
                            DriftCard(stats, onSetFocusHours)
                            if (stats.byApp.isNotEmpty()) ByAppCard(stats, appLabel)
                        }
                        AboutCounts()
                        OnPhoneNote()
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InsightsTitle(onBack)
                    WeekSwitcher(weekStart, today, canGoBack, canGoForward, onPrevWeek, onNextWeek)
                }
                when {
                    stats == null -> Unit
                    stats.isEmpty -> EmptyWeekCard()
                    else -> {
                        TurnedBackCard(stats)
                        OpensCard(stats, today)
                        DriftCard(stats, onSetFocusHours)
                        if (stats.byApp.isNotEmpty()) ByAppCard(stats, appLabel)
                    }
                }
                AboutCounts()
                OnPhoneNote()
            }
        }
    }
}

@Composable
private fun InsightsTitle(onBack: () -> Unit) {
    Row(Modifier.offset(x = (-10).dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TwIconButton(TwIcons.ChevronLeft, contentDescription = "Back", onClick = onBack)
        Text("Your week", style = TendwellType.h1, color = Tendwell.colors.ink, modifier = Modifier.semantics { heading() })
        Box(Modifier.padding(start = 10.dp)) { ProBadge() }
    }
}

@Composable
private fun WeekSwitcher(
    weekStart: LocalDate,
    today: LocalDate,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val c = Tendwell.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WeekButton(TwIcons.ChevronLeft, "Previous week", canGoBack, onPrevWeek)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(WeekInsights.weekName(weekStart, today), style = TendwellType.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = c.ink3)
            Text(WeekInsights.weekRange(weekStart), style = TendwellType.secondary.copy(fontWeight = FontWeight.Bold), color = c.ink)
        }
        WeekButton(TwIcons.ChevronRight, "Next week", canGoForward, onNextWeek)
    }
}

@Composable
private fun EmptyWeekCard() {
    Card {
        Text(
            "Nothing to show for this week yet. This fills in as your paused apps meet the pause screen.",
            style = TendwellType.small.copy(lineHeight = 20.sp),
            color = Tendwell.colors.ink2,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun AboutCounts() {
    val c = Tendwell.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface2)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(TwIcons.Info, contentDescription = null, tint = c.ink2, modifier = Modifier.size(20.dp))
            Text("About these counts", style = TendwellType.secondary.copy(fontWeight = FontWeight.Bold), color = c.ink, modifier = Modifier.semantics { heading() })
        }
        Text(
            "These counts include paused apps opened from your Home screen and search.",
            style = TendwellType.small.copy(lineHeight = 20.sp),
            color = c.ink2,
        )
    }
}

@Composable
private fun OnPhoneNote() {
    val c = Tendwell.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(TwIcons.Shield, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
        Text("Everything stays on this phone.", style = TendwellType.caption, color = c.ink3)
    }
}

@Composable
private fun WeekButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Tendwell.colors
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = label
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) c.ink else c.ink3, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val c = Tendwell.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (c.isDark) Modifier else Modifier.shadow(6.dp, shape, ambientColor = c.ink, spotColor = c.ink))
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.cardLine, shape),
        content = content,
    )
}

/** A small pill like "6 more than last week". */
@Composable
private fun ChangePill(text: String, up: Boolean, background: Color) {
    val c = Tendwell.colors
    Row(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (up) TwIcons.ArrowUp else TwIcons.ArrowDown, contentDescription = null, tint = c.onPrimarySoft, modifier = Modifier.size(14.dp))
        Text(text, style = TendwellType.caption.copy(fontWeight = FontWeight.Bold), color = c.onPrimarySoft)
    }
}

@Composable
private fun TurnedBackCard(s: WeekStats) {
    val c = Tendwell.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.primarySoft)
            .border(1.5.dp, c.primaryLine, shape)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${s.turnedBack}",
                    style = TendwellType.clock.copy(fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-2).sp, fontFeatureSettings = "tnum"),
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(52.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                    Icon(TwIcons.Sprout, contentDescription = null, tint = c.primary, modifier = Modifier.size(32.dp))
                }
            }
            Text(
                if (s.turnedBack == 1) "time you turned back at the pause" else "times you turned back at the pause",
                style = TendwellType.body.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
                color = c.onPrimarySoft,
            )
        }
        WeekInsights.turnedBackChange(s)?.let { ChangePill(it, up = s.turnedBack >= s.prevTurnedBack, background = c.surface) }
    }
}

@Composable
private fun OpensCard(s: WeekStats, today: LocalDate) {
    val c = Tendwell.colors
    val max = (s.opensPerDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val initials = (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    val fullNames = (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("Opens of paused apps", style = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = c.ink, modifier = Modifier.weight(1f).semantics { heading() })
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append("${s.opens}") }
                        append(" this week")
                    },
                    style = TendwellType.secondary.copy(fontWeight = FontWeight.Medium),
                    color = c.ink2,
                )
            }
            WeekInsights.opensChange(s)?.let { ChangePill(it, up = s.opens > s.prevOpens, background = c.primarySoft) }
            val description = "Opens of paused apps per day: " +
                fullNames.zip(s.opensPerDay).joinToString(", ") { (d, v) -> "$d $v" } + ". ${s.opens} in total."
            Column(Modifier.padding(top = 8.dp).clearAndSetSemantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().height(126.dp), verticalAlignment = Alignment.Bottom) {
                    s.opensPerDay.forEach { v ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$v", style = TendwellType.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = c.ink2)
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .height((100f * v / max).coerceAtLeast(if (v > 0) 4f else 0f).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(c.primary),
                            )
                        }
                    }
                }
                HorizontalDivider(Modifier.offset(y = (-8).dp), color = c.line)
                Row(Modifier.fillMaxWidth()) {
                    initials.forEach {
                        Text(it, style = TendwellType.caption.copy(fontSize = 12.sp), color = c.ink3, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
            }
            WeekInsights.calmestDays(s, today)?.let {
                Text(it, style = TendwellType.small.copy(lineHeight = 20.sp), color = c.ink2, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun DriftCard(s: WeekStats, onSetFocusHours: () -> Unit) {
    val c = Tendwell.colors
    val top = WeekInsights.driftSlot(s)
    val max = (s.opensPerSlot.maxOrNull() ?: 0).coerceAtLeast(1)
    Card {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("When you drift", style = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = c.ink, modifier = Modifier.weight(1f).semantics { heading() })
                Text("Opens by time of day", style = TendwellType.caption, color = c.ink3)
            }
            val description = "Opens of paused apps by time of day: " +
                WeekInsights.SLOT_LABELS.zip(s.opensPerSlot).joinToString(". ") { (l, v) -> "${l.replace("–", " to ")}, $v" } + "."
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.opensPerSlot.forEachIndexed { i, v ->
                    val isTop = i == top
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$v", style = TendwellType.secondary.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = if (isTop) c.ink else c.ink2)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.surface2),
                        ) {
                            Box(Modifier.fillMaxSize().alpha((v.toFloat() / max).coerceAtLeast(0f)).background(c.time))
                        }
                        Text(
                            WeekInsights.SLOT_LABELS[i],
                            style = TendwellType.caption.copy(fontSize = 12.sp, fontWeight = if (isTop) FontWeight.Bold else FontWeight.Medium, fontFeatureSettings = "tnum"),
                            color = if (isTop) c.ink else c.ink3,
                        )
                    }
                }
            }
            if (top != null) {
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(WeekInsights.driftSentence(top), style = TendwellType.cardTitle, color = c.ink)
                    Text(
                        "Focus hours or an earlier bedtime on the time bar can help here.",
                        style = TendwellType.small.copy(lineHeight = 20.sp),
                        color = c.ink2,
                    )
                    Row(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button, onClick = onSetFocusHours),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Set focus hours", style = TendwellType.button, color = c.primary)
                        Icon(TwIcons.ArrowRight, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ByAppCard(s: WeekStats, appLabel: (String) -> String) {
    val c = Tendwell.colors
    val bubbles = listOf(c.peach, c.lilac, c.sky, c.mint, c.butter)
    val apps = s.byApp.take(5)
    val max = (apps.maxOfOrNull { it.opens } ?: 0).coerceAtLeast(1)
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("By app", style = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = c.ink, modifier = Modifier.semantics { heading() })
            apps.forEachIndexed { i, a ->
                val name = appLabel(a.appKey)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(bubbles[i % bubbles.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.take(1).uppercase(), style = TendwellType.body.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Text(name, style = TendwellType.body.copy(fontWeight = FontWeight.Bold, lineHeight = 22.sp), color = c.ink, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(
                                if (a.opens == 1) "1 open" else "${a.opens} opens",
                                style = TendwellType.secondary.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                                color = c.ink,
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(c.surface2)) {
                            Box(Modifier.fillMaxWidth(a.opens.toFloat() / max).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(c.primary))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(TwIcons.Sprout, contentDescription = null, tint = c.primary, modifier = Modifier.size(14.dp))
                            Text(
                                if (a.turnedBack == 1) "Turned back once" else "Turned back ${a.turnedBack} times",
                                style = TendwellType.caption.copy(lineHeight = 18.sp),
                                color = c.ink3,
                            )
                        }
                    }
                }
            }
        }
    }
}
