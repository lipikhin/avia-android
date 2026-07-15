package com.aviatechnik.android.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
}
