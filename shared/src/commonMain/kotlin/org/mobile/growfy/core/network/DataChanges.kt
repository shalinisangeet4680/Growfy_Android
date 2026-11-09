package org.mobile.growfy.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

enum class DataKind { Reviews, Posts, Locations, Templates, Connections }

/**
 * Repositories announce mutations here; screens that show that data reload.
 * Keeps tabs in step (send a reply on Reply → Home and Reviews refresh) without a cache layer.
 * One mutation is one event, so a screen watching several kinds reloads once, not once per kind.
 */
class DataChanges {
    private val events = MutableSharedFlow<Set<DataKind>>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun notify(vararg kinds: DataKind) {
        events.tryEmit(kinds.toSet())
    }

    fun of(vararg kinds: DataKind): Flow<Unit> = events.filter { changed -> kinds.any { it in changed } }.map { }
}
