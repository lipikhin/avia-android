package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.DraftCreateRequest
import com.aviatechnik.android.data.api.DraftOptionsData
import com.aviatechnik.android.data.api.DraftUnitData
import com.aviatechnik.android.data.api.DraftUnitRequest
import com.aviatechnik.android.data.api.WorkorderDetailData
import com.aviatechnik.android.data.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftsRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    suspend fun options(): ApiResult<DraftOptionsData> = apiCall(tokenStore) { api.draftOptions() }

    suspend fun create(req: DraftCreateRequest): ApiResult<WorkorderDetailData> =
        apiCall(tokenStore) { api.storeDraft(req) }

    suspend fun createUnit(partNumber: String, name: String?): ApiResult<DraftUnitData> =
        apiCall(tokenStore) { api.storeDraftUnit(DraftUnitRequest(partNumber, name)) }
}
