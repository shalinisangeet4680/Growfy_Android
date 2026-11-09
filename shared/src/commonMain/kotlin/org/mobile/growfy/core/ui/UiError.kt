package org.mobile.growfy.core.ui

import org.mobile.growfy.core.network.NetworkError
import org.mobile.growfy.core.network.isUnknownRoute
import org.mobile.growfy.core.network.userMessage

enum class ErrorKind { Offline, NotAvailable, SessionEnded, Generic }

/** What a screen shows when its data can't load: picks the illustration and copy for [ErrorState]. */
data class UiError(val kind: ErrorKind, val title: String, val message: String)

fun NetworkError.toUiError(): UiError = when (this) {
    NetworkError.NoConnection -> UiError(ErrorKind.Offline, "You’re offline", "Check your connection and try again.")
    NetworkError.Timeout -> UiError(ErrorKind.Offline, "That took too long", "The connection is slow. Try again in a moment.")
    NetworkError.Unauthorized -> UiError(ErrorKind.SessionEnded, "Your session ended", "Sign in again to continue.")
    is NetworkError.Api ->
        if (isUnknownRoute) {
            UiError(ErrorKind.NotAvailable, "Coming soon", "This part of TreeVook isn’t live yet.")
        } else {
            UiError(ErrorKind.Generic, "Something went wrong", userMessage())
        }
    is NetworkError.Serialization, is NetworkError.Unknown -> UiError(ErrorKind.Generic, "Something went wrong", userMessage())
}
