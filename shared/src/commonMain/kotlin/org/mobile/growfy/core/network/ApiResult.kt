package org.mobile.growfy.core.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: NetworkError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> = also { if (it is ApiResult.Success) block(it.data) }

inline fun <T> ApiResult<T>.onFailure(block: (NetworkError) -> Unit): ApiResult<T> = also { if (it is ApiResult.Failure) block(it.error) }

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

@Serializable
data class ApiErrorBody(val code: String? = null, val message: String? = null)

/**
 * Runs a Ktor call and maps every expected failure to [NetworkError], so repositories
 * never leak exceptions to the UI. Relies on `expectSuccess = true` in [createHttpClient].
 */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: ResponseException) {
    val status = e.response.status
    if (status == HttpStatusCode.Unauthorized) {
        ApiResult.Failure(NetworkError.Unauthorized)
    } else {
        val body = runCatching { AppJson.decodeFromString<ApiErrorBody>(e.response.bodyAsText()) }.getOrNull()
        ApiResult.Failure(NetworkError.Api(status.value, body?.code, body?.message))
    }
} catch (e: HttpRequestTimeoutException) {
    ApiResult.Failure(NetworkError.Timeout)
} catch (e: SerializationException) {
    ApiResult.Failure(NetworkError.Serialization(e))
} catch (e: IOException) {
    ApiResult.Failure(NetworkError.NoConnection)
} catch (e: Exception) {
    ApiResult.Failure(NetworkError.Unknown(e))
}
