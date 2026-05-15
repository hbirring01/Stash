package com.example.creditcardapp.data.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.creditcardapp.data.places.NearbyPlace
import com.example.creditcardapp.data.repository.OffersRepository
import com.example.creditcardapp.domain.model.Offer
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Registers / refreshes geofences for nearby merchants that have active,
 * unactivated card-linked offers.
 *
 * Strategy:
 * - Called from [com.example.creditcardapp.ui.rewards.RewardsMapViewModel] each
 *   time the map screen refreshes the nearby-places list. We collect every
 *   place whose name matches an active offer's pattern and install a 150m
 *   geofence with the offer encoded in the request ID.
 * - System fires [OfferGeofenceReceiver] when the user re-enters the area
 *   even with the app closed. Receiver looks up the offer + notifies.
 *
 * Limits:
 * - Android caps geofences per app at 100; we cap ourselves at 50.
 * - Geofences are wiped on reboot. They get re-installed on next map refresh.
 * - Requires both ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION (Q+).
 */
@Singleton
class OfferGeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offersRepository: OffersRepository,
) {
    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, OfferGeofenceReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    fun hasBackgroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Replace the current geofence set with new ones derived from [places].
     * Only places with an active unactivated offer match are installed.
     */
    @SuppressLint("MissingPermission") // guarded by hasBackgroundLocationPermission()
    suspend fun refresh(places: List<NearbyPlace>) {
        if (!hasBackgroundLocationPermission()) return
        val offers = runCatching { offersRepository.observeUnactivated().first() }
            .getOrDefault(emptyList())
        if (offers.isEmpty() || places.isEmpty()) {
            clearAll()
            return
        }

        val now = System.currentTimeMillis()
        val matches: List<Pair<NearbyPlace, Offer>> = places.mapNotNull { place ->
            val offer = offers.firstOrNull { it.isActive(now) && it.matchesMerchant(place.name) }
                ?: return@mapNotNull null
            place to offer
        }
            .distinctBy { it.first.id }
            .take(MAX_GEOFENCES)

        if (matches.isEmpty()) {
            clearAll()
            return
        }

        val geofences = matches.map { (place, offer) ->
            Geofence.Builder()
                .setRequestId(buildRequestId(offer.id, place.name))
                .setCircularRegion(place.latitude, place.longitude, RADIUS_METERS)
                .setExpirationDuration(EXPIRATION_MS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0) // don't fire on registration
            .addGeofences(geofences)
            .build()

        // Replace existing — we always re-install the full current set.
        clearAll()
        runCatching {
            suspendCancellableCoroutine<Unit> { cont ->
                client.addGeofences(request, pendingIntent)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }
    }

    suspend fun clearAll() {
        runCatching {
            suspendCancellableCoroutine<Unit> { cont ->
                client.removeGeofences(pendingIntent)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT =
            "com.example.creditcardapp.OFFER_GEOFENCE_EVENT"

        private const val REQUEST_CODE = 8400
        private const val MAX_GEOFENCES = 50
        private const val RADIUS_METERS = 150f
        private const val EXPIRATION_MS = 24L * 60 * 60 * 1000 // 24 h
        private const val NOTIFICATION_RESPONSIVENESS_MS = 60_000 // 1 min

        private const val ID_PREFIX = "offer"
        private const val ID_SEP = "|"

        fun buildRequestId(offerId: Long, placeName: String): String {
            val safe = placeName.replace(ID_SEP, " ").take(64)
            return "$ID_PREFIX$ID_SEP$offerId$ID_SEP$safe"
        }

        /** Returns (offerId, placeName) or null if not one of ours. */
        fun parseRequestId(requestId: String): Pair<Long, String>? {
            val parts = requestId.split(ID_SEP)
            if (parts.size < 3 || parts[0] != ID_PREFIX) return null
            val id = parts[1].toLongOrNull() ?: return null
            val name = parts.drop(2).joinToString(ID_SEP)
            return id to name
        }
    }
}
