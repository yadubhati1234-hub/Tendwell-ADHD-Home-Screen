package app.tendwell.now

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tendwell.timebar.DayEvent
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * "What are you doing now?" sheet: a task, a duration, and when it would end.
 * Offers today's one thing as a one-tap pick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowSetSheet(
    initialTitle: String,
    oneThing: String?,
    events: List<DayEvent>,
    nowMinute: Int,
    onStart: (title: String, minutes: Int) -> Unit,
    onSaveWithoutTimer: (title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Tendwell.colors
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var minutes by rememberSaveable { mutableIntStateOf(NowTask.DEFAULT_MINUTES) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hasTitle = title.isNotBlank()
    val endMin = nowMinute + minutes
    val clash = events.firstOrNull { it.startMin in (nowMinute + 1) until endMin }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.scrim,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).width(36.dp).height(4.dp).background(c.line, RoundedCornerShape(999.dp)))
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
        ) {
            Text("What are you doing now?", style = TendwellType.h1.copy(fontSize = 28.sp, letterSpacing = (-0.3).sp), color = c.ink)

            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, if (hasTitle || focused) c.primary else c.line, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (title.isEmpty()) {
                    Text("Type a task", style = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium), color = c.ink3, modifier = Modifier.padding(start = 18.dp))
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    singleLine = true,
                    textStyle = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (hasTitle) onStart(title, minutes) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 52.dp)
                        .semantics { contentDescription = "Task for now" },
                )
                if (title.isNotEmpty()) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Clear") { title = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(c.surface2), contentAlignment = Alignment.Center) {
                            Icon(TwIcons.Close, contentDescription = "Clear", tint = c.ink2, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (!oneThing.isNullOrBlank()) {
                val on = title.trim().equals(oneThing.trim(), ignoreCase = true)
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) c.primary else c.primarySoft)
                        .clickable(role = Role.Button) { title = oneThing }
                        .padding(start = 14.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val fg = if (on) c.onPrimary else c.onPrimarySoft
                    Icon(TwIcons.Target, null, tint = fg, modifier = Modifier.size(18.dp))
                    Text(
                        buildAnnotatedString {
                            append("Your one thing: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(oneThing) }
                        },
                        style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold),
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text("For how long?".uppercase(), style = TendwellType.eyebrow, color = c.ink3, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NowTask.DURATIONS.forEach { n ->
                    val selected = n == minutes
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) c.primary else c.surface2)
                            .selectable(selected = selected, role = Role.RadioButton) { minutes = n }
                            .semantics { contentDescription = "$n minutes" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    ) {
                        val fg = if (selected) c.onPrimary else c.ink2
                        Text("$n", style = TendwellType.rowLabel.copy(fontWeight = FontWeight.Bold), color = fg)
                        Text("min", style = TendwellType.eyebrow.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp), color = fg.copy(alpha = 0.8f))
                    }
                }
            }

            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(TwIcons.Clock, null, tint = c.time, modifier = Modifier.size(20.dp))
                Text(
                    buildAnnotatedString {
                        append("Ends at ${TimeBarModel.formatClock(endMin)}")
                        if (clash != null) {
                            withStyle(SpanStyle(color = c.ink3, fontWeight = FontWeight.Medium, fontSize = 14.sp)) {
                                append(" · after your ${clash.title} at ${TimeBarModel.formatClock(clash.startMin)}")
                            }
                        }
                    },
                    style = TendwellType.secondary,
                    color = c.ink,
                )
            }

            Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (hasTitle) c.primary else c.surface2)
                        .clickable(enabled = hasTitle, role = Role.Button) { onStart(title, minutes) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    val fg = if (hasTitle) c.onPrimary else c.ink3
                    Icon(TwIcons.Play, null, tint = fg, modifier = Modifier.size(18.dp))
                    Text("Start timer", style = TendwellType.button.copy(fontSize = 16.sp), color = fg)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = hasTitle, role = Role.Button) { onSaveWithoutTimer(title) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save without a timer", style = TendwellType.secondary, color = if (hasTitle) c.ink2 else c.ink3)
                }
            }
        }
    }
}
