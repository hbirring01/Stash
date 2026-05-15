package com.example.creditcardapp.ui.rewards

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.creditcardapp.domain.model.RewardCategory
import com.example.creditcardapp.ui.permission.RequestLocationPermission
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val SAMPLE_SPEND_DOLLARS = 50.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsMapScreen(
    onBack: () -> Unit,
    viewModel: RewardsMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Configure osmdroid BEFORE any MapView is constructed.
    val osmReady = remember {
        Configuration.getInstance().load(
            context.applicationContext,
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    RequestLocationPermission { granted ->
        if (granted) viewModel.permissionGranted() else viewModel.permissionDenied()
    }

    // Tracks where the user has panned/zoomed the map. Used to decide whether to
    // show the "Search this area" floating button.
    var mapCenter by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val anchorLat = state.location?.latitude ?: DEFAULT_LAT
    val anchorLon = state.location?.longitude ?: DEFAULT_LON
    val centerDelta = mapCenter?.let { (lat, lon) -> haversineMeters(lat, lon, anchorLat, anchorLon) } ?: 0.0
    val showSearchHere = state.location != null && centerDelta > 400.0

    val listState = rememberLazyListState()
    val filtered = state.filteredPlaces
    LaunchedEffect(state.selectedPlaceId, filtered) {
        val id = state.selectedPlaceId ?: return@LaunchedEffect
        val idx = filtered.indexOfFirst { it.place.id == id }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    // Collapse the map once businesses are loaded so the list gets full screen.
    // User can re-expand via the toggle in the results header.
    var mapExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(filtered.isNotEmpty()) {
        if (filtered.isNotEmpty()) mapExpanded = false
    }
    // Re-tapping a marker on the map needs the map visible; selecting from list
    // doesn't, so we leave the user in control after the initial collapse.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Best card nearby", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSort() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = if (state.sort == PlacesSort.Distance)
                                "Sort by distance (tap to switch to points)"
                            else "Sort by points (tap to switch to distance)",
                            tint = if (state.sort == PlacesSort.Multiplier)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Global business search (no device location tether). Type
            // "Starbucks in Chicago" / "Best Buy near 90210" / "Apple @ NYC".
            GlobalBusinessSearchBar(
                onSearch = { viewModel.searchAnywhere(it) },
                onClear = { viewModel.load() },
            )

            // Map area — collapsible. Hidden by default once results load so the
            // business list can use the full screen.
            if (mapExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (osmReady) {
                        OsmMap(
                            centerLat = anchorLat,
                            centerLon = anchorLon,
                            hasUser = state.location != null,
                            markers = state.places.map { rec ->
                                MapMarker(
                                    placeId = rec.place.id,
                                    title = rec.place.name,
                                    snippet = rec.bestCard?.let { c ->
                                        "${c.brand} •••• ${c.last4} (${formatX(rec.multiplier)})"
                                    } ?: "No card recommendation",
                                    lat = rec.place.latitude,
                                    lon = rec.place.longitude,
                                    category = rec.place.category,
                                    selected = rec.place.id == state.selectedPlaceId,
                                )
                            },
                            onMapLongPress = { lat, lon -> viewModel.useMapPoint(lat, lon) },
                            onCenterChanged = { lat, lon -> mapCenter = lat to lon },
                            onMarkerTap = { id -> viewModel.selectPlace(id) },
                        )
                    }
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (state.location == null && !state.locationDenied) {
                        Text(
                            text = "Locating…",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (state.locationDenied) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Map, contentDescription = null)
                            Spacer(Modifier.height(8.dp))
                            Text("Location permission denied", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Enter a city or ZIP below, or grant permission in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            TextButton(onClick = { openAppSettings(context) }) {
                                Icon(Icons.Outlined.Settings, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open Settings")
                            }
                        }
                    }

                    // Floating "Search this area" button — surfaces when the user pans the map.
                    if (showSearchHere) {
                        val c = mapCenter
                        ElevatedButton(
                            onClick = {
                                if (c != null) viewModel.searchHere(c.first, c.second)
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                        ) {
                            Icon(Icons.Outlined.TravelExplore, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Search this area")
                        }
                    }
                }
                }
            }

            if (mapExpanded) {
                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Tip: long-press the map to drop a custom pin · tap a pin to highlight it below.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            ManualLocationField(
                currentLabel = state.location?.label?.takeIf { state.location?.isManual == true },
                onSearch = { viewModel.searchManualLocation(it) },
                onUseAutomatic = { viewModel.load() },
                showUseAutomatic = state.automaticFailed || state.location?.isManual == true,
            )

            Spacer(Modifier.height(8.dp))

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Category filter chips
            if (state.places.isNotEmpty() || state.nameSearchMode) {
                BusinessNameFilter(
                    value = state.nameFilter,
                    onChange = { viewModel.setNameFilter(it) },
                    onSubmit = { viewModel.searchBusinessByName(it) },
                    onClear = { viewModel.searchBusinessByName("") },
                    inSearchMode = state.nameSearchMode,
                )
                if (state.places.isNotEmpty()) {
                    CategoryFilterChips(
                        available = state.availableCategories,
                        selected = state.categoryFilter,
                        onSelect = { viewModel.selectCategory(it) },
                    )
                }
            }

            // Empty state
            if (filtered.isEmpty() && !state.loading && state.location != null) {
                EmptyStateBlock(
                    radiusMeters = state.radiusMeters,
                    onExpand = { viewModel.expandSearch() },
                )
            }

            if (filtered.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val catFilter = state.categoryFilter
                    Text(
                        text = if (catFilter == null) "Businesses near you"
                        else "${catFilter.displayName} nearby",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${filtered.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { mapExpanded = !mapExpanded }) {
                        Icon(
                            imageVector = if (mapExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.Map,
                            contentDescription = if (mapExpanded) "Hide map" else "Show map",
                        )
                    }
                }
            }

            // Pull-to-refresh wraps the LazyColumn. `weight(1f)` is critical:
            // it tells the parent Column to give this box all remaining height,
            // which lets the LazyColumn inside scroll independently of the map.
            val refreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                state = refreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.place.id }) { rec ->
                        PlaceCard(
                            rec = rec,
                            selected = rec.place.id == state.selectedPlaceId,
                            onTap = { viewModel.selectPlace(rec.place.id) },
                            onOpenInMaps = {
                                val uri = Uri.parse("geo:${rec.place.latitude},${rec.place.longitude}?q=${Uri.encode(rec.place.name)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                runCatching {
                                    ContextCompat.startActivity(context, intent, null)
                                }.onFailure {
                                    ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, uri), null)
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// --------------------------- Map ---------------------------

private data class MapMarker(
    val placeId: Long,
    val title: String,
    val snippet: String,
    val lat: Double,
    val lon: Double,
    val category: RewardCategory,
    val selected: Boolean,
)

@Composable
private fun OsmMap(
    centerLat: Double,
    centerLon: Double,
    hasUser: Boolean,
    markers: List<MapMarker>,
    onMapLongPress: (Double, Double) -> Unit,
    onCenterChanged: (Double, Double) -> Unit,
    onMarkerTap: (Long) -> Unit,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            // CARTO "Voyager" raster tiles — closer to Google Maps' clean palette
            // than the default OSM MAPNIK style. Free for reasonable, attributed use.
            setTileSource(
                XYTileSource(
                    "CartoVoyager",
                    0, 20, 256, ".png",
                    arrayOf(
                        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
                    ),
                    "\u00a9 OpenStreetMap contributors \u00a9 CARTO"
                )
            )
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            // Silence the MAPNIK reference so lint/tooling doesn't complain.
            @Suppress("UNUSED_EXPRESSION") TileSourceFactory.MAPNIK
        }
    }

    // Set initial center + zoom and attach the pan listener once.
    DisposableEffect(Unit) {
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val c = mapView.mapCenter
                onCenterChanged(c.latitude, c.longitude)
                return false
            }
            override fun onZoom(event: ZoomEvent?): Boolean = false
        }
        mapView.addMapListener(listener)
        onDispose {
            mapView.removeMapListener(listener)
            mapView.onDetach()
        }
    }

    // Programmatic pan when the anchor (user location / searched location) changes.
    LaunchedEffect(centerLat, centerLon) {
        mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
    }

    // Re-bind the long-press receiver each composition so it sees the latest lambda.
    DisposableEffect(onMapLongPress) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    onMapLongPress(p.latitude, p.longitude)
                    return true
                }
                return false
            }
        }
        val overlay = MapEventsOverlay(receiver)
        mapView.overlays.add(0, overlay)
        onDispose { mapView.overlays.remove(overlay) }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            // Preserve the MapEventsOverlay (index 0) and rebuild marker overlays only.
            while (view.overlays.size > 1) view.overlays.removeAt(view.overlays.size - 1)
            if (hasUser) {
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(centerLat, centerLon)
                    title = "You are here"
                    icon = userLocationIcon(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
            }
            markers.forEach { m ->
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(m.lat, m.lon)
                    title = m.title
                    snippet = m.snippet
                    icon = categoryMarkerIcon(context, m.category, selected = m.selected)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        onMarkerTap(m.placeId)
                        true
                    }
                })
            }
            view.invalidate()
        }
    )
}

