package app.tendwell.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tendwell.billing.PaywallScreen
import app.tendwell.billing.StoreState
import app.tendwell.checkin.CheckInScreen
import app.tendwell.focus.FocusHoursScreen
import app.tendwell.home.HomeScreen
import app.tendwell.insights.InsightsScreen
import app.tendwell.insights.WeekInsights
import app.tendwell.now.NowCard
import app.tendwell.now.NowSetSheet
import app.tendwell.now.NowTask
import app.tendwell.now.TimerScreen
import app.tendwell.onboarding.EssentialsStep
import app.tendwell.onboarding.PauseAppsStep
import app.tendwell.onboarding.SetHomeStep
import app.tendwell.onboarding.WelcomeStep
import app.tendwell.pause.PauseScreen
import app.tendwell.search.SearchScreen
import app.tendwell.settings.SettingsScreen
import app.tendwell.ui.components.AppActionsSheet
import app.tendwell.ui.components.CenteredPage
import app.tendwell.ui.theme.TendwellTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

/** A phone held upright: 411 × 914 dp, like a Pixel 7. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-port-xhdpi")
class PhoneScreenshots : ScreenshotTests("phone")

/** A 10" tablet held sideways: 1280 × 800 dp. Home, Pause, the timer and Insights use two columns here. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class TabletScreenshots : ScreenshotTests("tablet")

/**
 * Draws every screen with made-up content, in light and dark, and saves the
 * pictures to app/build/outputs/roborazzi/<device>-<light|dark>/. They are
 * for looking at: CI pushes them to a shots/ branch. A screen that crashes
 * while drawing fails the test.
 *
 * Screens are wrapped the way MainActivity wraps them, so one-column screens
 * are centred on tablets just like in the app.
 */
abstract class ScreenshotTests(private val device: String) {
    @get:Rule
    val compose = createComposeRule()

