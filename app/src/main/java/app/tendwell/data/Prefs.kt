package app.tendwell.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Small settings that live only on the phone. */
class Prefs(context: Context) {
    private val store = context.applicationContext.dataStore

    /** Ordered app keys shown on Home, or null if the user has never set them (fresh install). */
    val essentialKeys: Flow<List<String>?> = store.data.map { prefs ->
        prefs[ESSENTIALS]?.let { raw -> raw.split('\n').filter { it.isNotBlank() } }
    }

    suspend fun setEssentialKeys(keys: List<String>) {
        store.edit { it[ESSENTIALS] = keys.joinToString("\n") }
    }

    companion object {
        const val MAX_ESSENTIALS = 7
        private val ESSENTIALS = stringPreferencesKey("essential_apps")
    }
}
