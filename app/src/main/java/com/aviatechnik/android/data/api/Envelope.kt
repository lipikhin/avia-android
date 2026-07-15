package com.aviatechnik.android.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Every avia mobile API response is wrapped in this envelope
 * (see docs/mobile-native-android-brief.md in the avia repo):
 *   success: { ok: true,  data: {...}, meta: {}, message: null }
 *   error:   { ok: false, message: "...", errors: { field: ["..."] } }
 */
@Serializable
data class Envelope<T>(
    val ok: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errors: JsonObject? = null,
)
