package app.tendwell.focus

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tendwell.data.Prefs
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.ProBadge
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import java.time.LocalDateTime

/** Focus hours (Pro): longer pauses on paused apps during set hours. */
@Composable
fun FocusHoursScreen(
    blocks: List<FocusBlock>,
    now: LocalDateTime,
    pausedNames: List<String>,
    onChange: (List<FocusBlock>) -> Unit,
    onPickTime: (currentMin: Int, onPicked: (Int) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val c = Tendwell.colors
    var expandedId by rememberSaveable { mutableStateOf(blocks.firstOrNull()?.id) }
    fun update(b: FocusBlock) = onChange(blocks.map { if (it.id == b.id) b else it })

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.offset(x = (-10).dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TwIconButton(TwIcons.ChevronLeft, contentDescription = "Back to Settings", onClick = onBack)
                Text("Focus hours", style = TendwellType.h1, color = c.ink, modifier = Modifier.semantics { heading() })
                Box(Modifier.padding(start = 10.dp)) { ProBadge() }
            }
            Text(
                "During these hours, your paused apps wait longer before they open.",
                style = TendwellType.body.copy(fontWeight = FontWeight.Medium),
                color = c.ink2,
            )
        }

        StatusCard(blocks, now)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            blocks.forEach { b ->
                BlockCard(
                    block = b,
                    expanded = expandedId == b.id,
                    pausedNames = pausedNames,
                    onExpand = { expandedId = if (expandedId == b.id) null else b.id },
                    onChange = ::update,
                    onPickTime = onPickTime,
                    onRemove = { onChange(blocks.filterNot { it.id == b.id }) },
                )
            }
        }

        val suggestions = FocusHours.presets.filter { p -> blocks.none { it.id == p.id } }
        if (suggestions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "SUGGESTED",
                    style = TendwellType.eyebrow,
                    color = c.ink3,
                    modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
                )
                suggestions.forEach { p ->
                    PresetCard(p) {
                        onChange(blocks + p)
                        expandedId = p.id
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface2)
                .clickable(role = Role.Button) {
                    val b = FocusBlock("custom-${System.currentTimeMillis()}", "Focus hours", true, FocusHours.WEEKDAYS, 9 * 60, 12 * 60, 30)
                    onChange(blocks + b)
                    expandedId = b.id
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            Icon(TwIcons.Plus, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
            Text("Add focus hours", style = TendwellType.button, color = c.ink)
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Tendwell.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(if (c.isDark) Modifier else Modifier.shadow(6.dp, shape, ambientColor = c.ink, spotColor = c.ink))
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.cardLine, shape),
        content = content,
    )
}

