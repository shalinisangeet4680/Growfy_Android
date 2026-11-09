package org.mobile.growfy.core.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.mobile.growfy.core.config.AppConfig
import org.mobile.growfy.core.storage.AuthTokens
import org.mobile.growfy.core.storage.TokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpClientFactoryTest {

    @Serializable
    private data class Ping(val status: String)

    private val config = AppConfig(baseUrl = "https://api.test/", isDebug = false)

    private class FakeTokenStorage(private val tokens: AuthTokens?) : TokenStorage {
        override val isLoggedIn: Flow<Boolean> = flowOf(tokens != null)
        override suspend fun getTokens() = tokens
        override suspend fun saveTokens(tokens: AuthTokens) = Unit
        override suspend fun clear() = Unit
    }

    private fun client(tokens: AuthTokens? = null, handler: MockRequestHandler) =
        createHttpClient(MockEngine(handler), config, FakeTokenStorage(tokens), SessionEvents())

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun resolvesBaseUrlAndIgnoresUnknownFields() = runTest {
        var requestedUrl = ""
        val client = client { request ->
            requestedUrl = request.url.toString()
            respond("""{"status":"ok","extra":1}""", HttpStatusCode.OK, jsonHeaders)
        }
        val result = safeApiCall { client.get("health").body<Ping>() }
        assertEquals(ApiResult.Success(Ping("ok")), result)
        assertEquals("https://api.test/health", requestedUrl)
    }

    @Test
    fun attachesBearerToken() = runTest {
        var authHeader: String? = null
        val client = client(tokens = AuthTokens("abc", null)) { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"status":"ok"}""", HttpStatusCode.OK, jsonHeaders)
        }
        safeApiCall { client.get("health").body<Ping>() }
        assertEquals("Bearer abc", authHeader)
    }

    @Test
    fun sendsMobileClientHeader() = runTest {
        var clientHeader: String? = null
        val client = client { request ->
            clientHeader = request.headers["X-Client"]
            respond("""{"status":"ok"}""", HttpStatusCode.OK, jsonHeaders)
        }
        safeApiCall { client.get("health").body<Ping>() }
        assertEquals("mobile", clientHeader)
    }

    @Test
    fun mapsBackendErrorBody() = runTest {
        val client = client {
            respond("""{"code":"VALIDATION","message":"Write the post text.","traceId":"t1"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }
        val result = safeApiCall { client.get("health").body<Ping>() }
        assertEquals(ApiResult.Failure(NetworkError.Api(400, "VALIDATION", "Write the post text.")), result)
    }

    @Test
    fun mapsServerErrorsAfterRetries() = runTest {
        var calls = 0
        val client = client { calls++; respond("", HttpStatusCode.ServiceUnavailable) }
        val result = safeApiCall { client.get("health").body<Ping>() }
        assertEquals(ApiResult.Failure(NetworkError.Api(503, null, null)), result)
        assertEquals(3, calls)
    }
}
