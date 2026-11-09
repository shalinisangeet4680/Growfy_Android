package org.mobile.growfy.core.network

sealed interface NetworkError {
    data object NoConnection : NetworkError
    data object Timeout : NetworkError
    data object Unauthorized : NetworkError

    /** Any non-2xx reply. [code] and [message] come from the backend's `{ code, message }` error body. */
    data class Api(val status: Int, val code: String?, val message: String?) : NetworkError

    data class Serialization(val cause: Throwable) : NetworkError
    data class Unknown(val cause: Throwable) : NetworkError
}

/** True for a 404, which also means "this endpoint isn't deployed yet" for the proposed APIs. */
val NetworkError.isNotFound: Boolean get() = this is NetworkError.Api && status == 404

private val UNKNOWN_ROUTE = Regex("^Cannot (GET|POST|PUT|PATCH|DELETE) ")

/**
 * The route doesn't exist on the backend (an endpoint from docs/backend-gaps.md not deployed yet),
 * as opposed to a record that wasn't found. Nest answers unknown routes with "Cannot GET /path".
 */
val NetworkError.isUnknownRoute: Boolean
    get() = this is NetworkError.Api && status == 404 && (code == null || message?.let(UNKNOWN_ROUTE::containsMatchIn) == true)

fun NetworkError.userMessage(): String = when (this) {
    NetworkError.NoConnection -> "No internet connection"
    NetworkError.Timeout -> "That took too long. Try again."
    NetworkError.Unauthorized -> "Your session ended. Sign in again."
    is NetworkError.Api -> when {
        isUnknownRoute -> "This isn’t available yet"
        message != null -> message
        status >= 500 -> "Something went wrong on our side"
        else -> "Couldn’t complete that"
    }
    is NetworkError.Serialization, is NetworkError.Unknown -> "Something went wrong"
}
