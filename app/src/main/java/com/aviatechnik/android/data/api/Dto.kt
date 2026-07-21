package com.aviatechnik.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/* ── public/app-config ─────────────────────────────────────────── */

@Serializable
data class AppConfigData(
    val app: AppConfigApp,
    val auth: AppConfigAuth,
    val launch: AppConfigLaunch,
)

@Serializable
data class AppConfigApp(
    val name: String,
    @SerialName("bundle_display_name") val bundleDisplayName: String? = null,
    val theme: String = "dark",
    val logo: AppConfigLogo? = null,
    val background: AppConfigBackground? = null,
    val platform: String? = null,
)

@Serializable
data class AppConfigLogo(
    @SerialName("favicon_url") val faviconUrl: String? = null,
    @SerialName("hero_image_url") val heroImageUrl: String? = null,
)

@Serializable
data class AppConfigBackground(
    @SerialName("gradient_start") val gradientStart: String? = null,
    @SerialName("gradient_end") val gradientEnd: String? = null,
    @SerialName("hero_image_mobile_width") val heroImageMobileWidth: Int? = null,
)

@Serializable
data class AppConfigAuth(
    @SerialName("login_title") val loginTitle: String = "Login",
    @SerialName("email_label") val emailLabel: String = "Email Address",
    @SerialName("password_label") val passwordLabel: String = "Password",
    @SerialName("submit_label") val submitLabel: String = "Login",
    @SerialName("remember_me_supported") val rememberMeSupported: Boolean = true,
    @SerialName("forgot_password_supported") val forgotPasswordSupported: Boolean = false,
    @SerialName("forgot_password_url") val forgotPasswordUrl: String? = null,
)

@Serializable
data class AppConfigLaunch(
    @SerialName("show_splash") val showSplash: Boolean = true,
    @SerialName("initial_route") val initialRoute: String = "login",
)

/* ── auth ──────────────────────────────────────────────────────── */

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class LoginData(
    val token: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Int,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,          // server sends the role NAME, not an object
    val team: TeamDto? = null,
    val capabilities: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String? = null,
)

/* ── workorders ────────────────────────────────────────────────── */

@Serializable
data class WorkordersData(
    val items: List<WorkorderItemDto> = emptyList(),
)

@Serializable
data class WorkorderItemDto(
    val id: Int,
    val number: Long,
    @SerialName("number_display") val numberDisplay: String? = null,
    @SerialName("is_draft") val isDraft: Boolean = false,
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("done_at") val doneAt: String? = null,
    @SerialName("open_at") val openAt: String? = null,
    val approved: Boolean = false,
    @SerialName("owned_by_current_user") val ownedByCurrentUser: Boolean = false,
    val customer: CustomerDto? = null,
    val unit: UnitDto? = null,
)

@Serializable
data class CustomerDto(
    val id: Int,
    val name: String? = null,
)

@Serializable
data class UnitDto(
    val id: Int,
    @SerialName("part_number") val partNumber: String? = null,
    val name: String? = null,
    val description: String? = null,
)

/* ── workorder detail ──────────────────────────────────────────── */

@Serializable
data class WorkorderDetailData(
    val workorder: WorkorderDetailDto,
)

@Serializable
data class NamedDto(
    val id: Int,
    val name: String? = null,
)

@Serializable
data class WorkorderDetailDto(
    val id: Int,
    val number: Long,
    @SerialName("number_display") val numberDisplay: String? = null,
    @SerialName("is_draft") val isDraft: Boolean = false,
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("done_at") val doneAt: String? = null,
    @SerialName("open_at") val openAt: String? = null,
    val approved: Boolean = false,
    val owner: NamedDto? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    val description: String? = null,
    @SerialName("customer_po") val customerPo: String? = null,
    val customer: NamedDto? = null,
    val instruction: NamedDto? = null,
    val unit: UnitDto? = null,
    @SerialName("approve_at") val approveAt: String? = null,
    @SerialName("approve_name") val approveName: String? = null,
    val storage: StorageDto? = null,
    @SerialName("arrival_box") val arrivalBox: ArrivalBoxDto? = null,
    @SerialName("media_groups") val mediaGroups: List<MediaGroupDto> = emptyList(),
)

