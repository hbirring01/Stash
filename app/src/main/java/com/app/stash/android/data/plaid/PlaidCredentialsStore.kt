package com.app.stash.android.data.plaid

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the Plaid client_id + secret in EncryptedSharedPreferences. The master key
 * is generated and stored in the Android Keystore (AES256_GCM, hardware-backed on
 * devices that support it). The credentials never appear in source code, in
 * BuildConfig, in local.properties, or in any APK string table — they are entered
 * once at runtime through the in-app setup screen.
 */
@Singleton
class PlaidCredentialsStore @Inject constructor(
    private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "plaid_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasCredentials(): Boolean =
        !prefs.getString(KEY_CLIENT_ID, null).isNullOrBlank() &&
            !prefs.getString(KEY_SECRET, null).isNullOrBlank()

    private val _hasCredentialsFlow = MutableStateFlow(hasCredentials())
    /** Observable mirror of [hasCredentials] for UI status badges. Never exposes
     *  the underlying client_id or secret values. */
    val hasCredentialsFlow: StateFlow<Boolean> = _hasCredentialsFlow.asStateFlow()

    suspend fun save(clientId: String, secret: String, env: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, clientId.trim())
            .putString(KEY_SECRET, secret.trim())
            .putString(KEY_ENV, env.trim().ifBlank { "sandbox" })
            .apply()
        _hasCredentialsFlow.value = true
    }

    suspend fun clientId(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_CLIENT_ID, null)
    }

    suspend fun secret(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_SECRET, null)
    }

    suspend fun env(): String = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ENV, null)?.takeIf { it.isNotBlank() } ?: "sandbox"
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_CLIENT_ID)
            .remove(KEY_SECRET)
            .remove(KEY_ENV)
            .apply()
        _hasCredentialsFlow.value = false
    }

    private companion object {
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_SECRET = "secret"
        const val KEY_ENV = "env"
    }
}
