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

    @GET("draft/options")
    suspend fun draftOptions(): Envelope<DraftOptionsData>

    @POST("drafts")
    suspend fun storeDraft(@Body body: DraftCreateRequest): Envelope<WorkorderDetailData>

    @POST("draft-units")
    suspend fun storeDraftUnit(@Body body: DraftUnitRequest): Envelope<DraftUnitData>

    @GET("profile")
    suspend fun profile(): Envelope<ProfileData>

    @PUT("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Envelope<ProfileUpdateData>

    /** Avatar path: Laravel can't parse multipart on PUT → POST + _method=PUT. */
    @Multipart
    @POST("profile")
    suspend fun updateProfileWithAvatar(
        @Part("_method") method: okhttp3.RequestBody,
        @Part("name") name: okhttp3.RequestBody,
        @Part("phone") phone: okhttp3.RequestBody?,
        @Part("birthday") birthday: okhttp3.RequestBody?,
        @Part("stamp") stamp: okhttp3.RequestBody,
        @Part("team_id") teamId: okhttp3.RequestBody,
        @Part file: okhttp3.MultipartBody.Part,
    ): Envelope<ProfileUpdateData>

    @POST("profile/password")
    suspend fun updatePassword(@Body body: PasswordRequest): Envelope<kotlinx.serialization.json.JsonObject?>

    @GET("workorders/{id}/components")
    suspend fun components(@Path("id") id: Int): Envelope<ComponentsData>

    @POST("workorders/{id}/component-attachments")
    suspend fun storeAttachment(@Path("id") id: Int, @Body body: AttachmentRequest): Envelope<AttachmentData>

    @PATCH("component-attachments/{tdrId}")
    suspend fun updateAttachment(@Path("tdrId") tdrId: Int, @Body body: AttachmentRequest): Envelope<AttachmentData>

    @retrofit2.http.DELETE("component-attachments/{tdrId}")
    suspend fun deleteAttachment(@Path("tdrId") tdrId: Int): Envelope<kotlinx.serialization.json.JsonObject>

    @Multipart
    @POST("components/{id}/photo")
    suspend fun uploadComponentPhoto(
        @Path("id") id: Int,
        @Part photo: okhttp3.MultipartBody.Part, // form field name: photo
    ): Envelope<kotlinx.serialization.json.JsonObject>

    @GET("paint")
    suspend fun paint(): Envelope<PaintData>

    @Multipart
    @POST("paint/lost")
    suspend fun storePaintLost(
        @Part("part_number") partNumber: okhttp3.RequestBody,
        @Part("serial_number") serialNumber: okhttp3.RequestBody?,
        @Part("comment") comment: okhttp3.RequestBody?,
        @Part photo: okhttp3.MultipartBody.Part,
    ): Envelope<kotlinx.serialization.json.JsonObject>

    @retrofit2.http.DELETE("paint/lost/{id}")
    suspend fun deletePaintLost(@Path("id") id: Int): Envelope<kotlinx.serialization.json.JsonObject?>

    @POST("paint/messages")
    suspend fun sendPaintMessage(@Body body: PaintMessageRequest): Envelope<kotlinx.serialization.json.JsonObject?>

    @GET("machining")
    suspend fun machining(@Query("my_wo") myWo: Boolean = false): Envelope<MachiningData>

    @GET("machining/workorders/{id}")
    suspend fun machiningWorkorder(@Path("id") id: Int, @Query("my_wo") myWo: Boolean = false): Envelope<MachiningWoData>

    @PATCH("machining/steps/{id}")
    suspend fun updateMachiningStep(@Path("id") id: Int, @Body body: MachiningStepRequest): Envelope<kotlinx.serialization.json.JsonObject>

    @GET("materials")
    suspend fun materials(): Envelope<MaterialsData>

    @PATCH("materials/{id}")
    suspend fun updateMaterial(@Path("id") id: Int, @Body body: MaterialUpdateRequest): Envelope<MaterialUpdateData>

    @PATCH("tdr-processes/{id}/dates")
    suspend fun updateProcessDates(
        @Path("id") id: Int,
        @Body body: ProcessDatesRequest,
    ): Envelope<kotlinx.serialization.json.JsonObject>
}
