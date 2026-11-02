package app.tendwell.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tendwell.apps.AppEntry
import app.tendwell.data.Prefs
import app.tendwell.ui.components.TwIcons
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/**
 * Step 2: pick up to 7 apps for Home. [selected] is in Home order; apps
 * picked when the step opened are listed first so the list does not jump.
 */
@Composable
fun EssentialsStep(
    apps: List<AppEntry>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val c = Tendwell.colors
    val firstPicks = remember(apps.isEmpty()) { selected }
    val ordered = remember(apps, firstPicks) {
        val byKey = apps.associateBy { it.key }
        firstPicks.mapNotNull { byKey[it] } + apps.filter { it.key !in firstPicks }
    }
    val count = selected.size
    val full = count >= Prefs.MAX_ESSENTIALS

    PickerPage(
        step = 2,
        onBack = onBack,
        title = "Pick your essential apps",
        lead = "Up to ${Prefs.MAX_ESSENTIALS} apps on your Home screen, as plain text. Search finds the rest.",
        countRow = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountChip(
                    text = "$count of ${Prefs.MAX_ESSENTIALS}",
                    icon = if (count > 0) TwIcons.Check else null,
                    background = c.primarySoft,
                    content = c.onPrimarySoft,
                )
                if (full) {
                    Text(
                        "Seven is the limit. Fewer is calmer.",
                        style = TendwellType.small.copy(fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em),
                        color = c.ink2,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        list = {
            item { HorizontalDivider(color = c.line) }
            items(ordered, key = { it.key }) { app ->
                val on = app.key in selected
                val locked = full && !on
                PickRow(
                    app = app,
                    on = on,
                    enabled = !locked,
                    onToggle = { onToggle(app.key) },
                    mark = { EssentialMark(on, locked) },
                )
                HorizontalDivider(color = c.line)
            }
        },
        footer = {
            if (count > 0) {
                PrimaryPill("Continue", onClick = onContinue)
            } else {
                PrimaryPill("Pick at least one app", onClick = {}, enabled = false)
            }
            Footnote("You can change these later by long-pressing an app.")
        },
    )
}

/**
 * Step 3: pick apps that get the 10-second pause. Free covers 3; trying a
 * 4th shows the Pro card instead of selecting it.
 */
@Composable
fun PauseAppsStep(
    apps: List<AppEntry>,
    selected: List<String>,
    isPro: Boolean,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSeePro: () -> Unit,
) {
    val c = Tendwell.colors
    var upsell by rememberSaveable { mutableStateOf(false) }
    val (suggested, other) = remember(apps) { PauseSuggestions.split(apps) { it.component.packageName } }
    val count = selected.size
    val limit = Prefs.MAX_FREE_PAUSED

    fun pick(key: String) {
        if (key in selected || isPro || count < limit) {
            upsell = false
            onToggle(key)
        } else {
            upsell = true
        }
    }

    PickerPage(
        step = 3,
        onBack = onBack,
        title = "Which apps pull you in?",
        lead = if (isPro) {
            "They'll open after a ${Prefs.PAUSE_SECONDS}-second pause, so you can choose on purpose."
        } else {
            "They'll open after a ${Prefs.PAUSE_SECONDS}-second pause, so you can choose on purpose. Free covers $limit apps."
        },
        countRow = {
            CountChip(
                text = if (isPro) "$count paused" else "$count of $limit",
                icon = TwIcons.Hourglass,
                background = c.timeSoft,
                content = c.onTimeSoft,
            )
        },
        list = {
            fun group(label: String, list: List<AppEntry>) {
                if (list.isEmpty()) return
                item(key = "label-$label") {
                    Text(
                        label.uppercase(),
                        style = TendwellType.eyebrow,
                        color = c.ink3,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp).semantics { heading() },
                    )
                }
                items(list, key = { "$label-${it.key}" }) { app ->
                    val on = app.key in selected
                    HorizontalDivider(color = c.line)
                    PickRow(app = app, on = on, enabled = true, onToggle = { pick(app.key) }, mark = { PauseMark(on) })
                }
            }
            group("Suggested", suggested)
            group(if (suggested.isEmpty()) "Your apps" else "Other apps", other)
        },
        footer = {
            AnimatedVisibility(upsell && !isPro, enter = fadeIn() + slideInVertically { it / 3 }) {
                ProUpsellCard(onSeePro)
            }
            PrimaryPill("Continue", onClick = onContinue)
            Footnote("You can change these later by long-pressing an app.")
        },
    )
}

/** Layout shared by the two picker steps: header, a scrolling list with a bottom fade, and a fixed footer. */
@Composable
private fun PickerPage(
    step: Int,
    onBack: () -> Unit,
    title: String,
    lead: String,
    countRow: @Composable () -> Unit,
    list: LazyListScope.() -> Unit,
    footer: @Composable () -> Unit,
) {
    val c = Tendwell.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
    ) {
        OnboardingTopBar(step = step, onBack = onBack)
        OnboardingTitle(title, Modifier.padding(top = 16.dp))
        OnboardingLead(lead)
        Box(Modifier.padding(top = 16.dp)) { countRow() }
        Box(Modifier.padding(top = 12.dp).weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp), content = list)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Brush.verticalGradient(listOf(c.bg.copy(alpha = 0f), c.bg))),
            )
        }
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { footer() }
    }
}

@Composable
private fun CountChip(text: String, icon: ImageVector?, background: Color, content: Color) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(start = if (icon != null) 11.dp else 14.dp, end = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(text, style = TendwellType.small.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

/** A 56dp app row that toggles like a checkbox. */
@Composable
private fun PickRow(app: AppEntry, on: Boolean, enabled: Boolean, onToggle: () -> Unit, mark: @Composable () -> Unit) {
    val c = Tendwell.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = on, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                app.label,
                style = TendwellType.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                color = if (enabled) c.ink else c.ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (app.isWorkProfile) Text("Work", style = TendwellType.caption, color = c.ink3)
        }
        mark()
    }
}

@Composable
private fun EssentialMark(on: Boolean, locked: Boolean) {
    val c = Tendwell.colors
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (on) c.primary else Color.Transparent)
            .border(2.dp, if (on) c.primary else if (locked) c.line else c.ink3, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(TwIcons.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun PauseMark(on: Boolean) {
    val c = Tendwell.colors
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (on) c.timeSoft else Color.Transparent)
            .then(if (on) Modifier else Modifier.border(2.dp, c.ink3, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(TwIcons.Hourglass, contentDescription = null, tint = c.time, modifier = Modifier.size(16.dp))
    }
}

/** "Pause more than 3 apps with Pro", shown after trying to pick a 4th app. */
@Composable
private fun ProUpsellCard(onSeePro: () -> Unit) {
    val c = Tendwell.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.timeSoft)
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProBadge(background = c.surface)
            Text(
                "Pause more than ${Prefs.MAX_FREE_PAUSED} apps with Pro",
                style = TendwellType.secondary.copy(lineHeight = 1.35.em),
                color = c.ink,
            )
        }
        Row(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onSeePro)
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("See Pro", style = TendwellType.button, color = c.onTimeSoft)
            Icon(TwIcons.ChevronRight, contentDescription = null, tint = c.onTimeSoft, modifier = Modifier.size(18.dp))
        }
    }
}
