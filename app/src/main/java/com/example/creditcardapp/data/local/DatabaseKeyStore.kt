package com.example.creditcardapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and stores the 256-bit passphrase used to encrypt the Room database
 * via SQLCipher. The passphrase itself lives in EncryptedSharedPreferences, whose
 * master key is generated and held in the Android Keystore (AES-256-GCM, hardware-
 * backed when supported). The passphrase is never written to logs, BuildConfig, or
 * the APK — it's generated on first launch and never re-derived.
 */
@Singleton
class DatabaseKeyStore @Inject constructor(
    private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "db_key_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasKey(): Boolean = !prefs.getString(KEY_PASSPHRASE, null).isNullOrBlank()

    /** Returns the raw passphrase bytes, generating one the first time it's needed. */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (!existing.isNullOrBlank()) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(fresh, Base64.NO_WRAP))
            .apply()
        return fresh
    }

    private companion object {
        const val KEY_PASSPHRASE = "db_passphrase"
    }
}
