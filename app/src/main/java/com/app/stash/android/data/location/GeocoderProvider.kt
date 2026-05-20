package com.app.stash.android.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Geocodes a free-form query (city, zip, full address) to a single best lat/lon. */
@Singleton
class GeocoderProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Returns the first matching [Address], or null. */
    suspend fun resolve(query: String): Address? {
        val q = query.trim()
        if (q.isEmpty()) return null
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(q, 1) { results ->
                        cont.resume(results.firstOrNull())
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(q, 1)?.firstOrNull()
                }
            }
        }.getOrNull()
    }

    /** Reverse-geocode a lat/lon into a single best [Address], or null. */
    suspend fun reverse(lat: Double, lon: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { results ->
                        cont.resume(results.firstOrNull())
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
            }
        }.getOrNull()
    }
}
