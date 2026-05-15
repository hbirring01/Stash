package com.example.creditcardapp.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.location.GeocoderProvider
import com.example.creditcardapp.data.location.LocationProvider
import com.example.creditcardapp.data.places.NearbyPlace
import com.example.creditcardapp.data.places.PlacesRepository
import com.example.creditcardapp.data.repository.CreditCardRepository
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.domain.model.RewardCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null,
    val isManual: Boolean = false,
)

/** A single card option for a given place, with the per-$1 multiplier. */
data class CardOption(
    val card: CreditCard,
    val multiplier: Double,
)

data class PlaceRecommendation(
    val place: NearbyPlace,
    val bestCard: CreditCard?,
    val multiplier: Double,
    /** All known cards sorted by multiplier for this place, best first. */
    val allOptions: List<CardOption> = emptyList(),
)

enum class PlacesSort { Distance, Multiplier }

data class RewardsMapUiState(
    val location: UserLocation? = null,
    val places: List<PlaceRecommendation> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val locationDenied: Boolean = false,
    val automaticFailed: Boolean = false,
    /** When non-null, only places of this category are shown. */
    val categoryFilter: RewardCategory? = null,
    val sort: PlacesSort = PlacesSort.Distance,
    /** Place id of the currently highlighted business (from marker or list tap). */
    val selectedPlaceId: Long? = null,
    /** Free-text filter applied to business names (client-side substring match). */
    val nameFilter: String = "",
    /** True when the current [places] came from a name search (vs a geographic browse). */
    val nameSearchMode: Boolean = false,
    /** Search radius in meters used for the most recent successful fetch. */
    val radiusMeters: Int = DEFAULT_RADIUS_METERS,
) {
    companion object {
        // ~1 mi default. Imperial-friendly meters used throughout the API layer.
        const val DEFAULT_RADIUS_METERS = 1609
    }

    val filteredPlaces: List<PlaceRecommendation>
        get() {
            val byCategory = if (categoryFilter == null) places
            else places.filter { it.place.category == categoryFilter }
            // In name-search mode, the network already returned matching businesses;
            // skip the client-side substring filter to avoid double-filtering.
            val q = nameFilter.trim()
            val byName = if (q.isEmpty() || nameSearchMode) byCategory
            else byCategory.filter { it.place.name.contains(q, ignoreCase = true) }
            return when (sort) {
                PlacesSort.Distance -> byName.sortedBy { it.place.distanceMeters }
                PlacesSort.Multiplier -> byName.sortedByDescending { it.multiplier }
            }
        }

    val availableCategories: List<RewardCategory>
        get() = places.map { it.place.category }.distinct()
}

