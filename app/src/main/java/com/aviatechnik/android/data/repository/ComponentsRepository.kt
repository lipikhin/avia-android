package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.AttachmentData
import com.aviatechnik.android.data.api.AttachmentRequest
import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.ComponentsData
import com.aviatechnik.android.data.auth.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentsRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    suspend fun list(woId: Int): ApiResult<ComponentsData> =
        apiCall(tokenStore) { api.components(woId) }

    suspend fun attach(woId: Int, body: AttachmentRequest): ApiResult<AttachmentData> =
        apiCall(tokenStore) { api.storeAttachment(woId, body) }

    suspend fun updateAttachment(tdrId: Int, body: AttachmentRequest): ApiResult<AttachmentData> =
        apiCall(tokenStore) { api.updateAttachment(tdrId, body) }

    suspend fun deleteAttachment(tdrId: Int): ApiResult<*> =
        apiCall(tokenStore) { api.deleteAttachment(tdrId) }

    suspend fun uploadPhoto(componentId: Int, file: File): ApiResult<*> {
        val part = MultipartBody.Part.createFormData(
            "photo", file.name, file.asRequestBody("image/jpeg".toMediaType()),
        )
        return apiCall(tokenStore) { api.uploadComponentPhoto(componentId, part) }
    }
}
