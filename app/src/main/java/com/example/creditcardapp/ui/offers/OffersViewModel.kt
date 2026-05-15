package com.example.creditcardapp.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.notifications.OfferGeofenceManager
import com.example.creditcardapp.data.preferences.NotificationPreferences
import com.example.creditcardapp.data.repository.OffersRepository
import com.example.creditcardapp.domain.model.Offer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val repository: OffersRepository,
    private val notificationPreferences: NotificationPreferences,
    private val geofenceManager: OfferGeofenceManager,
) : ViewModel() {

    val offers: StateFlow<List<Offer>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val backgroundOffersEnabled: StateFlow<Boolean> =
        notificationPreferences.backgroundOffersEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setActivated(id: Long, activated: Boolean) {
        viewModelScope.launch { repository.setActivated(id, activated) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    /**
     * Persist the toggle. UI must request ACCESS_BACKGROUND_LOCATION beforehand;
     * if the permission is missing the toggle is forced back off and any
     * existing geofences are cleared.
     */
    fun setBackgroundOffersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !geofenceManager.hasBackgroundLocationPermission()) {
                notificationPreferences.setBackgroundOffersEnabled(false)
                geofenceManager.clearAll()
                return@launch
            }
            notificationPreferences.setBackgroundOffersEnabled(enabled)
            if (!enabled) geofenceManager.clearAll()
        }
    }

    fun hasBackgroundLocationPermission(): Boolean =
        geofenceManager.hasBackgroundLocationPermission()
}
