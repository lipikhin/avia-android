package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.ArrivalBoxUpdateData
import com.aviatechnik.android.data.api.ArrivalBoxUpdateRequest
import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.MediaUploadData
import com.aviatechnik.android.data.api.StorageUpdateData
import com.aviatechnik.android.data.api.StorageUpdateRequest
import com.aviatechnik.android.data.api.WorkorderDetailData
import com.aviatechnik.android.data.api.WorkordersData
import com.aviatechnik.android.data.auth.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkorderRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    suspend fun list(scope: String, includeDone: Boolean): ApiResult<WorkordersData> =
        apiCall(tokenStore) { api.workorders(scope = scope, includeDone = includeDone) }

    suspend fun detail(id: Int): ApiResult<WorkorderDetailData> =
        apiCall(tokenStore) { api.workorder(id) }

    suspend fun updateStorage(id: Int, rack: Int?, level: Int?, column: Int?): ApiResult<StorageUpdateData> =
        apiCall(tokenStore) { api.updateStorage(id, StorageUpdateRequest(rack, level, column)) }

    suspend fun updateArrivalBox(id: Int, status: String?, notes: String?): ApiResult<ArrivalBoxUpdateData> =
        apiCall(tokenStore) { api.updateArrivalBox(id, ArrivalBoxUpdateRequest(status, notes)) }

    suspend fun uploadPhoto(id: Int, category: String, file: File): ApiResult<MediaUploadData> {
        val part = MultipartBody.Part.createFormData(
            "photos[]", file.name, file.asRequestBody("image/jpeg".toMediaType()),
        )
        val cat = category.toRequestBody("text/plain".toMediaType())
        return apiCall(tokenStore) { api.uploadWorkorderPhoto(id, part, cat) }
    }
}
