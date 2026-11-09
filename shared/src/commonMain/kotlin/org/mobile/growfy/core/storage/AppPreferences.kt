package org.mobile.growfy.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Per-device UI state. Anything that should follow the user across devices lives on the backend. */
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    val swipeHintSeen: Flow<Boolean> = dataStore.data.map { it[SWIPE_HINT_SEEN] == true }

    /** Location picked in the Home/Reviews chip; null = all locations. */
    val selectedLocationId: Flow<String?> = dataStore.data.map { it[SELECTED_LOCATION] }

    suspend fun setSwipeHintSeen() {
        dataStore.edit { it[SWIPE_HINT_SEEN] = true }
    }

    suspend fun setSelectedLocationId(id: String?) {
        dataStore.edit { if (id == null) it.remove(SELECTED_LOCATION) else it[SELECTED_LOCATION] = id }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(SWIPE_HINT_SEEN)
            it.remove(SELECTED_LOCATION)
        }
    }

    private companion object {
        val SWIPE_HINT_SEEN = booleanPreferencesKey("swipe_hint_seen")
        val SELECTED_LOCATION = stringPreferencesKey("selected_location")
    }
}
