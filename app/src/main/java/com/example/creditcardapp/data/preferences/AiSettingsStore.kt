package com.example.creditcardapp.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.creditcardapp.data.ai.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted storage for AI Assist (LLM provider) configuration.
 *
 * Mirrors [ApiKeyStore]'s design: the plaintext API key is never returned to UI
 * code — Settings only sees the public state ([AiConfigState]). The key is only
 * handed out by [effectiveApiKey] to the [AiMatchClient] for the network call.
 *
 * The provider preset, optional base-URL override, and optional model override
 * are NOT secret and live in the same encrypted store purely for convenience
 * (one file, one read).
 */
@Singleton
class AiSettingsStore @Inject constructor(
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

    private val _state = MutableStateFlow(readState())
    /** Observable, non-secret config state for the Settings UI. */
    val state: StateFlow<AiConfigState> = _state.asStateFlow()

    /** True iff a usable key is saved AND AI is enabled. */
    fun isEnabled(): Boolean = _state.value.let { it.enabled && it.hasKey }

    /** Toggle AI Assist on/off. Has no effect if no key is saved. */
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = readState()
    }

    /** Save provider preset + key in one transaction. Blank key is rejected. */
    fun saveConfig(
        provider: AiProvider,
        apiKey: String,
        baseUrlOverride: String? = null,
        modelOverride: String? = null,
    ) {
        val key = apiKey.trim()
        require(key.isNotEmpty()) { "API key cannot be blank" }
        prefs.edit()
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_API_KEY, key)
            .putString(KEY_BASE_URL, baseUrlOverride?.trim()?.ifEmpty { null })
            .putString(KEY_MODEL, modelOverride?.trim()?.ifEmpty { null })
            .putBoolean(KEY_ENABLED, true)
            .apply()
        _state.value = readState()
    }

    /** Remove the saved key and disable AI. Other prefs (provider) are kept. */
    fun clear() {
        prefs.edit()
            .remove(KEY_API_KEY)
            .putBoolean(KEY_ENABLED, false)
            .apply()
        _state.value = readState()
    }

    /** Internal-facing — the key to send as Bearer. Callers MUST NOT surface this. */
    fun effectiveApiKey(): String = prefs.getString(KEY_API_KEY, null)?.trim().orEmpty()

    /** Resolved base URL: user override > provider default. */
    fun effectiveBaseUrl(): String {
        val override = prefs.getString(KEY_BASE_URL, null)?.trim().orEmpty()
        if (override.isNotEmpty()) return override.ensureTrailingSlash()
        return currentProvider().defaultBaseUrl.ensureTrailingSlash()
    }

    /** Resolved model: user override > provider default. */
    fun effectiveModel(): String {
        val override = prefs.getString(KEY_MODEL, null)?.trim().orEmpty()
        if (override.isNotEmpty()) return override
        return currentProvider().defaultModel
    }

    fun currentProvider(): AiProvider =
        runCatching { AiProvider.valueOf(prefs.getString(KEY_PROVIDER, AiProvider.GEMINI.name)!!) }
            .getOrDefault(AiProvider.GEMINI)

    private fun readState(): AiConfigState {
        val provider = currentProvider()
        val hasKey = !prefs.getString(KEY_API_KEY, null).isNullOrBlank()
        val enabled = prefs.getBoolean(KEY_ENABLED, false) && hasKey
        return AiConfigState(
            provider = provider,
            hasKey = hasKey,
            enabled = enabled,
            baseUrlOverride = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() },
            modelOverride = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() },
        )
    }

    companion object {
        private const val PREFS_NAME = "secure_ai_settings"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_ENABLED = "ai_enabled"
    }
}

/** Non-secret view of the AI Assist configuration. Safe to expose to UI. */
data class AiConfigState(
    val provider: AiProvider,
    val hasKey: Boolean,
    val enabled: Boolean,
    val baseUrlOverride: String?,
    val modelOverride: String?,
)

private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"
