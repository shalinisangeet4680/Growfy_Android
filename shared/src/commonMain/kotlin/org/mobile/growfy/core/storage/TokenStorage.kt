package org.mobile.growfy.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AuthTokens(val accessToken: String, val refreshToken: String?)

interface TokenStorage {
    val isLoggedIn: Flow<Boolean>
    suspend fun getTokens(): AuthTokens?
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun clear()
}

class DataStoreTokenStorage(private val dataStore: DataStore<Preferences>) : TokenStorage {

    override val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[ACCESS_TOKEN] != null }

    override suspend fun getTokens(): AuthTokens? {
        val prefs = dataStore.data.first()
        val access = prefs[ACCESS_TOKEN] ?: return null
        return AuthTokens(access, prefs[REFRESH_TOKEN])
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = tokens.accessToken
            tokens.refreshToken?.let { prefs[REFRESH_TOKEN] = it } ?: prefs.remove(REFRESH_TOKEN)
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(ACCESS_TOKEN)
            it.remove(REFRESH_TOKEN)
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
