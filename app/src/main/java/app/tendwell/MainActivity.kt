package app.tendwell

import android.Manifest
import android.app.role.RoleManager
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tendwell.analytics.PaywallFrom
import app.tendwell.apps.AppEntry
import app.tendwell.billing.BillingRepository
import app.tendwell.billing.PRIVACY_URL
import app.tendwell.billing.PaywallRoute
import app.tendwell.checkin.CheckInPick
import app.tendwell.checkin.CheckInScreen
import app.tendwell.data.DayState
import app.tendwell.data.Prefs
import app.tendwell.day.DayViewModel
import app.tendwell.focus.FocusHours
import app.tendwell.focus.FocusHoursScreen
import app.tendwell.home.HomeScreen
import app.tendwell.home.LauncherViewModel
import app.tendwell.home.rememberCurrentMinute
import app.tendwell.insights.InsightsScreen
import app.tendwell.insights.WeekInsights
import app.tendwell.insights.WeekStats
import app.tendwell.now.NowCard
import app.tendwell.now.NowPhase
import app.tendwell.now.NowSetSheet
import app.tendwell.now.TimerAlarm
import app.tendwell.now.TimerScreen
import app.tendwell.onboarding.EssentialsStep
import app.tendwell.onboarding.PauseAppsStep
import app.tendwell.onboarding.SetHomeStep
import app.tendwell.onboarding.WelcomeStep
import app.tendwell.pause.PauseScreen
import app.tendwell.search.SearchScreen
import app.tendwell.settings.SettingsScreen
import app.tendwell.timebar.CalendarReader
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.AppActionsSheet
import app.tendwell.ui.components.CenteredPage
import app.tendwell.ui.theme.Tendwell
import app.tendwell.ui.theme.TendwellTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private enum class Screen { Home, Search, Settings, Timer, CheckIn, Pause, Paywall, FocusHours, Insights }

/** The launcher activity. It handles the HOME intent, so pressing Home always lands here. */
class MainActivity : ComponentActivity() {

    private val launcher: LauncherViewModel by viewModels()
    private val dayModel: DayViewModel by viewModels()
    private val tendwell: TendwellApp get() = application as TendwellApp

