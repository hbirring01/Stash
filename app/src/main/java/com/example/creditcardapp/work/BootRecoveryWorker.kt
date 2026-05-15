package com.example.creditcardapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.creditcardapp.data.location.LocationProvider
import com.example.creditcardapp.data.notifications.OfferGeofenceManager
import com.example.creditcardapp.data.places.PlacesRepository
import com.example.creditcardapp.data.preferences.NotificationPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Re-installs offer geofences after device reboot or app upgrade. Both events
 * clear the system geofence list; without this worker the user would not get
 * offer notifications again until they next open the map screen.
 *
 * No-op unless:
 *  - the user opted in to background offer notifications, and
 *  - foreground + background location permission are still granted, and
 *  - we can resolve a recent device location.
 */
@HiltWorker
class BootRecoveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferences: NotificationPreferences,
    private val geofenceManager: OfferGeofenceManager,
    private val locationProvider: LocationProvider,
    private val placesRepository: PlacesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!preferences.isBackgroundOffersEnabled()) return Result.success()
        if (!geofenceManager.hasBackgroundLocationPermission()) return Result.success()
        if (!locationProvider.hasPermission()) return Result.success()

        val location = runCatching { locationProvider.current() }.getOrNull()
            ?: return Result.retry()
        val nearby = placesRepository.nearby(location.latitude, location.longitude)
            .getOrNull()
            ?: return Result.retry()

        runCatching { geofenceManager.refresh(nearby) }
            .onFailure { return Result.retry() }
        return Result.success()
    }

    companion object {
        const val NAME = "offer-geofence-boot-recovery"
    }
}
