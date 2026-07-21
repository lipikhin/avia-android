package com.aviatechnik.android.data.repository

import android.os.Build
import com.aviatechnik.android.data.api.AppConfigData
import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.BootstrapData
import com.aviatechnik.android.data.api.LoginRequest
import com.aviatechnik.android.data.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    fun hasPersistedToken(): Boolean = tokenStore.token != null

    suspend fun appConfig(): ApiResult<AppConfigData> = apiCall(tokenStore) { api.appConfig() }

    suspend fun login(email: String, password: String, rememberMe: Boolean): ApiResult<Unit> {
        val device = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { null }
        return when (val res = apiCall(tokenStore) { api.login(LoginRequest(email, password, device)) }) {
            is ApiResult.Ok -> {
                tokenStore.save(res.data.token, persist = rememberMe)
                ApiResult.Ok(Unit)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun bootstrap(): ApiResult<BootstrapData> = apiCall(tokenStore) { api.bootstrap() }

    suspend fun logout() {
        runCatching { api.logout() } // best effort — the token dies locally regardless
        tokenStore.clear()
    }

    fun dropSession() = tokenStore.clear()
}
