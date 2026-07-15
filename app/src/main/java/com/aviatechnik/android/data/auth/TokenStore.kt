package com.aviatechnik.android.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bearer token persistence. Remember Me is a pure client-side policy
 * (remember_me_mode = client_token_persistence): when off, the token
 * lives only in memory and a cold start lands back on the login screen.
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "avia_auth",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    @Volatile
    private var memoryToken: String? = null

    val token: String?
        get() = memoryToken ?: prefs.getString(KEY_TOKEN, null).also { memoryToken = it }

    fun save(token: String, persist: Boolean) {
        memoryToken = token
        if (persist) prefs.edit().putString(KEY_TOKEN, token).apply()
        else prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun clear() {
        memoryToken = null
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "bearer_token"
    }
}
