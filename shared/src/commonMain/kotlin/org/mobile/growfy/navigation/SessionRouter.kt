package org.mobile.growfy.navigation

import kotlinx.coroutines.flow.first
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.domain.repository.AuthRepository
import org.mobile.growfy.domain.repository.GoogleAccountRepository

/** Decides where a signed-in user lands: onboarding until a Google account is connected, then the app. */
class SessionRouter(private val auth: AuthRepository, private val google: GoogleAccountRepository) {
    suspend fun destination(): Route {
        if (!auth.isSignedIn.first()) return Route.Login
        return when (val result = google.connections()) {
            is ApiResult.Success -> if (result.data.isEmpty()) Route.Onboarding else Route.Main
            // Offline or server error: open the app; its screens show the error and retry.
            is ApiResult.Failure -> Route.Main
        }
    }
}
