package org.mobile.growfy.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.mobile.growfy.core.config.AppConfig
import org.mobile.growfy.core.storage.AuthTokens
import org.mobile.growfy.core.storage.TokenStorage
import org.mobile.growfy.data.remote.dto.RefreshRequest
import org.mobile.growfy.data.remote.dto.TokensDto
import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.plugins.logging.Logger as KtorLogger

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

fun createHttpClient(
    engine: HttpClientEngine,
    config: AppConfig,
    tokenStorage: TokenStorage,
    sessionEvents: SessionEvents,
): HttpClient = HttpClient(engine) {
    // Non-2xx responses throw, and safeApiCall turns them into NetworkError.
    expectSuccess = true

    defaultRequest {
        url(config.baseUrl)
        contentType(ContentType.Application.Json)
        // The backend returns tokens in the body (not cookies) and skips CSRF checks for mobile.
        header("X-Client", "mobile")
    }

    install(ContentNegotiation) {
        json(AppJson)
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }

    // Only GETs are retried: replaying a POST could send a review reply twice.
    install(HttpRequestRetry) {
        maxRetries = 2
        retryIf { request, response ->
            request.method == HttpMethod.Get && response.status.value in 500..599
        }
        retryOnExceptionIf { request, cause ->
            request.method == HttpMethod.Get && cause !is CancellationException
        }
        exponentialDelay()
    }

    install(Auth) {
        bearer {
            sendWithoutRequest { true }
            loadTokens {
                tokenStorage.getTokens()?.let { BearerTokens(it.accessToken, it.refreshToken) }
            }
            refreshTokens {
                val refreshToken = oldTokens?.refreshToken ?: return@refreshTokens null
                try {
                    val tokens = client.post("auth/refresh") {
                        markAsRefreshTokenRequest()
                        setBody(RefreshRequest(refreshToken))
                    }.body<TokensDto>()
                    val access = tokens.accessToken ?: return@refreshTokens null
                    val saved = AuthTokens(access, tokens.refreshToken ?: refreshToken)
                    tokenStorage.saveTokens(saved)
                    BearerTokens(saved.accessToken, saved.refreshToken)
                } catch (e: ResponseException) {
                    when (e.response.status) {
                        // SESSION_REFRESHED: a concurrent call already rotated the token; reuse it.
                        HttpStatusCode.Conflict -> tokenStorage.getTokens()?.let { BearerTokens(it.accessToken, it.refreshToken) }
                        // Only a rejected refresh token ends the session; 5xx/429 (e.g. a server restart) keep it.
                        HttpStatusCode.Unauthorized -> {
                            tokenStorage.clear()
                            sessionEvents.notifyExpired()
                            null
                        }
                        else -> null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Offline or timeout: keep the session; the original call fails with a network error.
                    null
                }
            }
        }
    }

    install(Logging) {
        level = if (config.isDebug) LogLevel.BODY else LogLevel.NONE
        logger = object : KtorLogger {
            override fun log(message: String) = KermitLogger.withTag("HTTP").d(message)
        }
        sanitizeHeader { it == HttpHeaders.Authorization }
    }
}

/** Drops Ktor's cached bearer tokens so the next request reloads them from [TokenStorage]. */
fun HttpClient.resetAuthTokens() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
