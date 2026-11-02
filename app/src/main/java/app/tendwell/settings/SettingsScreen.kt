package app.tendwell.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tendwell.BuildConfigInfo
import app.tendwell.ui.components.TwIconButton
import app.tendwell.ui.components.TwIcons
import app.tendwell.data.Prefs
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.TwRow
import app.tendwell.ui.components.TwToggleRow
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellType

/** First cut of Settings: home-app switching and the app version. More rows arrive with each feature. */
@Composable
fun SettingsScreen(
    isDefaultHome: Boolean,
    bedtimeMin: Int,
    calendarOn: Boolean,
    pausedNames: List<String>,
    isPro: Boolean,
    /** Summary of the focus hours that are on, like "Weekdays · 9:00 → 17:00", or null. */
    focusSummary: String?,
    onPickBedtime: () -> Unit,
    onToggleCalendar: (Boolean) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenFocusHours: () -> Unit,
    onOpenInsights: () -> Unit,
    onManagePro: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Tendwell.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TwIconButton(TwIcons.ChevronLeft, contentDescription = "Back", onClick = onBack, modifier = Modifier.offset(x = (-10).dp))
        Text("Settings", style = TendwellType.h1, color = c.ink, modifier = Modifier.semantics { heading() })

        val cardShape = RoundedCornerShape(24.dp)
        SectionLabel("Time bar")
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (c.isDark) Modifier else Modifier.shadow(6.dp, cardShape, ambientColor = c.ink, spotColor = c.ink))
                .clip(cardShape)
                .background(c.surface)
                .border(1.dp, c.cardLine, cardShape),
        ) {
            TwRow(
                "Bedtime",
                onClick = onPickBedtime,
                icon = TwIcons.Moon,
                supporting = "The bar counts down to ${TimeBarModel.formatClock(bedtimeMin)}",
                trailing = TwIcons.ChevronRight,
            )
            HorizontalDivider(color = c.line)
            TwToggleRow(
                "Show calendar events",
                supporting = "Read on this phone only, never sent anywhere",
                icon = TwIcons.Calendar,
                checked = calendarOn,
                onCheckedChange = onToggleCalendar,
            )
        }

        SectionLabel("Pause")
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (c.isDark) Modifier else Modifier.shadow(6.dp, cardShape, ambientColor = c.ink, spotColor = c.ink))
                .clip(cardShape)
                .background(c.surface)
                .border(1.dp, c.cardLine, cardShape),
        ) {
            TwRow(
                if (isPro) "Paused apps (${pausedNames.size})" else "Paused apps (${pausedNames.size} of ${Prefs.MAX_FREE_PAUSED})",
                onClick = null,
                enabled = true,
                icon = TwIcons.Hourglass,
                supporting = if (pausedNames.isEmpty()) {
                    "None yet. Long-press an app in search to pause it."
                } else {
                    pausedNames.joinToString(", ") + ". Long-press one to stop pausing it."
                },
            )
            HorizontalDivider(color = c.line)
            TwRow(
                "Focus hours",
                onClick = onOpenFocusHours,
                icon = TwIcons.Clock,
                supporting = when {
                    !isPro -> "Longer pauses while you work, like 9:00 \u2192 17:00"
                    focusSummary != null -> focusSummary
                    else -> "Off"
                },
                trailing = TwIcons.ChevronRight,
                proBadge = true,
            )
            HorizontalDivider(color = c.line)
            TwRow(
                "Weekly insights",
                onClick = onOpenInsights,
                icon = TwIcons.Bars,
                supporting = "Your opens and the times you drift",
                trailing = TwIcons.ChevronRight,
                proBadge = true,
            )
        }

        SectionLabel("Pro")
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (c.isDark) Modifier else Modifier.shadow(6.dp, cardShape, ambientColor = c.ink, spotColor = c.ink))
                .clip(cardShape)
                .background(c.surface)
                .border(1.dp, c.cardLine, cardShape),
        ) {
            if (isPro) {
                TwRow("Tendwell Pro is on", onClick = null, enabled = true, icon = TwIcons.Sprout, supporting = "Thank you for supporting Tendwell")
                HorizontalDivider(color = c.line)
                TwRow(
                    "Manage subscription",
                    onClick = onManagePro,
                    icon = TwIcons.Settings,
                    supporting = "Change or cancel in Google Play",
                    trailing = TwIcons.ChevronRight,
                )
            } else {
                TwRow(
                    "See Tendwell Pro",
                    onClick = onOpenPro,
                    icon = TwIcons.Sprout,
                    supporting = "Pause more than ${Prefs.MAX_FREE_PAUSED} apps, focus hours and weekly insights",
                    trailing = TwIcons.ChevronRight,
                )
            }
        }

        SectionLabel("Home screen")
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (c.isDark) Modifier else Modifier.shadow(6.dp, cardShape, ambientColor = c.ink, spotColor = c.ink))
                .clip(cardShape)
                .background(c.surface)
                .border(1.dp, c.cardLine, cardShape),
        ) {
            if (isDefaultHome) {
                TwRow(
                    "Switch back to my old launcher",
                    onClick = onOpenHomeSettings,
                    icon = TwIcons.Home,
                    supporting = "Opens Android's Home app setting",
                    trailing = TwIcons.ChevronRight,
                )
            } else {
                TwRow(
                    "Make Tendwell your home screen",
                    onClick = onOpenHomeSettings,
                    icon = TwIcons.Home,
                    supporting = "Choose Tendwell in Android's Home app setting",
                    trailing = TwIcons.ChevronRight,
                )
            }
            HorizontalDivider(color = c.line)
            TwRow("Version", onClick = null, enabled = true, supporting = BuildConfigInfo.versionLabel)
        }
        Text(
            "Designed for ADHD brains. Everything stays on your phone.",
            style = TendwellType.caption,
            color = c.ink3,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = TendwellType.eyebrow, color = Tendwell.colors.ink3, modifier = Modifier.semantics { heading() })
}
