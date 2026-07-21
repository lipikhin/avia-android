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
