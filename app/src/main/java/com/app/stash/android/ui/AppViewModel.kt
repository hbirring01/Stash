package com.app.stash.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stash.android.data.preferences.ThemeMode
import com.app.stash.android.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity-scoped view-model that surfaces app-wide UI preferences (just theme
 * mode for now) and lets settings UI mutate them.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }
}
