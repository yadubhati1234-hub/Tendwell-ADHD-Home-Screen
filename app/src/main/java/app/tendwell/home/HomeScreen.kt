package app.tendwell.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Tendwell.colors
    val now by rememberCurrentMinute()
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val clockFormat = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
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

        Spacer(Modifier.weight(1f).heightIn(min = 8.dp))

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
