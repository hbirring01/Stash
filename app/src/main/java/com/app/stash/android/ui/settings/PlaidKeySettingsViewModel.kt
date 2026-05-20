package com.app.stash.android.ui.settings

import androidx.lifecycle.ViewModel
import com.app.stash.android.data.plaid.PlaidCredentialsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Read-only status feed for the Plaid credentials row in Settings. The actual
 * edit/clear flow lives in [com.app.stash.android.ui.plaidsetup.PlaidSetupScreen];
 * this VM just exposes whether keys are currently saved so the row can show
 * "Set" vs "Not set" without ever reading the secret values.
 */
@HiltViewModel
class PlaidKeySettingsViewModel @Inject constructor(
    store: PlaidCredentialsStore,
) : ViewModel() {
    val hasPlaidKeys: StateFlow<Boolean> = store.hasCredentialsFlow
}
