package org.mobile.growfy.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Fires when the refresh token is rejected, so the app can return to sign-in. */
class SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
