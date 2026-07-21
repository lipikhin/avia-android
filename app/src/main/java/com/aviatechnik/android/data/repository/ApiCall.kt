package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.Envelope
import com.aviatechnik.android.data.auth.TokenStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed class ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val fieldErrors: Map<String, List<String>> = emptyMap()) : ApiResult<Nothing>()
}

/**
 * Shared envelope unwrapping: ok+data → Ok; HTTP 401 → drop the session and
 * surface a relogin message; other HTTP errors → server "message" if present.
 */
internal inline fun <T> apiCall(tokenStore: TokenStore, block: () -> Envelope<T>): ApiResult<T> {
    return try {
        val env = block()
        if (env.ok && env.data != null) ApiResult.Ok(env.data)
        else ApiResult.Error(env.message ?: "Request failed")
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 401) {
            tokenStore.clear()
            ApiResult.Error("Session expired. Please log in again.")
        } else {
            ApiResult.Error(parseHttpErrorMessage(e))
        }
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network error")
    }
}

internal fun parseHttpErrorMessage(e: retrofit2.HttpException): String {
    return try {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val el = Json { ignoreUnknownKeys = true }.parseToJsonElement(body)
        (el as? JsonObject)?.get("message")?.let { (it as? JsonPrimitive)?.content }
            ?: "Request failed (${e.code()})"
    } catch (_: Exception) {
        "Request failed (${e.code()})"
    }
}