@HiltViewModel
class RewardsMapViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val geocoderProvider: GeocoderProvider,
    private val placesRepository: PlacesRepository,
    private val cardRepository: CreditCardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RewardsMapUiState())
    val state: StateFlow<RewardsMapUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            if (!locationProvider.hasPermission()) {
                _state.value = _state.value.copy(
                    loading = false,
                    locationDenied = true,
                    automaticFailed = true,
                )
                return@launch
            }
            val loc = locationProvider.current()
            if (loc == null) {
                _state.value = _state.value.copy(
                    loading = false,
                    automaticFailed = true,
                    error = "Couldn't read your location. Enter a city or ZIP below."
                )
                return@launch
            }
            applyLocation(
                UserLocation(loc.latitude, loc.longitude, label = null, isManual = false)
            )
        }
    }

    fun refresh() {
        val loc = _state.value.location ?: return load()
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true, error = null)
            applyLocation(loc, isRefresh = true)
        }
    }

    fun searchManualLocation(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val address = geocoderProvider.resolve(query)
            if (address == null) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Couldn't find \"$query\". Try a city, ZIP, or full address."
                )
                return@launch
            }
            val label = listOfNotNull(
                address.locality,
                address.adminArea,
                address.postalCode,
                address.countryCode,
            ).take(2).joinToString(", ").ifBlank { query }
            applyLocation(
                UserLocation(address.latitude, address.longitude, label = label, isManual = true)
            )
        }
    }

    /** Use a point picked directly on the map (long-press). Reverse-geocodes for a nice label. */
    fun useMapPoint(lat: Double, lon: Double) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val address = geocoderProvider.reverse(lat, lon)
            val label = address?.let { a ->
                listOfNotNull(a.locality, a.adminArea, a.postalCode, a.countryCode)
                    .take(2).joinToString(", ")
                    .ifBlank { "%.4f, %.4f".format(lat, lon) }
            } ?: "%.4f, %.4f".format(lat, lon)
            applyLocation(UserLocation(lat, lon, label = label, isManual = true))
        }
    }

    /**
     * Re-search at the given coordinates without reverse-geocoding. Used by the
     * "Search this area" button after panning the map — the user already sees
     * where they are, so we don't need to hit the geocoder.
     */
    fun searchHere(lat: Double, lon: Double) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            applyLocation(
                UserLocation(lat, lon, label = "Map area", isManual = true)
            )
        }
    }

    fun selectCategory(category: RewardCategory?) {
        _state.value = _state.value.copy(categoryFilter = category)
    }

    fun setNameFilter(query: String) {
        _state.value = _state.value.copy(nameFilter = query)
    }

    /**
     * Run a network-side search for businesses matching [query] near the current
     * location. Pulls from Foursquare (when configured) and falls back to Overpass.
     * Replaces the visible list with the results until the user clears the search.
     */
    fun searchBusinessByName(query: String) {
        val q = query.trim()
        val loc = _state.value.location ?: return
        if (q.isEmpty()) {
            // Restore the geographic browse.
            _state.value = _state.value.copy(nameSearchMode = false, nameFilter = "")
            viewModelScope.launch { applyLocation(loc) }
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, nameFilter = q)
            val cards = runCatching { cardRepository.observeCards().first() }
                .getOrDefault(emptyList())
            val result = placesRepository.searchByName(loc.latitude, loc.longitude, q)
            val places = result.getOrDefault(emptyList())
            val recs = places.map { p -> recommend(p, cards) }
            val failure = result.exceptionOrNull()
            _state.value = _state.value.copy(
                places = recs,
                loading = false,
                nameSearchMode = true,
                selectedPlaceId = null,
                error = when {
                    failure != null -> "Search failed: ${failure.message ?: "network error"}"
                    places.isEmpty() -> "No matches found for \"$q\" within ~30 mi."
                    else -> null
                }
            )
        }
    }

    /**
     * Global search: find a business by name in any city. Not tied to the user's
     * current location. Expects the user-typed query in the form
     * "{business} in {place}" / "{business} near {place}" / "{business} @ {place}".
     * Returns results centered on the resolved location.
     */
    fun searchAnywhere(rawQuery: String) {
        val raw = rawQuery.trim()
        if (raw.isEmpty()) return
        val (business, near) = parseAnywhereQuery(raw)
        if (business.isEmpty() || near.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Try \"Starbucks in Chicago\" or \"Best Buy near 90210\"."
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val cards = runCatching { cardRepository.observeCards().first() }
                .getOrDefault(emptyList())
            val result = placesRepository.searchAnywhere(business, near)
            val places = result.getOrDefault(emptyList())
            val recs = places.map { p -> recommend(p, cards) }
            val failure = result.exceptionOrNull()

            // Center the map on the centroid of the returned results so the user
            // sees what they searched for (even though the search itself has no
            // device-location tether).
            val anchor = if (places.isNotEmpty()) {
                val avgLat = places.sumOf { it.latitude } / places.size
                val avgLon = places.sumOf { it.longitude } / places.size
                UserLocation(avgLat, avgLon, label = "$business in $near", isManual = true)
            } else _state.value.location

            _state.value = _state.value.copy(
                location = anchor,
                places = recs,
                loading = false,
                nameSearchMode = true,
                nameFilter = business,
                selectedPlaceId = null,
                error = when {
                    failure != null -> "Search failed: ${failure.message ?: "network error"}"
                    places.isEmpty() -> "No matches for \"$business\" in \"$near\"."
                    else -> null
                }
            )
        }
    }

    /**
     * One search bar to rule them all. Dispatches to the right backend based on
     * the shape of [rawQuery]:
     *  - Empty: reset to current/automatic location.
     *  - Contains a separator ("in", "near", "@", ","): global business-in-place
     *    search via [searchAnywhere].
     *  - Pure ZIP / digits-only / looks-like-place-only (e.g. "Chicago", "90210"):
     *    move the map there via [searchManualLocation].
     *  - Anything else: business-name search near the current location via
     *    [searchBusinessByName] (falls back to manual location if name search has
     *    no anchor yet).
     */
    fun unifiedSearch(rawQuery: String) {
        val raw = rawQuery.trim()
        if (raw.isEmpty()) {
            load()
            return
        }
        // Anywhere search: explicit separator present.
        val (business, near) = parseAnywhereQuery(raw)
        if (business.isNotEmpty() && near.isNotEmpty()) {
            searchAnywhere(raw)
            return
        }
        // Pure location: 5-digit ZIP, or starts with a digit (e.g. "1600 Penn Ave").
        val looksLikeZip = raw.matches(Regex("""\d{5}(-\d{4})?"""))
        if (looksLikeZip) {
            searchManualLocation(raw)
            return
        }
        // Default: business-name search near current location. If we have no
        // location anchor yet, treat the query as a place name instead.
        if (_state.value.location == null) {
            searchManualLocation(raw)
        } else {
            searchBusinessByName(raw)
        }
    }

    /** Split "{business} in/near/@ {place}" into (business, place). */
    private fun parseAnywhereQuery(raw: String): Pair<String, String> {
        // Order matters: try longer separators first.
        val patterns = listOf(
            Regex("""\s+near\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+in\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s*@\s*"""),
            Regex("""\s*,\s*"""),
        )
        for (p in patterns) {
            val parts = p.split(raw, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return parts[0].trim() to parts[1].trim()
            }
        }
        return raw to ""
    }

    fun toggleSort() {
        val next = if (_state.value.sort == PlacesSort.Distance) PlacesSort.Multiplier
        else PlacesSort.Distance
        _state.value = _state.value.copy(sort = next)
    }

    fun selectPlace(id: Long?) {
        _state.value = _state.value.copy(selectedPlaceId = id)
    }

    /** Expand the radius and re-fetch around the current location. */
    fun expandSearch() {
        val loc = _state.value.location ?: return
        val next = when (_state.value.radiusMeters) {
            in 0..3000 -> 4828   // ~3 mi
            in 3001..6000 -> 16093 // ~10 mi
            else -> 32187          // ~20 mi
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, radiusMeters = next)
            applyLocation(loc)
        }
    }

    fun permissionGranted() {
        _state.value = _state.value.copy(locationDenied = false)
        load()
    }

    fun permissionDenied() {
        _state.value = _state.value.copy(
            locationDenied = true,
            automaticFailed = true,
            loading = false,
        )
    }

    private suspend fun applyLocation(userLoc: UserLocation, isRefresh: Boolean = false) {
        val cards = runCatching { cardRepository.observeCards().first() }.getOrDefault(emptyList())
        val radius = _state.value.radiusMeters
        val nearbyResult = placesRepository.nearby(userLoc.latitude, userLoc.longitude, radius)
        val places = nearbyResult.getOrDefault(emptyList())
        val recs = places.map { p -> recommend(p, cards) }
        _state.value = _state.value.copy(
            location = userLoc,
            places = recs,
            loading = false,
            refreshing = false,
            error = nearbyResult.exceptionOrNull()?.let { "Couldn't load nearby places." },
            // Selection no longer valid after a fetch.
            selectedPlaceId = null,
            nameSearchMode = false,
        )
    }

    private fun recommend(place: NearbyPlace, cards: List<CreditCard>): PlaceRecommendation {
        if (cards.isEmpty()) return PlaceRecommendation(place, null, 1.0, emptyList())
        val options = cards
            .map { CardOption(it, it.multiplierFor(place.category)) }
            .sortedByDescending { it.multiplier }
        val best = options.first()
        return PlaceRecommendation(place, best.card, best.multiplier, options)
    }

}
