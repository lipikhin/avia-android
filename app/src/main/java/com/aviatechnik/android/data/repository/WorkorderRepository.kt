package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.WorkorderDetailData
import com.aviatechnik.android.data.api.WorkordersData
import com.aviatechnik.android.data.auth.TokenStore
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
}
