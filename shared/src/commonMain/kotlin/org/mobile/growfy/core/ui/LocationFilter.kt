package org.mobile.growfy.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mobile.growfy.core.storage.AppPreferences

/** The location picked on Home or Reviews, shared by every tab and remembered across launches. `null` = all. */
class LocationFilter(private val prefs: AppPreferences) {
    private val scope = CoroutineScope(SupervisorJob())

    val selectedId: StateFlow<String?> = prefs.selectedLocationId.stateIn(scope, SharingStarted.Eagerly, null)

    fun select(locationId: String?) {
        scope.launch { prefs.setSelectedLocationId(locationId) }
    }
}
