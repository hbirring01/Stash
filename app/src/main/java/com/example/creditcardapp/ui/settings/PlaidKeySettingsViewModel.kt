package com.example.creditcardapp.ui.settings

import androidx.lifecycle.ViewModel
import com.example.creditcardapp.data.plaid.PlaidCredentialsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Read-only status feed for the Plaid credentials row in Settings. The actual
 * edit/clear flow lives in [com.example.creditcardapp.ui.plaidsetup.PlaidSetupScreen];
 * this VM just exposes whether keys are currently saved so the row can show
 * "Set" vs "Not set" without ever reading the secret values.
 */
@HiltViewModel
class PlaidKeySettingsViewModel @Inject constructor(
    store: PlaidCredentialsStore,
) : ViewModel() {
    val hasPlaidKeys: StateFlow<Boolean> = store.hasCredentialsFlow
}
