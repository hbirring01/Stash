package com.example.creditcardapp.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight SharedPreferences wrapper for notification-related toggles.
 *
 * Kept separate from the encrypted [ApiKeyStore] because these values are
 * non-sensitive UI state (just booleans). No secrets ever live here.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True when the user has opted in to background offer notifications (geofence-based). */
    val backgroundOffersEnabled: Flow<Boolean> = prefs.booleanFlow(KEY_BG_OFFERS, default = false)

    fun isBackgroundOffersEnabled(): Boolean =
        prefs.getBoolean(KEY_BG_OFFERS, false)

    fun setBackgroundOffersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_OFFERS, enabled).apply()
    }

    private fun SharedPreferences.booleanFlow(key: String, default: Boolean): Flow<Boolean> =
        callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
                if (changed == key) trySend(getBoolean(key, default))
            }
            registerOnSharedPreferenceChangeListener(listener)
            awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
        }
            .onStart { emit(getBoolean(key, default)) }
            .distinctUntilChanged()

    private companion object {
        const val PREFS_NAME = "notification_prefs"
        const val KEY_BG_OFFERS = "bg_offers_enabled"
    }
}
