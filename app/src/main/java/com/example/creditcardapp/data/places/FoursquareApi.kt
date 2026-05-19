package com.example.creditcardapp.data.places

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface FoursquareApi {
    // New Places API: relative path is `places/search` (no /v3 prefix) and the
    // host is `places-api.foursquare.com`. Auth is `Bearer <token>` and the
    // `X-Places-Api-Version` header is required.
    @GET("places/search")
    suspend fun search(
        @Header("Authorization") apiKey: String,
        @Header("X-Places-Api-Version") apiVersion: String = "2025-06-17",
        @Header("Accept") accept: String = "application/json",
        @Query("ll") ll: String? = null,
        @Query("near") near: String? = null,
        @Query("radius") radiusMeters: Int? = null,
        @Query("limit") limit: Int = 50,
        @Query("sort") sort: String = "DISTANCE",
        @Query("query") query: String? = null,
        @Query("fields") fields: String = "fsq_place_id,name,categories,latitude,longitude,distance,location,website,social_media"
    ): FsqSearchResponse
}

@Serializable
data class FsqSearchResponse(
    val results: List<FsqPlace> = emptyList()
)

@Serializable
data class FsqPlace(
    // New API uses `fsq_place_id`; old API used `fsq_id`. We accept either.
    @SerialName("fsq_place_id") val fsqPlaceId: String? = null,
    @SerialName("fsq_id") val fsqId: String? = null,
    val name: String,
    val categories: List<FsqCategory> = emptyList(),
    // New API: top-level latitude/longitude. Old API: nested under `geocodes.main`.
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geocodes: FsqGeocodes? = null,
    val distance: Int? = null,
    val location: FsqLocation? = null,
    val website: String? = null,
) {
    val id: String get() = fsqPlaceId ?: fsqId ?: name
    val lat: Double? get() = latitude ?: geocodes?.main?.latitude
    val lon: Double? get() = longitude ?: geocodes?.main?.longitude
}

@Serializable
data class FsqCategory(
    // The new Foursquare Places API omits `id` on some categories (returning
    // only `name`/`short_name`), so make it optional to avoid hard failures
    // that would otherwise abort the entire `nearby` parse.
    val id: Int? = null,
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
)

@Serializable
data class FsqGeocodes(
    val main: FsqLatLon? = null,
)

@Serializable
data class FsqLatLon(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class FsqLocation(
    val address: String? = null,
    val locality: String? = null,
    val region: String? = null,
)
