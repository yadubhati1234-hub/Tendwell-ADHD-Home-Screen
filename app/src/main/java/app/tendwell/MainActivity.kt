package app.tendwell

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tendwell.apps.AppEntry
import app.tendwell.checkin.CheckInPick
import app.tendwell.checkin.CheckInScreen
import app.tendwell.data.DayState
import app.tendwell.day.DayViewModel
import app.tendwell.home.HomeScreen
import app.tendwell.home.LauncherViewModel
import app.tendwell.home.rememberCurrentMinute
import app.tendwell.now.NowCard
import app.tendwell.now.NowPhase
import app.tendwell.now.NowSetSheet
import app.tendwell.now.TimerAlarm
import app.tendwell.now.TimerScreen
import app.tendwell.search.SearchScreen
import app.tendwell.settings.SettingsScreen
import app.tendwell.timebar.CalendarReader
import app.tendwell.timebar.TimeBarModel
import app.tendwell.ui.components.AppActionsSheet
import app.tendwell.ui.theme.TendwellTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.LocalDate

private enum class Screen { Home, Search, Settings, Timer, CheckIn }

/** The launcher activity. It handles the HOME intent, so pressing Home always lands here. */
class MainActivity : ComponentActivity() {

    private val launcher: LauncherViewModel by viewModels()
    private val dayModel: DayViewModel by viewModels()

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

        var screen by rememberSaveable { mutableStateOf(Screen.Home) }
        var query by rememberSaveable { mutableStateOf("") }
        var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
        var sheetOnHome by rememberSaveable { mutableStateOf(false) }
        var showNowSet by rememberSaveable { mutableStateOf(false) }
        var isDefaultHome by remember { mutableStateOf(true) }
        // Check-in is offered once per day, the first time Home shows.
        var checkInOfferedOn by rememberSaveable { mutableStateOf<String?>(null) }

        fun goHome() {
            screen = Screen.Home
            query = ""
            sheetFor = null
            showNowSet = false
        }

        fun launch(app: AppEntry) {
            if (launcher.launch(app)) {
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
            onPauseOrDispose { }
        }
        // Refresh calendar events every hour while Home is open, and when the day setting changes.
        LaunchedEffect(nowMinute.hour, day?.calendarOn) { dayModel.refreshCalendar() }

        val currentDay: DayState? = day
        val today = nowMinute.toLocalDate()
        LaunchedEffect(currentDay?.checkInDate, today, screen) {
            if (currentDay != null && screen == Screen.Home && currentDay.checkInDate != today &&
                checkInOfferedOn != today.toString()
            ) {
                checkInOfferedOn = today.toString()
                screen = Screen.CheckIn
            }
        }

        // On Home, Back does nothing: there is nowhere further back to go.
        BackHandler(enabled = true) {
            when (screen) {
                Screen.Home -> Unit
                Screen.CheckIn -> { dayModel.skipCheckIn(); goHome() }
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
                Screen.Search -> SearchScreen(
                    apps = state.allApps,
                    query = query,
                    onQueryChange = { query = it },
                    onLaunch = ::launch,
                    onLongPress = { sheetFor = it.key; sheetOnHome = it.key in state.essentialKeys },
                    onClose = ::goHome,
                )
                Screen.Settings -> SettingsScreen(
                    isDefaultHome = isDefaultHome,
                    bedtimeMin = currentDay?.bedtimeMin ?: TimeBarModel.DAY - 60,
                    calendarOn = currentDay?.calendarOn == true,
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
                    onBack = ::goHome,
                )
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
                Screen.CheckIn -> CheckInScreen(
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

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "This setting is not available on your phone.", Toast.LENGTH_SHORT).show()
        }
    }
}

