package com.aviatechnik.android.data.repository

import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.api.PasswordRequest
import com.aviatechnik.android.data.api.ProfileData
import com.aviatechnik.android.data.api.ProfileUpdateData
import com.aviatechnik.android.data.api.ProfileUpdateRequest
import com.aviatechnik.android.data.auth.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: AviaApi,
    private val tokenStore: TokenStore,
) {
    suspend fun load(): ApiResult<ProfileData> = apiCall(tokenStore) { api.profile() }

    suspend fun update(req: ProfileUpdateRequest, avatar: File?): ApiResult<ProfileUpdateData> {
        if (avatar == null) {
            return apiCall(tokenStore) { api.updateProfile(req) }
        }
        fun part(v: String?): RequestBody? = v?.toRequestBody("text/plain".toMediaType())
        return apiCall(tokenStore) {
            api.updateProfileWithAvatar(
                method = part("PUT")!!,
                name = part(req.name)!!,
                phone = part(req.phone),
                birthday = part(req.birthday),
                stamp = part(req.stamp)!!,
                teamId = part(req.teamId.toString())!!,
                file = MultipartBody.Part.createFormData(
                    "file", avatar.name, avatar.asRequestBody("image/jpeg".toMediaType()),
                ),
            )
        }
    }

    suspend fun changePassword(old: String, new: String, confirm: String): ApiResult<*> =
        apiCall(tokenStore) { api.updatePassword(PasswordRequest(old, new, confirm)) }
}
