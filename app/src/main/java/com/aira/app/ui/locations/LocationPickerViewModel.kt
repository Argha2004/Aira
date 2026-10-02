package com.aira.app.ui.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.location.LocationProvider
import com.aira.app.data.repository.LocationsRepository
import com.aira.app.domain.engine.LocationRules
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.PlaceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the map picker shows. */
data class PickerUiState(
    /** Where the map opens: the live location, or Kolkata without permission. Null while it is being found. */
    val start: GeoPoint? = null,
    val query: String = "",
    val results: List<PlaceResult> = emptyList(),
    val searching: Boolean = false,
    /** The name in the "Name this place" field; filled in from a search result, editable. */
    val name: String = "",
    /** The point under the pin in the middle of the map. */
    val center: GeoPoint? = null,
    /** Set when the place was saved: the screen then closes. */
    val saved: Boolean = false,
    /** True when 5 places are saved already (should not happen: Home hides "Add location" then). */
    val full: Boolean = false,
)

/**
 * Behind the map picker: finds where to open the map, searches places as the user types (after a short
 * pause, so not every letter is a request), and saves the point under the pin.
 */
@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val locations: LocationsRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(PickerUiState())
    val state: StateFlow<PickerUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val start = locationProvider.getCurrentLocation() ?: KOLKATA
            _state.update { it.copy(start = start, center = start) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_PAUSE_MS)
            _state.update { it.copy(searching = true) }
            val results = locations.search(query)
            _state.update { it.copy(results = results, searching = false) }
        }
    }

    /** A search result was tapped: the screen moves the map there, and its name fills the name field. */
    fun onResultChosen(place: PlaceResult) {
        searchJob?.cancel()
        _state.update { it.copy(query = "", results = emptyList(), name = place.name, center = GeoPoint(place.latitude, place.longitude)) }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name.take(LocationRules.MAX_NAME_LENGTH)) }

    /** The map was moved: remember the point under the pin. */
    fun onCenterChange(point: GeoPoint) = _state.update { it.copy(center = point) }

    /** Saves the point under the pin with the typed name ([fallbackName] if none was typed). */
    fun save(fallbackName: String) {
        val center = _state.value.center ?: return
        viewModelScope.launch {
            val ok = locations.add(LocationRules.cleanName(_state.value.name, fallbackName), center.latitude, center.longitude)
            _state.update { if (ok) it.copy(saved = true) else it.copy(full = true) }
        }
    }

    private companion object {
        val KOLKATA = GeoPoint(22.57, 88.36)
        const val SEARCH_PAUSE_MS = 400L
    }
}
