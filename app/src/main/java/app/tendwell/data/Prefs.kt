package app.tendwell.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.tendwell.now.NowPhase
import app.tendwell.now.NowTask
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The user's "one thing" for a given day. */
data class OneThing(val text: String, val date: LocalDate, val done: Boolean)

/** Everything Home needs about today, read in one go. */
data class DayState(
    val bedtimeMin: Int = Prefs.DEFAULT_BEDTIME,
    val dayStartMin: Int = Prefs.DEFAULT_DAY_START,
    val calendarOn: Boolean = false,
    val oneThing: OneThing? = null,
    val checkInDate: LocalDate? = null,
    val recentTasks: List<String> = emptyList(),
    val now: NowTask? = null,
)

/** Small settings and today's state. All of it lives only on the phone. */
class Prefs(context: Context) {
    private val store = context.applicationContext.dataStore

    /** Ordered app keys shown on Home, or null if the user has never set them (fresh install). */
    val essentialKeys: Flow<List<String>?> = store.data.map { prefs ->
        prefs[ESSENTIALS]?.let { raw -> raw.split('\n').filter { it.isNotBlank() } }
    }

    suspend fun setEssentialKeys(keys: List<String>) {
        store.edit { it[ESSENTIALS] = keys.joinToString("\n") }
    }

    /** Apps that get the 10-second pause before opening. */
    val pausedKeys: Flow<Set<String>> = store.data.map { prefs ->
        prefs[PAUSED]?.split('\n')?.filter { it.isNotBlank() }?.toSet().orEmpty()
    }

    suspend fun setPausedKeys(keys: Set<String>) {
        store.edit { it[PAUSED] = keys.joinToString("\n") }
    }

    /**
     * Whether this Google account owns Pro. Billing refreshes it on every start,
     * so the cached value only bridges the moment before Play answers.
     */
    val isPro: Flow<Boolean> = store.data.map { it[IS_PRO] ?: false }

    suspend fun setPro(on: Boolean) {
        store.edit { it[IS_PRO] = on }
    }

    /** Focus hours (Pro). Raw saved form, decoded by FocusHours; null until first saved. */
    val focusBlocksRaw: Flow<String?> = store.data.map { it[FOCUS_BLOCKS] }

    suspend fun setFocusBlocksRaw(raw: String) {
        store.edit { it[FOCUS_BLOCKS] = raw }
    }

    /** False until the user finishes (or skips through) onboarding. */
    val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    val day: Flow<DayState> = store.data.map { p ->
        DayState(
            bedtimeMin = p[BEDTIME] ?: DEFAULT_BEDTIME,
            dayStartMin = p[DAY_START] ?: DEFAULT_DAY_START,
            calendarOn = p[CALENDAR_ON] ?: false,
            oneThing = readOneThing(p),
            checkInDate = p[CHECKIN_DATE]?.let(::parseDate),
            recentTasks = p[RECENT_TASKS]?.split('\n')?.filter { it.isNotBlank() }.orEmpty(),
            now = readNow(p),
        )
    }

    suspend fun setBedtime(min: Int) = store.edit { it[BEDTIME] = min }

    suspend fun setCalendarOn(on: Boolean) = store.edit { it[CALENDAR_ON] = on }

    suspend fun setOneThing(text: String, date: LocalDate) = store.edit {
        it[ONE_TEXT] = text.trim()
        it[ONE_DATE] = date.toString()
        it[ONE_DONE] = false
        it[CHECKIN_DATE] = date.toString()
        remember(it, text)
    }

    suspend fun setOneThingDone(done: Boolean) = store.edit { it[ONE_DONE] = done }

    /** Marks today's check-in as handled without setting a one thing. */
    suspend fun skipCheckIn(date: LocalDate) = store.edit { it[CHECKIN_DATE] = date.toString() }

