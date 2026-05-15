package com.example.creditcardapp.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.creditcardapp.data.repository.OffersRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives geofence ENTER transitions registered by [OfferGeofenceManager] and
 * fires the same notification the foreground flow uses. Runs even when the app
 * is in the background or fully killed.
 */
@AndroidEntryPoint
class OfferGeofenceReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: OfferNotifier
    @Inject lateinit var offersRepository: OffersRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != OfferGeofenceManager.ACTION_GEOFENCE_EVENT) return
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Geofence event error code=${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val triggers = event.triggeringGeofences ?: return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val offers = runCatching { offersRepository.observeUnactivated().first() }
                    .getOrDefault(emptyList())
                triggers.forEach { fence ->
                    val parsed = OfferGeofenceManager.parseRequestId(fence.requestId) ?: return@forEach
                    val (offerId, placeName) = parsed
                    val offer = offers.firstOrNull { it.id == offerId } ?: return@forEach
                    notifier.notifyOfferNearby(offer, placeName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "OfferGeofenceRecv"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
