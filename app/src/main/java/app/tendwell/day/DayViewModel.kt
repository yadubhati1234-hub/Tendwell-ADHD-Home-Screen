package app.tendwell.day

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tendwell.TendwellApp
import app.tendwell.data.DayState
import app.tendwell.now.NowPhase
import app.tendwell.now.NowTask
import app.tendwell.now.TimerAlarm
import app.tendwell.timebar.CalendarReader
import app.tendwell.timebar.DayEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Today's state: the time bar's settings and events, the one thing, and the Now task with its timer. */
class DayViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = (app as TendwellApp).prefs

    /** Null until the first read from disk, so Home never flashes a wrong check-in. */
    val day: StateFlow<DayState?> = prefs.day.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _events = MutableStateFlow<List<DayEvent>>(emptyList())
    val events: StateFlow<List<DayEvent>> = _events.asStateFlow()

    private fun clock() = System.currentTimeMillis()

    fun refreshCalendar() {
        viewModelScope.launch(Dispatchers.IO) {
            val on = prefs.day.first().calendarOn
            _events.value = if (on) CalendarReader.todaysEvents(getApplication()) else emptyList()
        }
    }

    // Settings

    fun setBedtime(min: Int) { viewModelScope.launch { prefs.setBedtime(min) } }

    fun setCalendarOn(on: Boolean) {
        viewModelScope.launch {
            prefs.setCalendarOn(on)
            refreshCalendar()
        }
    }

    // Daily check-in and the one thing

    fun setOneThing(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { prefs.setOneThing(text, LocalDate.now()) }
    }

    fun skipCheckIn() { viewModelScope.launch { prefs.skipCheckIn(LocalDate.now()) } }

    fun setOneThingDone(done: Boolean) { viewModelScope.launch { prefs.setOneThingDone(done) } }

    // Now task and timer

    private fun updateNow(change: (NowTask?) -> NowTask?) {
        viewModelScope.launch {
            val state = prefs.day.first()
            val next = change(state.now)
            prefs.setNow(next)
            if (next == null) TimerAlarm.cancel(getApplication()) else TimerAlarm.schedule(getApplication(), next)
            // Finishing the Now task that is today's one thing ticks the one thing off too.
            val one = state.oneThing
            if (next?.phase == NowPhase.Done && state.now?.phase == NowPhase.Task &&
                one != null && one.date == LocalDate.now() && one.text.equals(next.title, ignoreCase = true)
            ) {
                prefs.setOneThingDone(true)
            }
        }
    }

    fun startNow(title: String, minutes: Int) {
        if (title.isBlank()) return
        updateNow { NowTask.start(title, minutes, clock()) }
    }

    fun saveNowWithoutTimer(title: String) {
        if (title.isBlank()) return
        updateNow { NowTask.withoutTimer(title) }
    }

    fun startTimerOnCurrent(minutes: Int) = updateNow { it?.startTimer(clock(), minutes) }

    fun pauseNow() = updateNow { it?.pause(clock()) }

    fun resumeNow() = updateNow { it?.resume(clock()) }

    fun addFiveMinutes() = updateNow { it?.addMinutes(clock(), 5) }

    /** Done on a task shows the celebration; done on a break clears the Now card. */
    fun completeNow() = updateNow { task ->
        when (task?.phase) {
            NowPhase.Task -> task.complete(clock())
            else -> null
        }
    }

    fun startBreak() = updateNow { it?.startBreak(clock()) }

    fun clearNow() = updateNow { null }
}
