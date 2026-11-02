package app.tendwell.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tendwell.TendwellApp
import app.tendwell.apps.AppEntry
import app.tendwell.data.Prefs
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
) {
    val essentialKeys: Set<String> get() = essentials.mapTo(HashSet()) { it.key }
    val homeIsFull: Boolean get() = essentials.size >= Prefs.MAX_ESSENTIALS
}

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TendwellApp).apps
    private val prefs = (app as TendwellApp).prefs

    val state: StateFlow<LauncherState> = combine(repo.apps, prefs.essentialKeys) { apps, keys ->
        if (apps == null) return@combine LauncherState()
        val byKey = apps.associateBy { it.key }
        LauncherState(
            allApps = apps,
            // Uninstalled apps simply drop out; their keys stay saved in case they come back.
            essentials = keys.orEmpty().mapNotNull { byKey[it] },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    init {
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
