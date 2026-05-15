package com.example.creditcardapp.data.places

import com.example.creditcardapp.domain.model.RewardCategory
import kotlinx.serialization.Serializable

@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

@Serializable
data class OverpassElement(
    val id: Long,
    val type: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap()
) {
    val pointLat: Double? get() = lat ?: center?.lat
    val pointLon: Double? get() = lon ?: center?.lon
}

@Serializable
data class OverpassCenter(val lat: Double, val lon: Double)

/** A nearby business with a best-guess spending category. */
data class NearbyPlace(
    val id: Long,
    val name: String,
    val category: RewardCategory,
    val subtype: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double
)
