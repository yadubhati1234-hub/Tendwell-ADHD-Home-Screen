package app.tendwell.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tendwell.apps.AppEntry
import app.tendwell.data.OneThing
import app.tendwell.timebar.TimeBar
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.ReadableWidth
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.components.WideMaxWidth
import app.tendwell.ui.components.isWide
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home: date, big clock, time bar, Now card, the one thing, the essential
 * apps as a text list, and the search pill.
 *
 * On a wide window (a tablet held sideways, see [isWide]) it splits in two:
 * the clock and Now card on the left, the time bar, one thing and apps on the
 * right, as in the store's tablet screenshot.
 */
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
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val openSearch by rememberUpdatedState(onOpenSearch)
    // A swipe that starts on a column that scrolls reaches here as the scroll
    // that column couldn't use (it fits, or it's at its end).
    val swipeOverScroll = remember(swipeThreshold) { SwipeUpConnection(swipeThreshold) { openSearch() } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            // Swipe up anywhere on Home to search.
            .nestedScroll(swipeOverScroll)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -swipeThreshold) openSearch() },
                )
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        if (isWide(maxWidth, maxHeight)) {
            Row(
                modifier = Modifier
                    .widthIn(max = WideMaxWidth)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Each column centres itself and scrolls only if it doesn't fit.
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DateRow(now, onOpenSettings)
                    BigClock(now, if (maxHeight >= 520.dp) TendwellType.clock.copy(fontSize = 104.sp) else TendwellType.clock)
                    Box(Modifier.padding(top = 24.dp)) { nowCard() }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (timeBar != null) TimeBar(timeBar, Modifier.padding(bottom = 24.dp))
                    OneThingRow(oneThing, onToggleOneThing, onOpenCheckIn, Modifier.padding(bottom = 8.dp))
                    HorizontalDivider(color = c.line)
                    EssentialApps(essentials, loaded, onLaunch, onLongPress, Modifier.padding(top = 10.dp))
                    SearchPill(onOpenSearch, Modifier.padding(top = 12.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = ReadableWidth)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
            ) {
                // The top part scrolls on short screens; the app list and search stay put at the bottom.
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DateRow(now, onOpenSettings)
                    BigClock(now, TendwellType.clock)
                    if (timeBar != null) {
                        TimeBar(timeBar, Modifier.padding(top = 20.dp))
                    }
                    Box(Modifier.padding(top = 20.dp)) { nowCard() }
                    OneThingRow(oneThing, onToggleOneThing, onOpenCheckIn, Modifier.padding(top = 14.dp))
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider(color = c.line)
                EssentialApps(essentials, loaded, onLaunch, onLongPress, Modifier.padding(top = 10.dp))
                SearchPill(onOpenSearch, Modifier.padding(top = 12.dp))
            }
        }
    }
}

/** Counts the upward drag a scrolling column hands back, and opens search past [threshold]. */
private class SwipeUpConnection(private val threshold: Float, private val onSwipeUp: () -> Unit) : NestedScrollConnection {
    private var total = 0f
    private var fired = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y < 0f && !fired) {
            total += available.y
            if (total < -threshold) {
                fired = true
                onSwipeUp()
            }
        }
        return Offset.Zero
    }

    // A drag always ends in a fling, even a still one, so this resets every gesture.
    override suspend fun onPreFling(available: Velocity): Velocity {
        total = 0f
        fired = false
        return Velocity.Zero
    }
}

@Composable
private fun DateRow(now: LocalDateTime, onOpenSettings: () -> Unit) {
    val c = Tendwell.colors
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
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
}

@Composable
private fun BigClock(now: LocalDateTime, style: TextStyle) {
    val clockFormat = remember { DateTimeFormatter.ofPattern("H:mm", Locale.getDefault()) }
    Text(
        text = now.format(clockFormat),
        style = style,
        color = Tendwell.colors.ink,
        modifier = Modifier.semantics { heading() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EssentialApps(
    essentials: List<AppEntry>,
    loaded: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tendwell.colors
    Column(modifier) {
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
}

@Composable
private fun SearchPill(onOpenSearch: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tendwell.colors
    Row(
        modifier = modifier
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
