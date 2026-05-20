package com.app.stash.android.data.plaid

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaidTokenStore @Inject constructor(
    private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "plaid_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun save(accessToken: String, itemId: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_ITEM_ID, itemId)
            .apply()
    }

    suspend fun accessToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    suspend fun itemId(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ITEM_ID, null)
    }

    suspend fun saveInstitution(itemId: String, institutionId: String?, logoBase64: String?) =
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putString(institutionIdKey(itemId), institutionId)
                .putString(institutionLogoKey(itemId), logoBase64)
                .apply()
        }

    suspend fun institutionLogo(itemId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(institutionLogoKey(itemId), null)
    }

    suspend fun institutionId(itemId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(institutionIdKey(itemId), null)
    }

    suspend fun transactionsCursor(itemId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(cursorKey(itemId), null)
    }

    suspend fun saveTransactionsCursor(itemId: String, cursor: String?) = withContext(Dispatchers.IO) {
        prefs.edit().putString(cursorKey(itemId), cursor).apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun institutionIdKey(itemId: String) = "institution_id_$itemId"
    private fun institutionLogoKey(itemId: String) = "institution_logo_$itemId"
    private fun cursorKey(itemId: String) = "transactions_cursor_$itemId"

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_ITEM_ID = "item_id"
    }
}
