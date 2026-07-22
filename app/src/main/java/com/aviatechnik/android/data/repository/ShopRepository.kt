package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.MachiningData
import com.aviatechnik.android.data.api.MachiningStepRequest
import com.aviatechnik.android.data.api.MachiningWoData
import com.aviatechnik.android.data.api.PaintData
import com.aviatechnik.android.data.api.PaintMessageRequest
import com.aviatechnik.android.data.auth.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Paint + Machining shop sections (role-gated on the server). */
@Singleton
class ShopRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    suspend fun paint(): ApiResult<PaintData> = apiCall(tokenStore) { api.paint() }

    suspend fun addLost(partNumber: String, serialNumber: String?, comment: String?, photo: File): ApiResult<*> {
        fun part(v: String?): RequestBody? = v?.toRequestBody("text/plain".toMediaType())
        return apiCall(tokenStore) {
            api.storePaintLost(
                partNumber = part(partNumber)!!,
                serialNumber = part(serialNumber),
                comment = part(comment),
                photo = MultipartBody.Part.createFormData(
                    "photo", photo.name, photo.asRequestBody("image/jpeg".toMediaType()),
                ),
            )
        }
    }

    suspend fun deleteLost(id: Int): ApiResult<*> = apiCall(tokenStore) { api.deletePaintLost(id) }

    suspend fun sendOwnerMessage(userId: Int, message: String): ApiResult<*> =
        apiCall(tokenStore) { api.sendPaintMessage(PaintMessageRequest(userId, message)) }

    suspend fun machining(myWo: Boolean): ApiResult<MachiningData> =
        apiCall(tokenStore) { api.machining(myWo) }

    suspend fun machiningWorkorder(id: Int, myWo: Boolean): ApiResult<MachiningWoData> =
        apiCall(tokenStore) { api.machiningWorkorder(id, myWo) }

    /** field: "start" | "finish" — only the changed field is serialized. */
    suspend fun updateStepDate(stepId: Int, field: String, date: String): ApiResult<*> =
        apiCall(tokenStore) {
            api.updateMachiningStep(
                stepId,
                if (field == "start") MachiningStepRequest(dateStart = date)
                else MachiningStepRequest(dateFinish = date),
            )
        }
}
