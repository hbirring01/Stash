package com.example.creditcardapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.ai.AiProvider
import com.example.creditcardapp.data.preferences.AiConfigState
import com.example.creditcardapp.data.preferences.AiSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the (non-secret) state the Settings screen exposes for AI Assist.
 * Never returns the saved API key.
 */
@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val store: AiSettingsStore,
) : ViewModel() {

    val state: StateFlow<AiConfigState> = store.state

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
}
