package com.example.creditcardapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.preferences.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the (very limited) state the Settings screen exposes for the user's
 * Foursquare API key: only "is one saved?" — never the value itself.
 */
@HiltViewModel
class ApiKeySettingsViewModel @Inject constructor(
    private val store: ApiKeyStore,
) : ViewModel() {

    /** True when the user has saved an override key. UI uses this to flip the
     *  card label between "Set" and "Not set" without ever reading the key. */
    val hasFoursquareKey: StateFlow<Boolean> = store.foursquareKeyState

    fun saveFoursquareKey(raw: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { store.setFoursquareKey(raw) }.isSuccess
            onResult(ok)
        }
    }

    fun clearFoursquareKey() {
        viewModelScope.launch { store.clearFoursquareKey() }
    }
}
