package app.tendwell.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tendwell.data.OneThing
import app.tendwell.timebar.TimeBar
import app.tendwell.timebar.TimeBarModel
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tendwell.apps.AppEntry
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home: date, big clock, the essential apps as a text list, and the search pill.
 * The time bar, Now card and daily check-in slot in between the clock and the
 * app list in the next feature PR.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    essentials: List<AppEntry>,
    loaded: Boolean,
    timeBar: TimeBarModel?,
    oneThing: OneThing?,
    nowCard: @Composable () -> Unit,
    onToggleOneThing: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Tendwell.colors
    val now by rememberCurrentMinute()
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val clockFormat = remember { DateTimeFormatter.ofPattern("H:mm", Locale.getDefault()) }
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)
            // Swipe up anywhere on Home to search.
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -swipeThreshold) onOpenSearch() },
                )
            },
    ) {
        // The top part scrolls on short screens; the app list and search stay put at the bottom.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(now.format(dateFormat), style = TendwellType.secondary, color = c.ink2)
                TwIconButton(
                    icon = TwIcons.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    tint = c.ink2,
                    iconSize = 22.dp,
                    modifier = Modifier.offset(x = 10.dp),
                )
            }
            Text(
                text = now.format(clockFormat),
                style = TendwellType.clock,
                color = c.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (timeBar != null) {
                TimeBar(timeBar, Modifier.padding(top = 20.dp))
            }
            Box(Modifier.padding(top = 20.dp)) { nowCard() }
            OneThingRow(oneThing, onToggleOneThing, onOpenCheckIn, Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(color = c.line)
        Column(Modifier.padding(top = 10.dp)) {
            if (loaded && essentials.isEmpty()) {
                Text(
                    "Your essential apps go here. Long-press any app in search to add it.",
                    style = TendwellType.body,
                    color = c.ink3,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            essentials.forEach { app ->
                Text(
                    text = app.label,
                    style = TendwellType.homeApp,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = "Open",
                            onLongClickLabel = "Options",
                            onLongClick = { onLongPress(app) },
                            onClick = { onLaunch(app) },
                        )
                        .padding(vertical = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface2)
                .clickable(onClick = onOpenSearch, role = Role.Button)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(TwIcons.Search, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
            Text("Search apps", style = TendwellType.body, color = c.ink3)
        }
    }
}

/** "One thing today" under the Now card: tap to tick it off. */
@Composable
private fun OneThingRow(oneThing: OneThing?, onToggle: () -> Unit, onOpenCheckIn: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    if (oneThing == null) {
        Row(
            modifier = modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onOpenCheckIn),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(TwIcons.Target, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
            Text("Set your one thing for today", style = TendwellType.secondary, color = c.ink2)
        }
        return
    }
    val done = oneThing.done
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { stateDescription = if (done) "Done" else "Not done yet" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) c.primary else c.bg)
                .border(2.dp, if (done) c.primary else c.ink3, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(TwIcons.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(14.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("One thing today", style = TendwellType.eyebrow.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), color = c.ink3)
            Text(
                oneThing.text,
                style = TendwellType.rowLabel.copy(fontSize = 17.sp, textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None),
                color = c.ink,
            )
        }
    }
}
