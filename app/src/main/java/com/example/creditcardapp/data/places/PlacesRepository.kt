package com.example.creditcardapp.data.places

import com.example.creditcardapp.BuildConfig
import com.example.creditcardapp.domain.model.RewardCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class PlacesRepository @Inject constructor(
    private val api: OverpassApi,
    private val foursquare: FoursquareApi,
) {

    /**
     * Fetches named businesses around (lat, lon). Tries Overpass (OSM) first with
     * an auto-expanding radius; falls back to Foursquare Places when Overpass
     * returns empty or errors (Foursquare requires FOURSQUARE_API_KEY in
     * local.properties).
     */
    suspend fun nearby(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 1609,
    ): Result<List<NearbyPlace>> = withContext(Dispatchers.IO) {
        // Imperial-friendly cascade: 1 mi → 3 mi → 10 mi when the closer search
        // returns nothing (useful in suburbs / rural areas). Honor the caller's
        // requested radius as the starting point.
        val radii = listOf(radiusMeters, 4828, 16093)
            .filter { it >= radiusMeters }
            .distinct()
        var overpassError: Throwable? = null
        for (r in radii) {
            val result = runCatching { fetchOverpass(lat, lon, r) }
            result.onFailure { overpassError = it }
            val list = result.getOrNull().orEmpty()
            if (list.isNotEmpty()) return@withContext Result.success(list)
        }

        // Fallback: Foursquare. Only attempt when a key is configured.
        val key = BuildConfig.FOURSQUARE_API_KEY
        if (key.isNotBlank()) {
            for (r in radii) {
                val result = runCatching { fetchFoursquare(lat, lon, r, key) }
                val list = result.getOrNull().orEmpty()
                if (list.isNotEmpty()) return@withContext Result.success(list)
                result.onFailure { overpassError = it }
            }
        }

        if (overpassError != null) Result.failure(overpassError!!) else Result.success(emptyList())
    }

    private suspend fun fetchOverpass(lat: Double, lon: Double, radiusMeters: Int): List<NearbyPlace> {
        // `nwr` covers node/way/relation. `out center` gives ways/relations a representative
        // lat/lon. Many real businesses are tagged as building ways, not nodes.
        val query = """
            [out:json][timeout:25];
            (
              nwr["amenity"~"restaurant|cafe|fast_food|bar|pub|fuel|cinema|theatre|nightclub|bank|atm|pharmacy"]["name"](around:$radiusMeters,$lat,$lon);
              nwr["shop"]["name"](around:$radiusMeters,$lat,$lon);
              nwr["tourism"~"hotel|motel|hostel|attraction|museum"]["name"](around:$radiusMeters,$lat,$lon);
            );
            out center 60;
        """.trimIndent()
        val response = api.query(query)
        return response.elements.mapNotNull { el ->
            val pLat = el.pointLat ?: return@mapNotNull null
            val pLon = el.pointLon ?: return@mapNotNull null
            val name = el.tags["name"] ?: return@mapNotNull null
            val (category, subtype) = classifyOsm(el.tags)
            NearbyPlace(
                id = el.id,
                name = name,
                category = category,
                subtype = subtype,
                latitude = pLat,
                longitude = pLon,
                distanceMeters = haversine(lat, lon, pLat, pLon),
                website = el.tags["website"]
                    ?: el.tags["contact:website"]
                    ?: el.tags["brand:website"],
            )
        }.sortedBy { it.distanceMeters }
    }

    private suspend fun fetchFoursquare(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        apiKey: String,
        nameQuery: String? = null,
    ): List<NearbyPlace> {
        val response = foursquare.search(
            apiKey = "Bearer $apiKey",
            ll = "$lat,$lon",
            radiusMeters = radiusMeters,
            query = nameQuery?.takeIf { it.isNotBlank() },
        )
        return response.results.mapNotNull { p ->
            val pLat = p.lat ?: return@mapNotNull null
            val pLon = p.lon ?: return@mapNotNull null
            val (category, subtype) = classifyFoursquare(p.categories)
            // Foursquare ids are strings; hash to a stable Long for our domain model.
            NearbyPlace(
                id = p.id.hashCode().toLong(),
                name = p.name,
                category = category,
                subtype = subtype,
                latitude = pLat,
                longitude = pLon,
                distanceMeters = p.distance?.toDouble() ?: haversine(lat, lon, pLat, pLon),
                website = p.website,
            )
        }.sortedBy { it.distanceMeters }
    }

    /**
     * Search for businesses whose name matches [nameQuery] near (lat, lon).
     * Used when the user types in the business search field. Uses a wider
     * radius than the geographic browse since the user has named a specific
     * place they want to find.
     */
    suspend fun searchByName(
        lat: Double,
        lon: Double,
        nameQuery: String,
        radiusMeters: Int = 48_280, // ~30 mi
    ): Result<List<NearbyPlace>> = withContext(Dispatchers.IO) {
        val query = nameQuery.trim()
        if (query.isEmpty()) return@withContext Result.success(emptyList())

        // Cascade: start narrow, widen until we find something. This both speeds
        // up dense urban searches and keeps the result genuinely "nearest".
        val radii = listOf(4828, 16093, radiusMeters.coerceAtLeast(16093)).distinct()
        var lastError: Throwable? = null

        // Prefer Foursquare when configured \u2014 its search is name-aware and brand-aware.
        val key = BuildConfig.FOURSQUARE_API_KEY
        if (key.isNotBlank()) {
            for (r in radii) {
                val fsq = runCatching { fetchFoursquare(lat, lon, r, key, query) }
                fsq.onFailure { lastError = it }
                val list = fsq.getOrNull().orEmpty()
                if (list.isNotEmpty()) return@withContext Result.success(list)
            }
        }

        // Overpass fallback: search name/brand/operator tags case-insensitively.
        for (r in radii) {
            val osm = runCatching { fetchOverpassByName(lat, lon, r, query) }
            osm.onFailure { lastError = it }
            val list = osm.getOrNull().orEmpty()
            if (list.isNotEmpty()) return@withContext Result.success(list)
        }

        if (lastError != null) Result.failure(lastError!!) else Result.success(emptyList())
    }

    /**
     * Global business search that is NOT tied to the user's location. Uses the
     * Foursquare `near=` parameter so the location is interpreted server-side as
     * a free-text place name (city, neighborhood, address, etc.). Requires
     * FOURSQUARE_API_KEY in local.properties.
     */
    suspend fun searchAnywhere(
        businessQuery: String,
        nearQuery: String,
    ): Result<List<NearbyPlace>> = withContext(Dispatchers.IO) {
        val q = businessQuery.trim()
        val near = nearQuery.trim()
        if (q.isEmpty() || near.isEmpty()) return@withContext Result.success(emptyList())

        val key = BuildConfig.FOURSQUARE_API_KEY
        if (key.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Add FOURSQUARE_API_KEY to local.properties to enable global search.")
            )
        }
        runCatching {
            foursquare.search(
                apiKey = "Bearer $key",
                query = q,
                near = near,
                // No `ll` or `radius` — Foursquare resolves `near` globally.
            )
        }.fold(
            onSuccess = { resp ->
                val places = resp.results.mapNotNull { p ->
                    val pLat = p.lat ?: return@mapNotNull null
                    val pLon = p.lon ?: return@mapNotNull null
                    val (category, subtype) = classifyFoursquare(p.categories)
                    NearbyPlace(
                        id = p.id.hashCode().toLong(),
                        name = p.name,
                        category = category,
                        subtype = subtype,
                        latitude = pLat,
                        longitude = pLon,
                        // No anchor — Foursquare's `distance` is relative to `near`.
                        distanceMeters = p.distance?.toDouble() ?: 0.0,
                        website = p.website,
                    )
                }
                Result.success(places)
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun fetchOverpassByName(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        nameQuery: String,
    ): List<NearbyPlace> {
        // Escape regex specials so user input like "M&M's" doesn't break the query.
        val escaped = nameQuery.replace(Regex("""([\\.()\[\]{}+*?^$|/"])"""), "\\\\$1")
        // Many chains in OSM are tagged via `brand` or `operator`, not just `name`.
        // Querying all three (union) catches Starbucks, McDonald's, etc. that may
        // only have a generic `name` like "Coffee" but a `brand=Starbucks`.
        val query = """
            [out:json][timeout:25];
            (
              nwr["name"~"$escaped",i](around:$radiusMeters,$lat,$lon);
              nwr["brand"~"$escaped",i](around:$radiusMeters,$lat,$lon);
              nwr["operator"~"$escaped",i](around:$radiusMeters,$lat,$lon);
            );
            out center 80;
        """.trimIndent()
        val response = api.query(query)
        return response.elements.mapNotNull { el ->
            val pLat = el.pointLat ?: return@mapNotNull null
            val pLon = el.pointLon ?: return@mapNotNull null
            // Prefer the display name; fall back to brand/operator if name is missing.
            val name = el.tags["name"]
                ?: el.tags["brand"]
                ?: el.tags["operator"]
                ?: return@mapNotNull null
            val (category, subtype) = classifyOsm(el.tags)
            NearbyPlace(
                id = el.id,
                name = name,
                category = category,
                subtype = subtype,
                latitude = pLat,
                longitude = pLon,
                distanceMeters = haversine(lat, lon, pLat, pLon),
                website = el.tags["website"]
                    ?: el.tags["contact:website"]
                    ?: el.tags["brand:website"],
            )
        }
            // Overpass can return the same business as both a node and the parent way.
            // Dedupe by (name, rounded lat/lon) to avoid duplicates in the list.
            .distinctBy { Triple(it.name.lowercase(), (it.latitude * 1e4).toInt(), (it.longitude * 1e4).toInt()) }
            .sortedBy { it.distanceMeters }
    }

    private fun classifyOsm(tags: Map<String, String>): Pair<RewardCategory, String> {
        val amenity = tags["amenity"]
        val shop = tags["shop"]
        val tourism = tags["tourism"]

        return when {
            amenity in DINING_AMENITIES -> RewardCategory.DINING to (amenity ?: "restaurant")
            amenity == "fuel" -> RewardCategory.GAS to "gas station"
            amenity in ENTERTAINMENT_AMENITIES -> RewardCategory.ENTERTAINMENT to (amenity ?: "")
            tourism in TRAVEL_TOURISM -> RewardCategory.TRAVEL to (tourism ?: "lodging")
            shop in GROCERY_SHOPS -> RewardCategory.GROCERIES to (shop ?: "grocery")
            shop != null -> RewardCategory.SHOPPING to shop
            else -> RewardCategory.OTHER to (amenity ?: tourism ?: "place")
        }
    }

    private fun classifyFoursquare(categories: List<FsqCategory>): Pair<RewardCategory, String> {
        val names = categories.map { it.name.lowercase() }
        val subtype = categories.firstOrNull()?.name?.lowercase() ?: "place"
        val category = when {
            names.any { it.contains("restaurant") || it.contains("cafe") || it.contains("coffee")
                || it.contains("diner") || it.contains("food") || it.contains("bar")
                || it.contains("pub") || it.contains("pizza") || it.contains("bakery")
                || it.contains("steakhouse") } -> RewardCategory.DINING
            names.any { it.contains("gas station") || it.contains("fuel") } -> RewardCategory.GAS
            names.any { it.contains("grocery") || it.contains("supermarket") } -> RewardCategory.GROCERIES
            names.any { it.contains("hotel") || it.contains("motel") || it.contains("hostel")
                || it.contains("resort") || it.contains("airport") } -> RewardCategory.TRAVEL
            names.any { it.contains("movie") || it.contains("theater") || it.contains("theatre")
                || it.contains("nightclub") || it.contains("concert") } -> RewardCategory.ENTERTAINMENT
            names.any { it.contains("shop") || it.contains("store") || it.contains("retail")
                || it.contains("market") || it.contains("mall") } -> RewardCategory.SHOPPING
            else -> RewardCategory.OTHER
        }
        return category to subtype
    }

    private companion object {
        val DINING_AMENITIES = setOf("restaurant", "cafe", "fast_food", "bar", "pub")
        val ENTERTAINMENT_AMENITIES = setOf("cinema", "theatre", "nightclub")
        val TRAVEL_TOURISM = setOf("hotel", "motel", "hostel")
        val GROCERY_SHOPS = setOf(
            "supermarket", "convenience", "greengrocer", "butcher",
            "bakery", "deli", "farm"
        )
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2).let { it * it }
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