/** "On now, until 17:00" with today's focus hours drawn on a 24-hour line. */
@Composable
private fun StatusCard(blocks: List<FocusBlock>, now: LocalDateTime) {
    val c = Tendwell.colors
    val status = FocusHours.status(blocks, now)
    val segments = FocusHours.segmentsOn(blocks, now.toLocalDate())
    val nowMin = now.hour * 60 + now.minute
    val (label, aside) = when (status) {
        is FocusStatus.On -> "On now, until ${TimeBarModel.formatClock(status.untilMin % 1440)}" to "${FocusHours.durationLabel(status.minutesLeft)} to go"
        is FocusStatus.Next -> "Next: ${status.block.name} at ${TimeBarModel.formatClock(status.startMin)}" to "in ${FocusHours.durationLabel(status.minutesUntil)}"
        FocusStatus.Off -> "Off for today" to ""
    }
    val pulse = rememberInfiniteTransition(label = "focusDot")
    val dotAlpha by pulse.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "focusDotAlpha")
    Card(Modifier) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .alpha(if (status is FocusStatus.On) dotAlpha else 1f)
                        .clip(CircleShape)
                        .background(if (status is FocusStatus.On) c.time else c.ink3),
                )
                Text(label, style = TendwellType.body.copy(fontWeight = FontWeight.Bold), color = c.ink, modifier = Modifier.weight(1f))
                if (aside.isNotEmpty()) Text(aside, style = TendwellType.caption.copy(fontFeatureSettings = "tnum"), color = c.ink3)
            }
            val description = buildString {
                append("Today from 0:00 to 24:00. It is ${TimeBarModel.formatClock(nowMin)}. ")
                if (segments.isEmpty()) append("No focus hours today.")
                else append("Focus hours: " + segments.joinToString(", ") { (a, b) -> "${TimeBarModel.formatClock(a)} to ${TimeBarModel.formatClock(b % 1440)}" } + ".")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .semantics { contentDescription = description },
                ) {
                    val w = size.width
                    val trackTop = 7.dp.toPx()
                    val trackH = 12.dp.toPx()
                    val r = CornerRadius(trackH / 2, trackH / 2)
                    fun x(min: Int) = w * min / 1440f
                    drawRoundRect(c.surface2, Offset(0f, trackTop), Size(w, trackH), r)
                    drawRoundRect(c.past, Offset(0f, trackTop), Size(x(nowMin), trackH), r)
                    segments.forEach { (a, b) ->
                        val active = nowMin in a until b
                        drawRect(c.time, Offset(x(a), trackTop), Size(x(b) - x(a), trackH), alpha = if (active || nowMin < a) 1f else 0.45f)
                    }
                    val markerW = 4.dp.toPx()
                    drawRoundRect(c.surface, Offset(x(nowMin) - markerW / 2 - 3.dp.toPx(), 0f), Size(markerW + 6.dp.toPx(), size.height), CornerRadius(markerW, markerW))
                    drawRoundRect(c.ink, Offset(x(nowMin) - markerW / 2, 0f), Size(markerW, size.height), CornerRadius(markerW / 2, markerW / 2))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("0:00", "6:00", "12:00", "18:00", "24:00").forEach {
                        Text(it, style = TendwellType.caption.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"), color = c.ink3)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockCard(
    block: FocusBlock,
    expanded: Boolean,
    pausedNames: List<String>,
    onExpand: () -> Unit,
    onChange: (FocusBlock) -> Unit,
    onPickTime: (Int, (Int) -> Unit) -> Unit,
    onRemove: () -> Unit,
) {
    val c = Tendwell.colors
    Card(Modifier.animateContentSize()) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide settings" else "Edit", onClick = onExpand),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(block.name, style = TendwellType.cardTitle.copy(fontSize = 24.sp, lineHeight = 1.15.em), color = c.ink, modifier = Modifier.semantics { heading() })
                Text(
                    FocusHours.summary(block),
                    style = TendwellType.small.copy(fontFeatureSettings = "tnum"),
                    color = c.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FocusSwitch(block.on, label = block.name) { onChange(block.copy(on = it)) }
        }
        if (expanded) {
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = c.line)
            Column(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                    .alpha(if (block.on) 1f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DayChips(block.days) { onChange(block.copy(days = it)) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeButton("From", block.startMin, Modifier.weight(1f)) {
                        onPickTime(block.startMin) { onChange(block.copy(startMin = it)) }
                    }
                    Icon(TwIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
                    TimeButton("To", block.endMin, Modifier.weight(1f)) {
                        onPickTime(block.endMin) { onChange(block.copy(endMin = it)) }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text("Pause length", style = TendwellType.rowLabel, color = c.ink, modifier = Modifier.weight(1f))
                        Text("Usually ${Prefs.PAUSE_SECONDS} s", style = TendwellType.caption, color = c.ink3)
                    }
                    Segmented(
                        options = FocusHours.PAUSE_OPTIONS,
                        selected = block.pauseSeconds,
                        label = FocusHours::pauseLabel,
                        onPick = { onChange(block.copy(pauseSeconds = it)) },
                    )
                }
                Text(
                    if (pausedNames.isEmpty()) "No paused apps yet. Long-press an app to pause it."
                    else "Applies to your paused apps: ${pausedNames.joinToString(", ")}",
                    style = TendwellType.small,
                    color = c.ink2,
                )
            }
            Box(
                Modifier
                    .padding(start = 8.dp, bottom = 8.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(role = Role.Button, onClick = onRemove)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Remove", style = TendwellType.secondary, color = c.ink2)
            }
        }
    }
}

@Composable
private fun FocusSwitch(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val c = Tendwell.colors
    val knobX by animateDpAsState(if (checked) 24.dp else 4.dp, label = "knob")
    Box(
        Modifier
            .size(width = 52.dp, height = 44.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) c.primary else c.surface2)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, c.line, RoundedCornerShape(999.dp))),
        ) {
            Box(
                Modifier
                    .offset(x = knobX, y = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (checked) c.onPrimary else c.ink3),
            )
        }
    }
}

@Composable
private fun DayChips(days: Int, onChange: (Int) -> Unit) {
    val c = Tendwell.colors
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    val names = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    Row(Modifier.fillMaxWidth().offset(x = (-4).dp), horizontalArrangement = Arrangement.SpaceBetween) {
        letters.forEachIndexed { i, letter ->
            val bit = 1 shl i
            val on = days and bit != 0
            Box(
                Modifier
                    .size(44.dp)
                    .toggleable(value = on, role = Role.Checkbox) { onChange(if (it) days or bit else days and bit.inv()) }
                    .semantics { contentDescription = names[i] },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(if (on) c.primary else c.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, style = TendwellType.small.copy(fontWeight = FontWeight.Bold), color = if (on) c.onPrimary else c.ink2)
                }
            }
        }
    }
}

@Composable
private fun TimeButton(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = Tendwell.colors
    Column(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface2)
            .clickable(role = Role.Button, onClickLabel = "Change $label time", onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = TendwellType.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = c.ink3)
        Text(
            TimeBarModel.formatClock(minutes),
            style = TendwellType.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = c.ink,
        )
    }
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    val c = Tendwell.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(c.surface2)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) c.surface else c.surface2)
                    .then(if (on && c.isDark) Modifier.border(1.dp, c.line, RoundedCornerShape(999.dp)) else Modifier)
                    .selectable(selected = on, role = Role.RadioButton) { onPick(o) }
                    .semantics { stateDescription = if (on) "Selected" else "" },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = TendwellType.small.copy(fontWeight = FontWeight.Bold), color = if (on) c.ink else c.ink2)
            }
        }
    }
}

@Composable
private fun PresetCard(preset: FocusBlock, onAdd: () -> Unit) {
    val c = Tendwell.colors
    val morning = preset.startMin < 12 * 60
    Card {
        Row(
            Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (morning) c.butter else c.lilac),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (morning) TwIcons.Sun else TwIcons.Moon, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(preset.name, style = TendwellType.body.copy(fontWeight = FontWeight.Bold, lineHeight = 22.sp), color = c.ink)
                Text(
                    "${FocusHours.range(preset)} · ${FocusHours.daysLabel(preset.days)}",
                    style = TendwellType.small.copy(fontFeatureSettings = "tnum"),
                    color = c.ink2,
                )
            }
            TwIconButton(TwIcons.Plus, contentDescription = "Add ${preset.name}", onClick = onAdd, modifier = Modifier.clip(CircleShape).background(c.surface2))
        }
    }
}
