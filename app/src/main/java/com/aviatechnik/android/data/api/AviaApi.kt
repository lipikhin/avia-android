package com.aviatechnik.android.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
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

    @PATCH("workorders/{id}/storage")
    suspend fun updateStorage(@Path("id") id: Int, @Body body: StorageUpdateRequest): Envelope<StorageUpdateData>

    @PATCH("workorders/{id}/arrival-box")
    suspend fun updateArrivalBox(@Path("id") id: Int, @Body body: ArrivalBoxUpdateRequest): Envelope<ArrivalBoxUpdateData>

    @Multipart
    @POST("workorders/{id}/media")
    suspend fun uploadWorkorderPhoto(
        @Path("id") id: Int,
        @Part photo: okhttp3.MultipartBody.Part, // form field name: photos[]
        @Part("category") category: okhttp3.RequestBody,
    ): Envelope<MediaUploadData>

    @GET("workorders/{id}/tasks")
    suspend fun tasks(@Path("id") id: Int): Envelope<TasksData>

    @PUT("workorders/{id}/tasks/{taskId}/dates")
    suspend fun updateTaskDates(
        @Path("id") id: Int,
        @Path("taskId") taskId: Int,
        @Body body: TaskDatesRequest,
    ): Envelope<kotlinx.serialization.json.JsonObject>

    @GET("workorders/{id}/processes")
    suspend fun processes(@Path("id") id: Int): Envelope<ProcessesData>

    @PATCH("tdr-processes/{id}/dates")
    suspend fun updateProcessDates(
        @Path("id") id: Int,
        @Body body: ProcessDatesRequest,
    ): Envelope<kotlinx.serialization.json.JsonObject>
}
