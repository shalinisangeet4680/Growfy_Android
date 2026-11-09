package org.mobile.growfy.data.repository

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.NetworkError
import org.mobile.growfy.core.network.resetAuthTokens
import org.mobile.growfy.core.network.safeApiCall
import org.mobile.growfy.core.storage.AppPreferences
import org.mobile.growfy.core.storage.AuthTokens
import org.mobile.growfy.core.storage.TokenStorage
import org.mobile.growfy.data.remote.AuthApi
import org.mobile.growfy.domain.model.Me
import org.mobile.growfy.domain.model.OtpChallenge
import org.mobile.growfy.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val client: HttpClient,
    private val tokens: TokenStorage,
    private val prefs: AppPreferences,
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> = tokens.isLoggedIn

    override suspend fun requestCode(phone: String): ApiResult<OtpChallenge> = safeApiCall {
        api.requestOtp(phone).let { OtpChallenge(it.phone, it.resendInSeconds) }
    }

    override suspend fun verifyCode(phone: String, code: String): ApiResult<Unit> {
        val result = safeApiCall { api.verifyOtp(phone, code) }
        if (result !is ApiResult.Success) return result as ApiResult.Failure
        val access = result.data.accessToken
            ?: return ApiResult.Failure(NetworkError.Api(200, "NO_TOKENS", "Sign-in didn’t return a session. Update the app."))
        tokens.saveTokens(AuthTokens(access, result.data.refreshToken))
        client.resetAuthTokens()
        return ApiResult.Success(Unit)
    }

    override suspend fun me(): ApiResult<Me> = safeApiCall {
        api.me().let { Me(it.user.name, it.user.email, it.user.phone, it.workspace.name) }
    }

    override suspend fun signOut() {
        safeApiCall { api.logout() }
        tokens.clear()
        prefs.clear()
        client.resetAuthTokens()
    }
}
