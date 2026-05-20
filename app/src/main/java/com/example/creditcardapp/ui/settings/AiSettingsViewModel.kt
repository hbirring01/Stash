package com.example.creditcardapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.ai.AiProvider
import com.example.creditcardapp.data.local.AiMatchCacheDao
import com.example.creditcardapp.data.preferences.AiConfigState
import com.example.creditcardapp.data.preferences.AiSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the (non-secret) state the Settings screen exposes for AI Assist.
 * Never returns the saved API key.
 */
@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val store: AiSettingsStore,
    private val cacheDao: AiMatchCacheDao,
) : ViewModel() {

    val state: StateFlow<AiConfigState> = store.state

    /** Number of cached LLM verdicts currently stored. Updates live. */
    val cacheCount: StateFlow<Int> = cacheDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun save(
        provider: AiProvider,
        apiKey: String,
        baseUrlOverride: String?,
        modelOverride: String?,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val ok = runCatching {
                store.saveConfig(provider, apiKey, baseUrlOverride, modelOverride)
            }.isSuccess
            onResult(ok)
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setEnabled(enabled) }
    }

    fun clear() {
        viewModelScope.launch { store.clear() }
    }

    /**
     * Wipe every cached LLM verdict. The next sync will re-ask the model for
     * each ambiguous merchant (within the per-batch budget). Useful when the
     * user has changed providers or believes a cached "NO" was wrong.
     */
    fun clearCache() {
        viewModelScope.launch { cacheDao.clearAll() }
    }
}