    private val options = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))

    /**
     * Shows [content] and saves it in light and then dark. [settleMs] is for
     * the pause, whose countdown runs on the wall clock: the test waits that
     * long for real, then lets the screen tick once. [wholeScreen] also
     * captures dialogs and bottom sheets.
     */
    @OptIn(ExperimentalRoborazziApi::class)
    private fun shoot(name: String, settleMs: Long? = null, wholeScreen: Boolean = false, content: @Composable () -> Unit) {
        var dark by mutableStateOf(false)
        if (settleMs != null) compose.mainClock.autoAdvance = false
        compose.setContent { TendwellTheme(dark = dark) { content() } }
        if (settleMs != null) Thread.sleep(settleMs)
        for (isDark in listOf(false, true)) {
            dark = isDark
            if (settleMs != null) compose.mainClock.advanceTimeBy(500) else compose.waitForIdle()
            val path = "build/outputs/roborazzi/$device-${if (isDark) "dark" else "light"}/$name.png"
            if (wholeScreen) captureScreenRoboImage(path, options) else compose.onRoot().captureRoboImage(path, options)
        }
    }

    private val noop: () -> Unit = {}

    // Onboarding

    @Test fun onboarding1Welcome() = shoot("01-onboarding-welcome") { CenteredPage { WelcomeStep(onStart = noop) } }

    @Test fun onboarding2Essentials() = shoot("02-onboarding-essentials") {
        CenteredPage {
            EssentialsStep(FakeData.allApps, FakeData.essentials.map { it.key }, onToggle = {}, onBack = noop, onContinue = noop)
        }
    }

    @Test fun onboarding3PauseApps() = shoot("03-onboarding-pause-apps") {
        CenteredPage {
            PauseAppsStep(
                apps = FakeData.allApps,
                selected = FakeData.pausedKeys.toList(),
                isPro = false,
                onToggle = {},
                onBack = noop,
                onContinue = noop,
                onSeePro = noop,
            )
        }
    }

    @Test fun onboarding4SetHome() = shoot("04-onboarding-set-home") {
        CenteredPage { SetHomeStep(isDefaultHome = false, onBack = noop, onSetHome = noop, onNext = noop) }
    }

    @Test fun onboarding5Paywall() = shoot("05-onboarding-paywall") { CenteredPage { Paywall(StoreState.Ready(FakeData.offers), step = 5) } }

    // Home

    @Test fun home() = shoot("10-home") {
        Home(task = FakeData.runningTask(), withContent = true)
    }

    @Test fun homeFirstDay() = shoot("11-home-empty") {
        Home(task = null, withContent = false)
    }

    @Test fun homeTimeUp() = shoot("12-home-time-up") { Home(task = FakeData.timeUpTask(), withContent = true) }

    @Test fun homePaused() = shoot("13-home-timer-paused") { Home(task = FakeData.pausedTask(), withContent = true) }

    @Test fun homeNoTimer() = shoot("14-home-no-timer") { Home(task = FakeData.noTimerTask(), withContent = true) }

    @Test fun homeBreak() = shoot("15-home-break") { Home(task = FakeData.breakTask(), withContent = true) }

    @Test fun homeDone() = shoot("16-home-done") { Home(task = FakeData.doneTask(), withContent = true) }

    @Test fun homeAppActions() = shoot("17-home-app-actions", wholeScreen = true) {
        Box(Modifier.fillMaxSize()) {
            Home(task = FakeData.runningTask(), withContent = true)
            AppActionsSheet(
                app = FakeData.messages,
                onHome = true,
                homeIsFull = false,
                canMoveUp = true,
                canMoveDown = true,
                isPaused = false,
                pauseIsFull = true,
                onSetPaused = {},
                onSeePro = noop,
                onAdd = noop,
                onRemove = noop,
                onMoveUp = noop,
                onMoveDown = noop,
                onAppInfo = noop,
                onDismiss = noop,
            )
        }
    }

    @Test fun homeNowSheet() = shoot("18-home-now-sheet", wholeScreen = true) {
        Box(Modifier.fillMaxSize()) {
            Home(task = null, withContent = true)
            NowSetSheet(
                initialTitle = "",
                oneThing = FakeData.oneThing.text,
                events = FakeData.events,
                nowMinute = FakeData.now.hour * 60 + FakeData.now.minute,
                onStart = { _, _ -> },
                onSaveWithoutTimer = {},
                onDismiss = noop,
            )
        }
    }

    // Check-in, search, pause

    @Test fun checkIn() = shoot("20-check-in") {
        CenteredPage {
            CheckInScreen(now = FakeData.now.withHour(8).withMinute(15), picks = FakeData.checkInPicks, onSet = {}, onSkip = noop)
        }
    }

    @Test fun search() = shoot("21-search") { Search(query = "") }

    @Test fun searchQuery() = shoot("22-search-query") { Search(query = "c") }

    @Test fun searchNothingFound() = shoot("23-search-nothing") { Search(query = "zzz") }

    @Test fun pause() = shoot("30-pause", settleMs = 3_000) { Pause(seconds = 10, focusNote = null) }

    @Test fun pauseOver() = shoot("31-pause-over", settleMs = 11_000) { Pause(seconds = 10, focusNote = null) }

    @Test fun pauseFocusHours() = shoot("32-pause-focus-hours", settleMs = 3_000) {
        Pause(seconds = 30, focusNote = "Focus hours until 17:00")
    }

    // Focus timer

    @Test fun timer() = shoot("40-timer") { Timer(FakeData.runningTask()) }

    @Test fun timerPaused() = shoot("41-timer-paused") { Timer(FakeData.pausedTask()) }

    @Test fun timerTimeUp() = shoot("42-timer-time-up") { Timer(FakeData.timeUpTask()) }

    @Test fun timerDone() = shoot("43-timer-done") { Timer(FakeData.doneTask()) }

    @Test fun timerBreak() = shoot("44-timer-break") { Timer(FakeData.breakTask()) }

    // Settings, Pro

    @Test fun settings() = shoot("50-settings") { CenteredPage { Settings(isPro = false) } }

    @Test fun settingsPro() = shoot("51-settings-pro") { CenteredPage { Settings(isPro = true) } }

    @Test fun paywall() = shoot("52-paywall") { CenteredPage { Paywall(StoreState.Ready(FakeData.offers), step = null) } }

    @Test fun paywallLoading() = shoot("53-paywall-loading") { CenteredPage { Paywall(StoreState.Loading, step = null) } }

    @Test fun paywallUnavailable() = shoot("54-paywall-unavailable") { CenteredPage { Paywall(StoreState.Unavailable, step = null) } }

    @Test fun focusHours() = shoot("55-focus-hours") {
        CenteredPage {
            FocusHoursScreen(
                blocks = FakeData.focusBlocks,
                now = FakeData.now,
                pausedNames = listOf("Videos", "Social", "Clips"),
                onChange = {},
                onPickTime = { _, _ -> },
                onBack = noop,
            )
        }
    }

    @Test fun insights() = shoot("56-insights") {
        val zone = ZoneId.systemDefault()
        Insights(WeekInsights.stats(FakeData.insightEvents(zone), FakeData.insightsWeek, zone))
    }

    @Test fun insightsEmptyWeek() = shoot("57-insights-empty") {
        Insights(WeekInsights.stats(emptyList(), FakeData.insightsWeek, ZoneId.systemDefault()))
    }

    // Building blocks

    @Composable
    private fun Home(task: NowTask?, withContent: Boolean) {
        HomeScreen(
            essentials = if (withContent) FakeData.essentials else emptyList(),
            loaded = true,
            timeBar = FakeData.timeBar(withEvents = withContent),
            oneThing = if (withContent) FakeData.oneThing else null,
            nowCard = {
                NowCard(
                    task = task,
                    onOpenTimer = noop,
                    onSetTask = noop,
                    onPause = noop,
                    onResume = noop,
                    onDone = noop,
                    onStartTimer = noop,
                )
            },
            onToggleOneThing = noop,
            onOpenCheckIn = noop,
            onLaunch = {},
            onLongPress = {},
            onOpenSearch = noop,
            onOpenSettings = noop,
        )
    }

    @Composable
    private fun Search(query: String) {
        CenteredPage {
            SearchScreen(
                apps = FakeData.allApps,
                pausedKeys = FakeData.pausedKeys,
                query = query,
                onQueryChange = {},
                onLaunch = {},
                onLongPress = {},
                onClose = noop,
            )
        }
    }

    @Composable
    private fun Pause(seconds: Int, focusNote: String?) {
        PauseScreen(
            app = FakeData.videos,
            openNumber = 4,
            task = "Write the report",
            onBack = noop,
            onOpenAnyway = noop,
            seconds = seconds,
            focusNote = focusNote,
        )
    }

    @Composable
    private fun Timer(task: NowTask) {
        TimerScreen(
            task = task,
            nextEvent = FakeData.timeBar().next,
            onBack = noop,
            onPause = noop,
            onResume = noop,
            onAddFive = noop,
            onDone = noop,
            onStartBreak = noop,
        )
    }

    @Composable
    private fun Settings(isPro: Boolean) {
        SettingsScreen(
            isDefaultHome = true,
            bedtimeMin = 23 * 60,
            calendarOn = true,
            pausedNames = listOf("Videos", "Social", "Clips"),
            isPro = isPro,
            focusSummary = if (isPro) "Weekdays · 9:00 → 17:00" else null,
            onPickBedtime = noop,
            onToggleCalendar = {},
            onOpenHomeSettings = noop,
            onOpenPro = noop,
            onOpenFocusHours = noop,
            onOpenInsights = noop,
            onManagePro = noop,
            onBack = noop,
        )
    }

    @Composable
    private fun Paywall(store: StoreState, step: Int?) {
        PaywallScreen(
            store = store,
            step = step,
            today = FakeData.today,
            restoring = false,
            message = null,
            onMessageShown = noop,
            onClose = noop,
            onRestore = noop,
            onRetry = noop,
            onBuy = {},
            onOpenLink = {},
            onAccessCode = { false },
        )
    }

    @Composable
    private fun Insights(stats: app.tendwell.insights.WeekStats) {
        InsightsScreen(
            weekStart = FakeData.insightsWeek,
            today = FakeData.today,
            stats = stats,
            canGoBack = true,
            canGoForward = true,
            appLabel = FakeData::label,
            onPrevWeek = noop,
            onNextWeek = noop,
            onSetFocusHours = noop,
            onBack = noop,
        )
    }
}
