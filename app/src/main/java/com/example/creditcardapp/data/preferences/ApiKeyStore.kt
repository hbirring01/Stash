package com.example.creditcardapp.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.creditcardapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted storage for user-supplied third-party API keys (Foursquare, etc.).
 *
 * Design constraints:
 *  - The plaintext key is never returned to UI code. Settings only ever sees
 *    [hasFoursquareKey] / [foursquareKeyState] — booleans. The raw key is only
 *    handed out by [effectiveFoursquareKey], which is consumed by
 *    PlacesRepository for the network call.
 *  - Persisted via AES-256-GCM EncryptedSharedPreferences keyed off the Android
 *    Keystore, so the value is unreadable by other apps and by `adb backup`.
 *  - If the user hasn't supplied a key, falls back to the build-time
 *    BuildConfig.FOURSQUARE_API_KEY so existing installs keep working.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _foursquareKeyState = MutableStateFlow(hasUserFoursquareKey())
    /** Observable flag — true when the user has saved an override key. */
    val foursquareKeyState: StateFlow<Boolean> = _foursquareKeyState.asStateFlow()

    /** True when the user has explicitly saved a key (vs falling back to BuildConfig). */
    fun hasUserFoursquareKey(): Boolean =
        prefs.contains(KEY_FOURSQUARE) && !prefs.getString(KEY_FOURSQUARE, null).isNullOrBlank()

    /** Save a new user-supplied key. Blank input is rejected. */
    fun setFoursquareKey(rawKey: String) {
        val trimmed = rawKey.trim()
        require(trimmed.isNotEmpty()) { "API key cannot be blank" }
        prefs.edit().putString(KEY_FOURSQUARE, trimmed).apply()
        _foursquareKeyState.value = true
    }

    /** Remove the user override; falls back to the build-time key (if any). */
    fun clearFoursquareKey() {
        prefs.edit().remove(KEY_FOURSQUARE).apply()
        _foursquareKeyState.value = false
    }

    /**
     * Internal-facing: returns the key to send to Foursquare. Saved user
     * override takes precedence over the build-time key. Empty string when
     * neither is configured. Callers MUST NOT surface this to the UI.
     */
    fun effectiveFoursquareKey(): String {
        val user = prefs.getString(KEY_FOURSQUARE, null)?.trim()
        if (!user.isNullOrEmpty()) return user
        return BuildConfig.FOURSQUARE_API_KEY
    }

    companion object {
        private const val PREFS_NAME = "secure_api_keys"
        private const val KEY_FOURSQUARE = "foursquare_api_key"
    }
}
