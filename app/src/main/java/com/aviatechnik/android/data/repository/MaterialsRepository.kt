package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.MaterialUpdateData
import com.aviatechnik.android.data.api.MaterialUpdateRequest
import com.aviatechnik.android.data.api.MaterialsData
import com.aviatechnik.android.data.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialsRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    suspend fun list(): ApiResult<MaterialsData> = apiCall(tokenStore) { api.materials() }

    suspend fun updateDescription(id: Int, description: String): ApiResult<MaterialUpdateData> =
        apiCall(tokenStore) { api.updateMaterial(id, MaterialUpdateRequest(description)) }
}
