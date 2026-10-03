package com.aira.app.ui.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.ui.components.LocalOnSky
import com.aira.app.ui.components.glassSurface
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.PlaceResult
import com.aira.app.ui.components.WideButton
import com.aira.app.ui.theme.HeatCoral
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint as OsmPoint

private const val START_ZOOM = 12.0
private const val PLACE_ZOOM = 13.0
private const val MOVE_MS = 700L

/**
 * "Add location": an OpenStreetMap map with a pin fixed in the middle. Move the map (or search a place) until
 * the pin is where you want, name the place and save. Closes itself after saving.
 */
@Composable
fun LocationPickerRoute(onDone: () -> Unit, modifier: Modifier = Modifier, viewModel: LocationPickerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val start = state.start
    if (start == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val fallbackName = stringResource(R.string.loc_pinned_place)
    LocationPicker(
        state = state,
        start = start,
        onQueryChange = viewModel::onQueryChange,
        onResultChosen = viewModel::onResultChosen,
        onNameChange = viewModel::onNameChange,
        onCenterChange = viewModel::onCenterChange,
        onSave = { viewModel.save(fallbackName) },
        modifier = modifier,
    )
}

/** The search box's dark frosted fill on the sky. */
private val SearchGlass = Color(0xFF14233A).copy(alpha = 0.78f)

@Composable
private fun LocationPicker(
    state: PickerUiState,
    start: GeoPoint,
    onQueryChange: (String) -> Unit,
    onResultChosen: (PlaceResult) -> Unit,
    onNameChange: (String) -> Unit,
    onCenterChange: (GeoPoint) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val map = rememberMapView(start, onCenterChange)
    val onSky = LocalOnSky.current
    Column(modifier = modifier.fillMaxSize()) {
        // On the sky the map is a rounded panel with the sky around it.
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .then(if (onSky) Modifier.padding(horizontal = 12.dp).glassSurface(MaterialTheme.shapes.large).padding(1.dp).clip(MaterialTheme.shapes.large) else Modifier),
        ) {
            // The map is a classic Android view. Drawing it in its own Compose layer, clipped to its box, makes it
            // move together with the page when the page slides in or out (opening and going back).
            AndroidView(factory = { map }, modifier = Modifier.fillMaxSize().clipToBounds().graphicsLayer())
            // The pin stays in the middle; its tip marks the chosen point, so it is lifted by half its height.
            Icon(
                Icons.Filled.Place,
                contentDescription = stringResource(R.string.loc_pin),
                tint = HeatCoral,
                modifier = Modifier.align(Alignment.Center).size(48.dp).offset(y = (-24).dp),
            )
            SearchBox(
                state = state,
                onQueryChange = onQueryChange,
                onResultChosen = { place ->
                    onResultChosen(place)
                    map.controller.animateTo(OsmPoint(place.latitude, place.longitude), PLACE_ZOOM, MOVE_MS)
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
            )
            Text(
                stringResource(R.string.loc_attribution),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomEnd).background(Color.White.copy(alpha = 0.8f)).padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.DarkGray,
            )
        }
        SaveSheet(state, onNameChange, onSave)
    }
}

/** The search field over the map, with the matching places under it. */
@Composable
private fun SearchBox(state: PickerUiState, onQueryChange: (String) -> Unit, onResultChosen: (PlaceResult) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().shadow(6.dp, MaterialTheme.shapes.large).clip(MaterialTheme.shapes.large)
            // On the sky: dark frosted glass, so the white text stays readable over the light map.
            .background(if (LocalOnSky.current) SearchGlass else MaterialTheme.colorScheme.surface)
            .then(if (LocalOnSky.current) Modifier.glassSurface(MaterialTheme.shapes.large) else Modifier),
    ) {
        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.loc_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = { if (state.searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.results.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                items(state.results) { place ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onResultChosen(place) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(place.name, style = MaterialTheme.typography.titleSmall)
                            if (place.region.isNotEmpty()) {
                                Text(place.region, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else if (state.query.trim().length >= 2 && !state.searching) {
            Text(
                stringResource(R.string.loc_no_results),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Under the map: the coordinates under the pin, a name field and Save. */
@Composable
private fun SaveSheet(state: PickerUiState, onNameChange: (String) -> Unit, onSave: () -> Unit) {
    Column(
        modifier = if (LocalOnSky.current) {
            Modifier.fillMaxWidth().padding(12.dp).glassSurface(MaterialTheme.shapes.large).padding(16.dp)
        } else {
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant).padding(16.dp)
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.loc_move_map), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        state.center?.let {
            Text(
                stringResource(R.string.loc_coordinates, it.latitude, it.longitude),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.loc_name)) },
            placeholder = { Text(stringResource(R.string.loc_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.full) {
            Text(stringResource(R.string.loc_full), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        WideButton(onClick = onSave, enabled = state.center != null && !state.full) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.loc_save), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * One osmdroid map for the screen: OpenStreetMap tiles, pinch to zoom, centred on [start]. It reports the
 * point in the middle whenever it is moved, and follows the screen's lifecycle (pause/resume/detach) as
 * osmdroid requires.
 */
@Composable
private fun rememberMapView(start: GeoPoint, onCenterChange: (GeoPoint) -> Unit): MapView {
    val context = LocalContext.current
    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(START_ZOOM)
            controller.setCenter(OsmPoint(start.latitude, start.longitude))
        }
    }
    DisposableEffect(map) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = report()
            override fun onZoom(event: ZoomEvent?): Boolean = report()
            private fun report(): Boolean {
                val center = map.mapCenter
                onCenterChange(GeoPoint(center.latitude, center.longitude))
                return false
            }
        }
        map.addMapListener(listener)
        onDispose { map.removeMapListener(listener) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, map) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onDetach()
        }
    }
    return map
}