@Serializable
data class StorageDto(
    val rack: Int? = null,
    val level: Int? = null,
    val column: Int? = null,
    val location: String? = null,
    @SerialName("can_update") val canUpdate: Boolean = false,
)

@Serializable
data class ArrivalBoxDto(
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    val notes: String? = null,
    @SerialName("recorded_by") val recordedBy: Long? = null,       // user id (int on the wire)
    @SerialName("recorded_by_name") val recordedByName: String? = null, // Android contour extra
    @SerialName("recorded_at") val recordedAt: String? = null,
    @SerialName("can_update") val canUpdate: Boolean = false,
)

@Serializable
data class MediaGroupDto(
    val key: String,
    val label: String? = null,
    val count: Int = 0,
    val media: List<MediaDto> = emptyList(),
)

@Serializable
data class MediaDto(
    val id: Int,
    val name: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    val url: String? = null,
)

@Serializable
data class StorageUpdateRequest(
    @SerialName("storage_rack") val rack: Int? = null,
    @SerialName("storage_level") val level: Int? = null,
    @SerialName("storage_column") val column: Int? = null,
)

@Serializable
data class StorageUpdateData(val storage: StorageDto)

@Serializable
data class ArrivalBoxUpdateRequest(
    @SerialName("arrival_box_status") val status: String? = null,
    @SerialName("arrival_box_notes") val notes: String? = null,
)

@Serializable
data class ArrivalBoxUpdateData(@SerialName("arrival_box") val arrivalBox: ArrivalBoxDto)

@Serializable
data class MediaUploadData(
    val media: List<MediaDto> = emptyList(),
    @SerialName("photo_count") val photoCount: Int = 0,
)

/* ── tasks ─────────────────────────────────────────────────────── */

@Serializable
data class TasksData(
    val groups: List<TaskGroupDto> = emptyList(),
)

@Serializable
data class TaskGroupDto(
    val id: Int,
    val name: String? = null,
    @SerialName("is_done") val isDone: Boolean = false,
    val tasks: List<TaskDto> = emptyList(),
)

@Serializable
data class TaskDto(
    val id: Int,
    val name: String? = null,
    @SerialName("has_start_date") val hasStartDate: Boolean = false,
    @SerialName("can_edit_start") val canEditStart: Boolean = false,
    @SerialName("can_edit_finish") val canEditFinish: Boolean = false,
    val main: MainDto? = null,
)

@Serializable
data class MainDto(
    val id: Int? = null,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("date_finish") val dateFinish: String? = null,
    @SerialName("ignore_row") val ignoreRow: Boolean = false,
    val user: NamedDto? = null,
)

/** Only the field being changed is serialized (explicitNulls=false). */
@Serializable
data class TaskDatesRequest(
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("date_finish") val dateFinish: String? = null,
)

/* ── processes ─────────────────────────────────────────────────── */

@Serializable
data class ProcessesData(
    val components: List<ProcessComponentDto> = emptyList(),
)

@Serializable
data class ProcessComponentDto(
    val id: Int,
    val name: String? = null,
    @SerialName("ipl_num") val iplNum: String? = null,
    @SerialName("part_number") val partNumber: String? = null,
    val processes: List<TdrProcessDto> = emptyList(),
)

@Serializable
data class TdrProcessDto(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    @SerialName("repair_order") val repairOrder: String? = null,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("date_finish") val dateFinish: String? = null,
    @SerialName("date_promise") val datePromise: String? = null,
    @SerialName("can_edit_start") val canEditStart: Boolean = false,
    @SerialName("can_edit_finish") val canEditFinish: Boolean = false,
    @SerialName("can_edit_promise") val canEditPromise: Boolean = false,
)

