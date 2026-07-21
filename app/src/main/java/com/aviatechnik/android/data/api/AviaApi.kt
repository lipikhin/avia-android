package com.aviatechnik.android.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Android contour of the avia mobile API — base path /api/android/ */
interface AviaApi {

    @GET("public/app-config")
    suspend fun appConfig(): Envelope<AppConfigData>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Envelope<LoginData>

    @POST("auth/logout")
    suspend fun logout(): Envelope<Unit>

    @GET("bootstrap")
    suspend fun bootstrap(): Envelope<BootstrapData>

    /** scope: my | all | done | draft; search filters by WO number. */
    @GET("workorders")
    suspend fun workorders(
        @Query("scope") scope: String = "my",
        @Query("search") search: String = "",
        @Query("include_done") includeDone: Boolean = false,
    ): Envelope<WorkordersData>

    @GET("workorders/{id}")
    suspend fun workorder(@Path("id") id: Int): Envelope<WorkorderDetailData>
}
