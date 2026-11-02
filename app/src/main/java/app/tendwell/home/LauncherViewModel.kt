package app.tendwell.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tendwell.TendwellApp
import app.tendwell.apps.AppEntry
import app.tendwell.data.OpenLogFormat
import app.tendwell.data.PauseOutcome
import app.tendwell.data.Prefs
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherState(
    /** All launchable apps, alphabetical. Empty while loading. */
    val allApps: List<AppEntry> = emptyList(),
    /** The apps on Home, in the user's order. */
    val essentials: List<AppEntry> = emptyList(),
    val loaded: Boolean = false,
    /** Keys of apps that get the pause screen. */
    val pausedKeys: Set<String> = emptySet(),
) {
    val essentialKeys: Set<String> get() = essentials.mapTo(HashSet()) { it.key }
    val homeIsFull: Boolean get() = essentials.size >= Prefs.MAX_ESSENTIALS
    val pausedApps: List<AppEntry> get() = allApps.filter { it.key in pausedKeys }
    val pauseIsFull: Boolean get() = pausedApps.size >= Prefs.MAX_FREE_PAUSED
}

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TendwellApp).apps
    private val prefs = (app as TendwellApp).prefs

    private val openLog = (app as TendwellApp).openLog

    val state: StateFlow<LauncherState> = combine(repo.apps, prefs.essentialKeys, prefs.pausedKeys) { apps, keys, paused ->
        if (apps == null) return@combine LauncherState()
        val byKey = apps.associateBy { it.key }
        LauncherState(
            allApps = apps,
            // Uninstalled apps simply drop out; their keys stay saved in case they come back.
            essentials = keys.orEmpty().mapNotNull { byKey[it] },
            loaded = true,
            pausedKeys = paused,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    init {
        viewModelScope.launch { openLog.prune() }
        // First run: fill Home with the phone's default essentials.
        viewModelScope.launch {
            val keys = prefs.essentialKeys.first()
            if (keys == null) {
                val apps = repo.apps.filterNotNull().first()
                prefs.setEssentialKeys(repo.defaultEssentialKeys(apps))
            }
        }
    }

    fun launch(app: AppEntry) = repo.launch(app)

    /** Opens a paused app after its pause and notes it. Returns false if it could not be started. */
    fun openAfterPause(app: AppEntry): Boolean {
        val ok = repo.launch(app)
        if (ok) viewModelScope.launch { openLog.record(app.key, PauseOutcome.Opened) }
        return ok
    }

    fun turnedBack(app: AppEntry) {
        viewModelScope.launch { openLog.record(app.key, PauseOutcome.TurnedBack) }
    }

    /** How many times this paused app has been opened today, before this time. */
    suspend fun opensToday(app: AppEntry): Int {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = openLog.eventsSince(start)
        return OpenLogFormat.opensBetween(events, app.key, start, Long.MAX_VALUE)
    }

    fun setPaused(app: AppEntry, paused: Boolean) {
        viewModelScope.launch {
            val current = prefs.pausedKeys.first()
            val next = if (paused) {
                if (current.size >= Prefs.MAX_FREE_PAUSED) current else current + app.key
            } else {
                current - app.key
            }
            prefs.setPausedKeys(next)
        }
    }

    fun openAppInfo(app: AppEntry) = repo.openAppInfo(app)

    fun addToHome(app: AppEntry) = editEssentials { keys ->
        if (app.key in keys || keys.size >= Prefs.MAX_ESSENTIALS) keys else keys + app.key
    }

    fun removeFromHome(app: AppEntry) = editEssentials { keys -> keys - app.key }

    fun moveOnHome(app: AppEntry, by: Int) = editEssentials { keys ->
        val from = keys.indexOf(app.key)
        val to = from + by
        if (from < 0 || to !in keys.indices) keys
        else keys.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun editEssentials(change: (List<String>) -> List<String>) {
        viewModelScope.launch {
            // Keep saved keys of apps that are temporarily missing (e.g. on an unmounted SD card).
            val saved = prefs.essentialKeys.first().orEmpty()
            prefs.setEssentialKeys(change(saved))
        }
    }

    fun isDefaultHome(): Boolean = repo.isDefaultHome()

    fun homeSettingsIntent() = repo.homeSettingsIntent()

    fun refreshApps() = repo.refresh()
}
