package app.tendwell.checkin

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A quick pick on the check-in screen, like yesterday's unfinished one thing. */
data class CheckInPick(val label: String, val note: String? = null)

/** "What's your one thing today?", shown the first time Home opens each day. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(
    now: LocalDateTime,
    picks: List<CheckInPick>,
    onSet: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val c = Tendwell.colors
    var text by rememberSaveable { mutableStateOf("") }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ready = text.isNotBlank()
    val greeting = when (now.hour) {
        in 4..11 -> "Good morning."
        in 12..17 -> "Good afternoon."
        else -> "Good evening."
    }
    val date = remember(now.toLocalDate()) { now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(date, style = TendwellType.secondary, color = c.ink3, modifier = Modifier.weight(1f))
                TwIconButton(TwIcons.Close, contentDescription = "Skip for today", onClick = onSkip, tint = c.ink2, iconSize = 22.dp, modifier = Modifier.offset(x = 10.dp))
            }
            Sunrise(Modifier.padding(top = 4.dp))

            Text(greeting, style = TendwellType.cardTitle, color = c.ink2, modifier = Modifier.padding(top = 16.dp))
            Text(
                "What's your one thing today?",
                style = TendwellType.h1.copy(fontSize = 37.sp, letterSpacing = (-0.6).sp),
                color = c.ink,
                modifier = Modifier.padding(top = 6.dp).semantics { heading() },
            )
            Text(
                "The one thing that would make today feel like a win. Small is fine.",
                style = TendwellType.body.copy(fontWeight = FontWeight.Medium),
                color = c.ink2,
                modifier = Modifier.padding(top = 12.dp),
            )

            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
                    .clip(shape)
                    .background(c.surface)
                    .border(1.5.dp, if (ready || focused) c.primary else c.line, shape)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                val fieldStyle = TendwellType.cardTitle.copy(lineHeight = 29.sp)
                if (text.isEmpty()) Text("Type it in your own words", style = fieldStyle, color = c.ink3)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.replace("\n", " ").take(120) },
                    textStyle = fieldStyle.copy(color = c.ink),
                    cursorBrush = SolidColor(c.primary),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Your one thing" },
                )
            }

            if (picks.isNotEmpty()) {
                Text(
                    "Or pick up where you left off".uppercase(),
                    style = TendwellType.eyebrow,
                    color = c.ink3,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    picks.forEach { pick ->
                        val on = text.trim().equals(pick.label, ignoreCase = true)
                        Row(
                            Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (on) c.primary else c.surface2)
                                .clickable(role = Role.Button) { text = pick.label }
                                .semantics { selected = on }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(pick.label, style = TendwellType.button, color = if (on) c.onPrimary else c.ink2)
                            pick.note?.let {
                                Text(it, style = TendwellType.caption, color = if (on) c.onPrimary.copy(alpha = 0.82f) else c.ink3)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (ready) c.primary else c.surface2)
                .clickable(enabled = ready, role = Role.Button) { onSet(text) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            val fg = if (ready) c.onPrimary else c.ink3
            Icon(TwIcons.Target, null, tint = fg, modifier = Modifier.size(18.dp))
            Text("Set my one thing", style = TendwellType.button.copy(fontSize = 16.sp), color = fg)
        }
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onSkip),
            contentAlignment = Alignment.Center,
        ) {
            Text("Skip today", style = TendwellType.secondary, color = c.ink2)
        }
    }
}

/** The small half-sun over the horizon from the mockup. Decorative. */
@Composable
private fun Sunrise(modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    val rayAlpha = if (c.isDark) 0.6f else 0.45f
    Canvas(modifier.size(92.dp, 54.dp)) {
        val u = size.width / 92f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        drawArc(c.butter, 180f, 180f, true, p(14f, 14f), Size(64f * u, 64f * u))
        drawArc(c.peach, 180f, 180f, true, p(27f, 27f), Size(38f * u, 38f * u))
        val ray = Stroke(2.25f * u, cap = StrokeCap.Round)
        listOf(
            p(46f, 3.5f) to p(46f, 9f),
            p(17.6f, 15.6f) to p(21.5f, 19.5f),
            p(74.4f, 15.6f) to p(70.5f, 19.5f),
            p(3.5f, 35f) to p(9f, 35f),
            p(88.5f, 35f) to p(83f, 35f),
        ).forEach { (a, b) -> drawLine(c.ink3, a, b, strokeWidth = ray.width, cap = StrokeCap.Round, alpha = rayAlpha) }
        drawLine(c.line, p(2f, 46.25f), p(90f, 46.25f), strokeWidth = 1.5f * u, cap = StrokeCap.Round)
    }
}