    /** Fires when the Home button is pressed while Tendwell is already showing. */
    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            TendwellTheme {
                LauncherRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressed.tryEmit(Unit)
        }
    }

    @Composable
    private fun LauncherRoot() {
        val state by launcher.state.collectAsStateWithLifecycle()
        val day by dayModel.day.collectAsStateWithLifecycle()
        val events by dayModel.events.collectAsStateWithLifecycle()
        val nowMinute by rememberCurrentMinute()
        val onboardingDone by launcher.onboardingDone.collectAsStateWithLifecycle()
        val focusBlocks by launcher.focusBlocks.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        var screen by rememberSaveable { mutableStateOf(Screen.Home) }
        var query by rememberSaveable { mutableStateOf("") }
        var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
        var sheetOnHome by rememberSaveable { mutableStateOf(false) }
        var showNowSet by rememberSaveable { mutableStateOf(false) }
        var pauseFor by rememberSaveable { mutableStateOf<String?>(null) }
        var isDefaultHome by remember { mutableStateOf(true) }
        // Check-in is offered once per day, the first time Home shows.
        var checkInOfferedOn by rememberSaveable { mutableStateOf<String?>(null) }
        // Where Close on the paywall goes back to, and what opened it (for analytics).
        var paywallReturn by rememberSaveable { mutableStateOf(Screen.Home) }
        var paywallSource by rememberSaveable { mutableStateOf(PaywallFrom.Settings) }
        // Onboarding: the step (1 to 5), the picks so far (newline-joined keys, null = not edited yet),
        // and whether the paywall was opened from the pause step's Pro card.
        var obStep by rememberSaveable { mutableIntStateOf(1) }
        var obEssentials by rememberSaveable { mutableStateOf<String?>(null) }
        var obPaused by rememberSaveable { mutableStateOf<String?>(null) }
        var obProFromPause by rememberSaveable { mutableStateOf(false) }
        // Weekly insights: the Monday of the week shown (null = this week), and whether
        // focus hours were opened from insights (so Back returns there).
        var insightsWeek by rememberSaveable { mutableStateOf<String?>(null) }
        var focusFromInsights by rememberSaveable { mutableStateOf(false) }

        fun goHome() {
            screen = Screen.Home
            query = ""
            sheetFor = null
            showNowSet = false
            pauseFor = null
        }

        fun openPaywall(from: Screen, source: PaywallFrom) {
            paywallReturn = from
            paywallSource = source
            sheetFor = null
            screen = Screen.Paywall
        }

        fun closePaywall() {
            if (paywallReturn == Screen.Home) goHome() else screen = paywallReturn
        }

        fun launch(app: AppEntry) {
            if (app.key in state.pausedKeys) {
                // Paused apps get the breathing pause first.
                val focus = if (state.isPro) FocusHours.activeAt(focusBlocks, LocalDateTime.now()) else null
                tendwell.analytics.pauseShown(focus?.pauseSeconds ?: Prefs.PAUSE_SECONDS)
                pauseFor = app.key
                sheetFor = null
                screen = Screen.Pause
            } else if (launcher.launch(app)) {
                goHome()
            } else {
                Toast.makeText(this, "That app could not be opened.", Toast.LENGTH_SHORT).show()
            }
        }

        val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        fun askForNotificationsIfNeeded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !TimerAlarm.canNotify(this)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            dayModel.setCalendarOn(granted)
            if (!granted) Toast.makeText(this, "Calendar stays off. The bar counts down to bedtime.", Toast.LENGTH_LONG).show()
        }

        LaunchedEffect(Unit) { homePressed.collect { goHome() } }
        LifecycleResumeEffect(Unit) {
            isDefaultHome = launcher.isDefaultHome()
            dayModel.refreshCalendar()
            // Re-check Pro a moment after Home is up, so billing never slows the first frame.
            val refresh = scope.launch {
                delay(1500)
                tendwell.billing.refreshIfStale()
                tendwell.analytics.linkCrashReports()
            }
            onPauseOrDispose { refresh.cancel() }
        }

        val homeRoleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isDefaultHome = launcher.isDefaultHome()
            obStep = 5
        }
        fun requestHomeRole() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roles = getSystemService(RoleManager::class.java)
                if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                    homeRoleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                }
            }
            startSafely(launcher.homeSettingsIntent())
            obStep = 5
        }
        // Refresh calendar events every hour while Home is open, and when the day setting changes.
        LaunchedEffect(nowMinute.hour, day?.calendarOn) { dayModel.refreshCalendar() }

        val currentDay: DayState? = day
        val today = nowMinute.toLocalDate()
        // The keyboard from Search or the check-in can outlive its text field and cover the next
        // screen's buttons (seen on the pause screen), so it closes whenever the screen changes.
        // The manifest's stateAlwaysHidden also stops Android reopening it when the window gets
        // focus back, for example after a bottom sheet closes.
        val rootView = LocalView.current
        LaunchedEffect(screen) {
            if (screen != Screen.Search) {
                WindowCompat.getInsetsController(window, rootView).hide(WindowInsetsCompat.Type.ime())
            }
        }
        LaunchedEffect(currentDay?.checkInDate, today, screen, onboardingDone) {
            if (currentDay != null && onboardingDone == true && screen == Screen.Home && currentDay.checkInDate != today &&
                checkInOfferedOn != today.toString()
            ) {
                checkInOfferedOn = today.toString()
                screen = Screen.CheckIn
            }
        }

        // On Home, Back does nothing: there is nowhere further back to go.
        BackHandler(enabled = true) {
            if (onboardingDone != true) {
                when {
                    obProFromPause -> obProFromPause = false
                    obStep > 1 -> obStep -= 1
                }
                return@BackHandler
            }
            when (screen) {
                Screen.Home -> Unit
                Screen.CheckIn -> { dayModel.skipCheckIn(); goHome() }
                Screen.Pause -> {
                    state.allApps.firstOrNull { it.key == pauseFor }?.let(launcher::turnedBack)
                    goHome()
                }
                Screen.Paywall -> closePaywall()
                Screen.FocusHours -> screen = if (focusFromInsights) Screen.Insights else Screen.Settings
                Screen.Insights -> screen = Screen.Settings
                else -> goHome()
            }
        }

        val timeBar = currentDay?.let {
            TimeBarModel(
                dayStartMin = it.dayStartMin,
                bedtimeMin = it.bedtimeMin,
                nowMin = nowMinute.hour * 60 + nowMinute.minute,
                events = if (it.calendarOn) events else emptyList(),
            )
        }
        val todaysOneThing = currentDay?.oneThing?.takeIf { it.date == today }
        val nowTask = currentDay?.now

        when (onboardingDone) {
            null -> {
                // A few milliseconds while settings load from disk.
                Box(Modifier.fillMaxSize().background(Tendwell.colors.bg))
                return
            }
            false -> {
                val essentialsSel = obEssentials?.split('\n')?.filter { it.isNotBlank() } ?: state.essentials.map { it.key }
                val pausedSel = obPaused?.split('\n')?.filter { it.isNotBlank() } ?: state.pausedKeys.toList()
                fun toggled(list: List<String>, key: String) = (if (key in list) list - key else list + key).joinToString("\n")
                fun finish() {
                    launcher.finishOnboarding()
                    goHome()
                }
                // On tablets the one-column onboarding stays at a readable width.
                CenteredPage {
                    when {
                        obProFromPause -> PaywallRoute(
                            tendwell.billing,
                            step = null,
                            onDone = { obProFromPause = false },
                            onShown = { tendwell.analytics.paywallShown(PaywallFrom.PauseAppsStep) },
                        )
                        obStep <= 1 -> WelcomeStep(onStart = { obStep = 2 })
                        obStep == 2 -> EssentialsStep(
                            apps = state.allApps,
                            selected = essentialsSel,
                            onToggle = { obEssentials = toggled(essentialsSel, it) },
                            onBack = { obStep = 1 },
                            onContinue = {
                                launcher.saveEssentials(essentialsSel)
                                obStep = 3
                            },
                        )
                        obStep == 3 -> PauseAppsStep(
                            apps = state.allApps,
                            selected = pausedSel,
                            isPro = state.isPro,
                            onToggle = { obPaused = toggled(pausedSel, it) },
                            onBack = { obStep = 2 },
                            onContinue = {
                                launcher.savePaused(pausedSel)
                                obStep = 4
                            },
                            onSeePro = { obProFromPause = true },
                        )
                        obStep == 4 -> SetHomeStep(
                            isDefaultHome = isDefaultHome,
                            onBack = { obStep = 3 },
                            onSetHome = ::requestHomeRole,
                            onNext = { obStep = 5 },
                        )
                        else -> PaywallRoute(
                            tendwell.billing,
                            step = 5,
                            onDone = ::finish,
                            onShown = { tendwell.analytics.paywallShown(PaywallFrom.Onboarding) },
                        )
                    }
                }
                return
            }
            true -> Unit
        }

        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    essentials = state.essentials,
                    loaded = state.loaded,
                    timeBar = timeBar,
                    oneThing = todaysOneThing,
                    nowCard = {
                        NowCard(
                            task = nowTask,
                            onOpenTimer = { screen = Screen.Timer },
                            onSetTask = { showNowSet = true },
                            onPause = dayModel::pauseNow,
                            onResume = dayModel::resumeNow,
                            onDone = {
                                if (nowTask?.phase == NowPhase.Task) screen = Screen.Timer
                                dayModel.completeNow()
                            },
                            onStartTimer = { showNowSet = true },
                        )
                    },
                    onToggleOneThing = { todaysOneThing?.let { dayModel.setOneThingDone(!it.done) } },
                    onOpenCheckIn = { screen = Screen.CheckIn },
                    onLaunch = ::launch,
                    onLongPress = { sheetFor = it.key; sheetOnHome = true },
                    onOpenSearch = { screen = Screen.Search },
                    onOpenSettings = { screen = Screen.Settings },
                )
                Screen.Search -> CenteredPage {
                    SearchScreen(
                        apps = state.allApps,
                        pausedKeys = state.pausedKeys,
                        query = query,
                        onQueryChange = { query = it },
                        onLaunch = ::launch,
                        onLongPress = { sheetFor = it.key; sheetOnHome = it.key in state.essentialKeys },
                        onClose = ::goHome,
                    )
                }
                Screen.Settings -> CenteredPage {
                    SettingsScreen(
                        isDefaultHome = isDefaultHome,
                        bedtimeMin = currentDay?.bedtimeMin ?: TimeBarModel.DAY - 60,
                        calendarOn = currentDay?.calendarOn == true,
                        pausedNames = state.pausedApps.map { it.label },
                        isPro = state.isPro,
                        focusSummary = focusBlocks.filter { it.on }.let { on ->
                            when (on.size) {
                                0 -> null
                                1 -> FocusHours.summary(on[0])
                                else -> "${on.size} sets of focus hours on"
                            }
                        },
                        onPickBedtime = {
                            val current = currentDay?.bedtimeMin ?: (23 * 60)
                            TimePickerDialog(this@MainActivity, { _, h, m -> dayModel.setBedtime(h * 60 + m) }, current / 60, current % 60, true).show()
                        },
                        onToggleCalendar = { on ->
                            if (on && !CalendarReader.hasPermission(this@MainActivity)) {
                                calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                            } else {
                                dayModel.setCalendarOn(on)
                            }
                        },
                        onOpenHomeSettings = { startSafely(launcher.homeSettingsIntent()) },
                        onOpenPro = { openPaywall(Screen.Settings, PaywallFrom.Settings) },
                        onOpenFocusHours = {
                            focusFromInsights = false
                            if (state.isPro) {
                                tendwell.analytics.focusHoursOpened()
                                screen = Screen.FocusHours
                            } else {
                                openPaywall(Screen.Settings, PaywallFrom.FocusHours)
                            }
                        },
                        onOpenInsights = {
                            insightsWeek = null
                            if (state.isPro) {
                                tendwell.analytics.insightsOpened()
                                screen = Screen.Insights
                            } else {
                                openPaywall(Screen.Settings, PaywallFrom.Insights)
                            }
                        },
                        onManagePro = {
                            startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(BillingRepository.manageSubscriptionUrl(packageName))))
                        },
                        onOpenPrivacy = { startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) },
                        onCopyAnalyticsId = { scope.launch { copyAnalyticsId() } },
                        onBack = ::goHome,
                    )
                }
                Screen.Timer -> {
                    val timerTask = nowTask?.takeIf { it.hasTimer || it.phase == NowPhase.Done }
                    if (timerTask != null) TimerScreen(
                        task = timerTask,
                        nextEvent = timeBar?.next,
                        onBack = ::goHome,
                        onPause = dayModel::pauseNow,
                        onResume = dayModel::resumeNow,
                        onAddFive = dayModel::addFiveMinutes,
                        onDone = {
                            val wasBreak = timerTask.phase == NowPhase.Break
                            dayModel.completeNow()
                            if (wasBreak) goHome()
                        },
                        onStartBreak = dayModel::startBreak,
                    )
                    // Otherwise the new task is still being saved; the timer appears a moment later.
                }
                Screen.Pause -> {
                    val app = state.allApps.firstOrNull { it.key == pauseFor }
                    if (app != null) {
                        var openNumber by remember(app.key) { mutableStateOf<Int?>(null) }
                        LaunchedEffect(app.key) { openNumber = launcher.opensToday(app) + 1 }
                        val taskName = nowTask?.takeIf { it.phase == NowPhase.Task }?.title
                            ?: todaysOneThing?.takeIf { !it.done }?.text
                        val focus = remember(app.key) {
                            if (state.isPro) FocusHours.activeAt(focusBlocks, LocalDateTime.now()) else null
                        }
                        PauseScreen(
                            seconds = focus?.pauseSeconds ?: Prefs.PAUSE_SECONDS,
                            focusNote = focus?.let { "Focus hours until ${TimeBarModel.formatClock(it.endMin)}" },
                            app = app,
                            openNumber = openNumber,
                            task = taskName,
                            onBack = {
                                launcher.turnedBack(app)
                                goHome()
                            },
                            onOpenAnyway = {
                                if (launcher.openAfterPause(app)) {
                                    goHome()
                                } else {
                                    Toast.makeText(this@MainActivity, "That app could not be opened.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }
                }
                Screen.Paywall -> CenteredPage {
                    PaywallRoute(
                        tendwell.billing,
                        step = null,
                        onDone = ::closePaywall,
                        onShown = { tendwell.analytics.paywallShown(paywallSource) },
                    )
                }
                Screen.FocusHours -> CenteredPage {
                    FocusHoursScreen(
                        blocks = focusBlocks,
                        now = nowMinute,
                        pausedNames = state.pausedApps.map { it.label },
                        onChange = launcher::setFocusBlocks,
                        onPickTime = { current, onPicked ->
                            TimePickerDialog(this@MainActivity, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, true).show()
                        },
                        onBack = { screen = if (focusFromInsights) Screen.Insights else Screen.Settings },
                    )
                }
                Screen.Insights -> {
                    val thisWeek = WeekInsights.weekStart(today)
                    val week = insightsWeek?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: thisWeek
                    val stats by produceState<WeekStats?>(null, week) {
                        value = WeekInsights.stats(launcher.eventsForInsights(week), week, ZoneId.systemDefault())
                    }
                    InsightsScreen(
                        weekStart = week,
                        today = today,
                        stats = stats,
                        // The open log keeps 35 days: this week and the four before it.
                        canGoBack = week > thisWeek.minusWeeks(4),
                        canGoForward = week < thisWeek,
                        appLabel = { key -> state.allApps.firstOrNull { it.key == key }?.label ?: key.substringBefore('/') },
                        onPrevWeek = { insightsWeek = week.minusWeeks(1).toString() },
                        onNextWeek = { insightsWeek = week.plusWeeks(1).toString() },
                        onSetFocusHours = {
                            focusFromInsights = true
                            tendwell.analytics.focusHoursOpened()
                            screen = Screen.FocusHours
                        },
                        onBack = { screen = Screen.Settings },
                    )
                }
                Screen.CheckIn -> CenteredPage {
                    CheckInScreen(
                        now = nowMinute,
                        picks = checkInPicks(currentDay, today),
                        onSet = { text ->
                            dayModel.setOneThing(text)
                            goHome()
                        },
                        onSkip = {
                            dayModel.skipCheckIn()
                            goHome()
                        },
                    )
                }
            }

            if (showNowSet) {
                NowSetSheet(
                    initialTitle = nowTask?.takeIf { it.phase == NowPhase.Task }?.title.orEmpty(),
                    oneThing = todaysOneThing?.takeIf { !it.done }?.text,
                    events = timeBar?.events.orEmpty(),
                    nowMinute = nowMinute.hour * 60 + nowMinute.minute,
                    onStart = { title, minutes ->
                        askForNotificationsIfNeeded()
                        dayModel.startNow(title, minutes)
                        showNowSet = false
                        screen = Screen.Timer
                    },
                    onSaveWithoutTimer = { title ->
                        dayModel.saveNowWithoutTimer(title)
                        showNowSet = false
                    },
                    onDismiss = { showNowSet = false },
                )
            }

            val sheetApp = sheetFor?.let { key -> state.allApps.firstOrNull { it.key == key } }
            if (sheetApp != null) {
                val index = state.essentials.indexOfFirst { it.key == sheetApp.key }
                AppActionsSheet(
                    app = sheetApp,
                    onHome = sheetOnHome && index >= 0,
                    homeIsFull = state.homeIsFull,
                    canMoveUp = index > 0,
                    canMoveDown = index in 0 until state.essentials.lastIndex,
                    isPaused = sheetApp.key in state.pausedKeys,
                    pauseIsFull = state.pauseIsFull,
                    onSetPaused = { launcher.setPaused(sheetApp, it) },
                    onSeePro = { openPaywall(if (screen == Screen.Search) Screen.Search else Screen.Home, PaywallFrom.AppActions) },
                    onAdd = { launcher.addToHome(sheetApp) },
                    onRemove = { launcher.removeFromHome(sheetApp) },
                    onMoveUp = { launcher.moveOnHome(sheetApp, -1) },
                    onMoveDown = { launcher.moveOnHome(sheetApp, 1) },
                    onAppInfo = { launcher.openAppInfo(sheetApp) },
                    onDismiss = { sheetFor = null },
                )
            }
        }
    }

    /** Quick picks for the check-in: yesterday's unfinished one thing first, then recent tasks. */
    private fun checkInPicks(day: DayState?, today: LocalDate): List<CheckInPick> {
        if (day == null) return emptyList()
        val picks = mutableListOf<CheckInPick>()
        day.oneThing?.let { one ->
            if (!one.done && one.date.isBefore(today)) {
                val note = if (one.date == today.minusDays(1)) "from yesterday" else null
                picks += CheckInPick(one.text, note)
            }
        }
        day.recentTasks.forEach { t ->
            if (picks.none { it.label.equals(t, ignoreCase = true) }) picks += CheckInPick(t)
        }
        return picks.take(4)
    }

    /** Copies the Firebase app instance ID, which people send to have their data deleted (docs/delete-data). */
    private suspend fun copyAnalyticsId() {
        val id = tendwell.analytics.appInstanceId()
        if (id == null) {
            Toast.makeText(this, "No analytics ID yet. Nothing has been sent from this phone.", Toast.LENGTH_LONG).show()
            return
        }
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Tendwell analytics ID", id))
        // Android 13 and later show their own "Copied" message.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "Analytics ID copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "This setting is not available on your phone.", Toast.LENGTH_SHORT).show()
        }
    }
}

