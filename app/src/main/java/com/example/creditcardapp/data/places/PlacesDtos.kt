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
    val distanceMeters: Double,
    /** Website URL (from OSM `website`/`contact:website` or Foursquare `website`). */
    val website: String? = null,
) {
    /**
     * Bare domain extracted from [website], suitable for Clearbit Logo:
     *   `https://www.starbucks.com/menu` → `starbucks.com`
     * Returns null when no website is set or parsing fails.
     */
    val logoDomain: String?
        get() = extractDomain(website)
}

private fun extractDomain(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return runCatching {
        val withScheme = if (url.contains("://")) url else "http://$url"
        val host = java.net.URI(withScheme).host ?: return null
        host.removePrefix("www.").lowercase().takeIf { it.contains('.') }
    }.getOrNull()
}