@Serializable
data class ProcessDatesRequest(
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("date_finish") val dateFinish: String? = null,
    @SerialName("date_promise") val datePromise: String? = null,
)

/* ── materials ─────────────────────────────────────────────────── */

@Serializable
data class MaterialsData(
    val items: List<MaterialDto> = emptyList(),
)

@Serializable
data class MaterialDto(
    val id: Int,
    val code: String? = null,
    val material: String? = null,
    val specification: String? = null,
    val description: String? = null,
)

@Serializable
data class MaterialUpdateRequest(val description: String? = null)

@Serializable
data class MaterialUpdateData(val material: MaterialDto)

/* ── components / TDR ──────────────────────────────────────────── */

@Serializable
data class ComponentsData(
    @SerialName("attached_components") val attachedComponents: List<ComponentDto> = emptyList(),
    @SerialName("manual_components") val manualComponents: List<ComponentDto> = emptyList(),
    val codes: List<NamedDto> = emptyList(),
    val necessaries: List<NamedDto> = emptyList(),
)

@Serializable
data class ComponentDto(
    val id: Int,
    val name: String? = null,
    @SerialName("ipl_num") val iplNum: String? = null,
    @SerialName("part_number") val partNumber: String? = null,
    @SerialName("eff_code") val effCode: String? = null,
    @SerialName("is_bush") val isBush: Boolean = false,
    @SerialName("log_card") val logCard: Boolean = false,
    val text: String? = null,
    val photo: MediaDto? = null,
    val tdrs: List<TdrAttachmentDto> = emptyList(),
)

@Serializable
data class TdrAttachmentDto(
    val id: Int,
    @SerialName("component_id") val componentId: Int? = null,
    @SerialName("code_id") val codeId: Int? = null,
    @SerialName("code_name") val codeName: String? = null,
    @SerialName("necessaries_id") val necessariesId: Int? = null,
    @SerialName("necessaries_name") val necessariesName: String? = null,
    val qty: Int? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
)

@Serializable
data class AttachmentRequest(
    @SerialName("component_id") val componentId: Int? = null,
    @SerialName("code_id") val codeId: Int,
    @SerialName("necessaries_id") val necessariesId: Int? = null,
    val qty: Int? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
)

@Serializable
data class AttachmentData(val attachment: TdrAttachmentDto)

/* ── profile ───────────────────────────────────────────────────── */

@Serializable
data class ProfileData(
    val profile: ProfileDto,
    val teams: List<NamedDto> = emptyList(),
)

@Serializable
data class ProfileUpdateData(val profile: ProfileDto)

@Serializable
data class ProfileDto(
    val id: Int,
    val name: String? = null,
    val phone: String? = null,
    val birthday: String? = null,
    val email: String? = null,
    val stamp: String? = null,
    val team: NamedDto? = null,
    val avatar: MediaDto? = null,
)

@Serializable
data class ProfileUpdateRequest(
    val name: String,
    val phone: String? = null,
    val birthday: String? = null,
    val stamp: String,
    @SerialName("team_id") val teamId: Int,
)

@Serializable
data class PasswordRequest(
    @SerialName("old_pass") val oldPass: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

/* ── bootstrap ─────────────────────────────────────────────────────
 * The payload is large and server-driven; the shell only needs a few
 * typed fields — the rest stays as JSON for the screens that use it. */

@Serializable
data class BootstrapData(
    val user: UserDto,
    @SerialName("menu_mode") val menuMode: String = "workorders",
    @SerialName("available_menu_modes") val availableMenuModes: List<String> = emptyList(),
    @SerialName("display_date_format") val displayDateFormat: String = "dd/mmm/yyyy",
    val navigation: JsonObject? = null,
    val screens: JsonObject? = null,
)