// --------------------------- Filter chips ---------------------------

@Composable
private fun GlobalBusinessSearchBar(
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val canSearch = query.trim().isNotEmpty()
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        placeholder = { Text("Find any business (e.g. Mezeh in Ellicott City)") },
        leadingIcon = { Icon(Icons.Outlined.TravelExplore, contentDescription = null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onClear()
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                    }
                }
                IconButton(
                    onClick = {
                        keyboard?.hide()
                        onSearch(query)
                    },
                    enabled = canSearch,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = if (canSearch) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboard?.hide()
                onSearch(query)
            },
            onDone = {
                keyboard?.hide()
                onSearch(query)
            },
            onGo = {
                keyboard?.hide()
                onSearch(query)
            },
        ),
        shape = RoundedCornerShape(24.dp),
    )
    Text(
        text = "Tip: include a location, e.g. \"Mezeh in Ellicott City\" or \"Best Buy near 90210\".",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

@Composable
private fun BusinessNameFilter(
    value: String,
    onChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onClear: () -> Unit,
    inSearchMode: Boolean,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        placeholder = { Text("Search businesses (e.g. Starbucks)") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty() || inSearchMode) {
                IconButton(onClick = {
                    onChange("")
                    onClear()
                }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            onSubmit(value)
        }),
        shape = RoundedCornerShape(20.dp),
    )
    if (inSearchMode && value.isNotBlank()) {
        Text(
            text = "Showing nearest \"$value\" within ~30 mi. Clear to return to map area.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CategoryFilterChips(
    available: List<RewardCategory>,
    selected: RewardCategory?,
    onSelect: (RewardCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
            shape = RoundedCornerShape(20.dp),
        )
        available.forEach { cat ->
            val color = Color(categoryColor(cat))
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(if (selected == cat) null else cat) },
                label = { Text(cat.displayName) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    }
}

// --------------------------- Place card ---------------------------

@Composable
private fun PlaceCard(
    rec: PlaceRecommendation,
    selected: Boolean,
    onTap: () -> Unit,
    onOpenInMaps: () -> Unit,
) {
    var expanded by rememberSaveable(rec.place.id) { mutableStateOf(false) }
    val categoryTint = Color(categoryColor(rec.place.category))
    val containerColor = if (selected) categoryTint.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = if (selected) 3.dp else 1.dp,
        onClick = onTap,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaceLogo(
                    name = rec.place.name,
                    logoDomain = rec.place.logoDomain,
                    tint = categoryTint,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = rec.place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess
                        else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            Text(
                text = "${rec.place.subtype.replaceFirstChar { it.titlecase() }} · ${formatDistance(rec.place.distanceMeters)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            val card = rec.bestCard
            if (card == null) {
                Text("Add a card to get recommendations.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "Use ${card.brand} •••• ${card.last4}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${formatX(rec.multiplier)} on ${rec.place.category.displayName.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "≈ ${(SAMPLE_SPEND_DOLLARS * rec.multiplier).roundToInt()} pts on \$${SAMPLE_SPEND_DOLLARS.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded && rec.allOptions.size > 1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "All your cards here",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                rec.allOptions.drop(1).forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${opt.card.brand} •••• ${opt.card.last4}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${formatX(opt.multiplier)} · ${(SAMPLE_SPEND_DOLLARS * opt.multiplier).roundToInt()} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onOpenInMaps) {
                Text("Directions", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// --------------------------- Empty state ---------------------------

@Composable
private fun EmptyStateBlock(radiusMeters: Int, onExpand: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "No businesses found within ${formatRadius(radiusMeters)}.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        AssistChip(
            onClick = onExpand,
            label = { Text("Search a wider area") },
            leadingIcon = { Icon(Icons.Outlined.TravelExplore, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You can also try a different city/ZIP, or long-press the map to drop a pin elsewhere.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// --------------------------- Manual location field ---------------------------

@Composable
private fun ManualLocationField(
    currentLabel: String?,
    onSearch: (String) -> Unit,
    onUseAutomatic: () -> Unit,
    showUseAutomatic: Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("City or ZIP") },
                leadingIcon = { Icon(Icons.Outlined.LocationSearching, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    onSearch(query)
                }),
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    keyboard?.hide()
                    onSearch(query)
                },
                enabled = query.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
        }
        if (currentLabel != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Using manual location: $currentLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showUseAutomatic) {
            TextButton(onClick = onUseAutomatic) {
                Text("Use my current location instead")
            }
        }
    }
}

// --------------------------- Formatting helpers ---------------------------

private fun formatX(multiplier: Double): String =
    if (multiplier % 1.0 == 0.0) "${multiplier.toInt()}x" else "%.1fx".format(multiplier)

/** Imperial distance: feet under ~0.1 mi, otherwise miles to 1 decimal. */
private fun formatDistance(meters: Double): String {
    val feet = meters * 3.28084
    return if (feet < 528.0) "${feet.roundToInt()} ft"
    else "%.1f mi".format(meters / 1609.344)
}

private fun formatRadius(meters: Int): String =
    "%.1f mi".format(meters / 1609.344).removeSuffix(".0 mi")
        .let { if (it.endsWith(" mi")) it else "$it mi" }

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ContextCompat.startActivity(context, intent, null)
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2).let { it * it }
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

// Mountain View, CA — fallback while we wait for a real GPS fix.
private const val DEFAULT_LAT = 37.4220
private const val DEFAULT_LON = -122.0841

/**
 * Circular logo for a business. Loads from Clearbit Logo
 * (`https://logo.clearbit.com/<domain>?size=128`) when we have a website
 * domain. Falls back to a colored circle with the first letter of the
 * business name while loading, on error, or when no domain is available.
 *
 * Clearbit Logo is free, no API key required.
 */
@Composable
private fun PlaceLogo(
    name: String,
    logoDomain: String?,
    tint: Color,
) {
    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
        }
    }

    if (logoDomain.isNullOrBlank()) {
        fallback()
        return
    }

    val context = LocalContext.current
    val request = remember(logoDomain) {
        ImageRequest.Builder(context)
            .data("https://logo.clearbit.com/$logoDomain?size=128")
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = "$name logo",
        modifier = Modifier
            .width(40.dp)
            .height(40.dp)
            .clip(CircleShape)
            .background(Color.White),
        loading = { fallback() },
        error = { fallback() },
    )
}



