package com.aviatechnik.android.data.repository

import android.os.Build
import com.aviatechnik.android.data.api.AppConfigData
import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.BootstrapData
import com.aviatechnik.android.data.api.LoginRequest
import com.aviatechnik.android.data.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val fieldErrors: Map<String, List<String>> = emptyMap()) : ApiResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    fun hasPersistedToken(): Boolean = tokenStore.token != null

    suspend fun appConfig(): ApiResult<AppConfigData> = call { api.appConfig() }

    suspend fun login(email: String, password: String, rememberMe: Boolean): ApiResult<Unit> {
        val device = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { null }
        return when (val res = call { api.login(LoginRequest(email, password, device)) }) {
            is ApiResult.Ok -> {
                tokenStore.save(res.data.token, persist = rememberMe)
                ApiResult.Ok(Unit)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun bootstrap(): ApiResult<BootstrapData> = call { api.bootstrap() }

    suspend fun logout() {
        runCatching { api.logout() } // best effort — the token dies locally regardless
        tokenStore.clear()
    }

    fun dropSession() = tokenStore.clear()

    private inline fun <T> call(block: () -> com.aviatechnik.android.data.api.Envelope<T>): ApiResult<T> {
        return try {
            val env = block()
            if (env.ok && env.data != null) ApiResult.Ok(env.data)
            else ApiResult.Error(env.message ?: "Request failed")
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                tokenStore.clear()
                ApiResult.Error("Session expired. Please log in again.")
            } else {
                ApiResult.Error(parseHttpError(e))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    private fun parseHttpError(e: retrofit2.HttpException): String {
        return try {
            val body = e.response()?.errorBody()?.string().orEmpty()
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val el = json.parseToJsonElement(body)
            (el as? kotlinx.serialization.json.JsonObject)
                ?.get("message")
                ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                ?: "Request failed (${e.code()})"
        } catch (_: Exception) {
            "Request failed (${e.code()})"
        }
    }
}
