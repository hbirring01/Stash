package com.example.creditcardapp.ui.plaidsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.plaid.PlaidCredentialsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaidSetupUiState(
    val hasCredentials: Boolean = false,
    val env: String = "sandbox",
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PlaidSetupViewModel @Inject constructor(
    private val store: PlaidCredentialsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaidSetupUiState(hasCredentials = store.hasCredentials()))
    val state: StateFlow<PlaidSetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(env = store.env()) }
        }
    }

    fun save(clientId: String, secret: String, env: String, onDone: () -> Unit) {
        val id = clientId.trim()
        val sec = secret.trim()
        if (id.isBlank() || sec.isBlank()) {
            _state.update { it.copy(error = "Both fields are required.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            runCatching { store.save(id, sec, env) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            saving = false,
                            hasCredentials = true,
                            justSaved = true,
                            env = env,
                        )
                    }
                    onDone()
                }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = e.message ?: "Save failed.") }
                }
        }
    }

    fun clear() {
        viewModelScope.launch {
            runCatching { store.clear() }
            _state.update {
                PlaidSetupUiState(hasCredentials = false, env = "sandbox")
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