    suspend fun setNow(task: NowTask?) = store.edit { p ->
        if (task == null) {
            p.remove(NOW_TITLE); p.remove(NOW_PHASE); p.remove(NOW_TOTAL); p.remove(NOW_STARTED)
            p.remove(NOW_END); p.remove(NOW_PAUSED_LEFT); p.remove(NOW_RUNNING); p.remove(NOW_SPENT)
        } else {
            p[NOW_TITLE] = task.title
            p[NOW_PHASE] = task.phase.name
            p[NOW_TOTAL] = task.totalMs
            p[NOW_STARTED] = task.startedAtMs
            p[NOW_END] = task.endAtMs
            p[NOW_PAUSED_LEFT] = task.pausedRemainingMs
            p[NOW_RUNNING] = task.running
            p[NOW_SPENT] = task.spentMinutes
            if (task.phase == NowPhase.Task) remember(p, task.title)
        }
    }

    /** Keeps a short list of recent task names for the check-in's quick picks. */
    private fun remember(p: MutablePreferences, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val list = p[RECENT_TASKS]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        p[RECENT_TASKS] = (listOf(t) + list.filterNot { it.equals(t, ignoreCase = true) })
            .take(MAX_RECENT).joinToString("\n")
    }

    private fun readOneThing(p: Preferences): OneThing? {
        val text = p[ONE_TEXT] ?: return null
        val date = p[ONE_DATE]?.let(::parseDate) ?: return null
        return OneThing(text, date, p[ONE_DONE] ?: false)
    }

    private fun readNow(p: Preferences): NowTask? {
        val title = p[NOW_TITLE] ?: return null
        return NowTask(
            title = title,
            phase = p[NOW_PHASE]?.let { name -> NowPhase.entries.firstOrNull { it.name == name } } ?: NowPhase.Task,
            totalMs = p[NOW_TOTAL] ?: 0,
            startedAtMs = p[NOW_STARTED] ?: 0,
            endAtMs = p[NOW_END] ?: 0,
            pausedRemainingMs = p[NOW_PAUSED_LEFT] ?: 0,
            running = p[NOW_RUNNING] ?: false,
            spentMinutes = p[NOW_SPENT] ?: 0,
        )
    }

    private fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()

    companion object {
        const val MAX_ESSENTIALS = 7
        /** Paused apps on the free plan (Pro will lift this). */
        const val MAX_FREE_PAUSED = 3
        const val PAUSE_SECONDS = 10
        const val DEFAULT_BEDTIME = 23 * 60
        const val DEFAULT_DAY_START = 7 * 60
        private const val MAX_RECENT = 6

        private val ESSENTIALS = stringPreferencesKey("essential_apps")
        private val PAUSED = stringPreferencesKey("paused_apps")
        private val IS_PRO = booleanPreferencesKey("is_pro")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val FOCUS_BLOCKS = stringPreferencesKey("focus_blocks")
        private val BEDTIME = intPreferencesKey("bedtime_min")
        private val DAY_START = intPreferencesKey("day_start_min")
        private val CALENDAR_ON = booleanPreferencesKey("calendar_on")
        private val ONE_TEXT = stringPreferencesKey("one_thing_text")
        private val ONE_DATE = stringPreferencesKey("one_thing_date")
        private val ONE_DONE = booleanPreferencesKey("one_thing_done")
        private val CHECKIN_DATE = stringPreferencesKey("checkin_date")
        private val RECENT_TASKS = stringPreferencesKey("recent_tasks")
        private val NOW_TITLE = stringPreferencesKey("now_title")
        private val NOW_PHASE = stringPreferencesKey("now_phase")
        private val NOW_TOTAL = longPreferencesKey("now_total_ms")
        private val NOW_STARTED = longPreferencesKey("now_started_ms")
        private val NOW_END = longPreferencesKey("now_end_ms")
        private val NOW_PAUSED_LEFT = longPreferencesKey("now_paused_left_ms")
        private val NOW_RUNNING = booleanPreferencesKey("now_running")
        private val NOW_SPENT = intPreferencesKey("now_spent_min")
    }
}
