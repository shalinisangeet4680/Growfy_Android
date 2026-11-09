package org.mobile.growfy.core.ui

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class UserMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null,
)

/**
 * App-scoped queue of snackbar messages. ViewModels post here so a message
 * (e.g. "Reply posted · Undo") outlives the screen that triggered it.
 */
class Messenger {
    private val channel = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = channel.receiveAsFlow()

    fun show(text: String, actionLabel: String? = null, onAction: (suspend () -> Unit)? = null) {
        channel.trySend(UserMessage(text, actionLabel, onAction))
    }
}
