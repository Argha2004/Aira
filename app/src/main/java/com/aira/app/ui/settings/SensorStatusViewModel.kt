package com.aira.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.location.LocationProvider
import com.aira.app.data.sensor.SensorAvailability
import com.aira.app.data.sensor.SensorReader
import com.aira.app.data.sensor.SensorStatus
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.usecase.NoLocationException
import com.aira.app.domain.usecase.TakeSnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One round of readings. A null value means "not available right now". */
data class SensorReading(
    val lux: Float?,
    val inPocket: Boolean?,
    val totalSteps: Long?,
    val pressure: Float?,
    val movement: Float?,
    val location: GeoPoint?,
)

/** Result of pressing "Take snapshot now". */
sealed interface SnapshotOutcome {
    data class Saved(val snapshot: Snapshot) : SnapshotOutcome
    data class Failed(val noLocation: Boolean, val message: String?) : SnapshotOutcome
}

data class SensorStatusUiState(
    val availability: SensorStatus,
    val isReading: Boolean = false,
    val reading: SensorReading? = null,
    val isTakingSnapshot: Boolean = false,
    val snapshotOutcome: SnapshotOutcome? = null,
)

@HiltViewModel
class SensorStatusViewModel @Inject constructor(
    availability: SensorAvailability,
    private val sensorReader: SensorReader,
    private val locationProvider: LocationProvider,
    private val takeSnapshot: TakeSnapshotUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensorStatusUiState(availability.status()))
    val uiState: StateFlow<SensorStatusUiState> = _uiState.asStateFlow()

    /** Runs the same snapshot the background worker will run, and shows what was saved. */
    fun takeSnapshotNow() {
        if (_uiState.value.isTakingSnapshot) return
        _uiState.update { it.copy(isTakingSnapshot = true) }
        viewModelScope.launch {
            val outcome = takeSnapshot().fold(
                onSuccess = { SnapshotOutcome.Saved(it) },
                onFailure = { SnapshotOutcome.Failed(noLocation = it is NoLocationException, message = it.message) },
            )
            _uiState.update { it.copy(isTakingSnapshot = false, snapshotOutcome = outcome) }
        }
    }

    /** Reads all sensors and the location at the same time (takes about 3 seconds). */
    fun readNow() {
        if (_uiState.value.isReading) return
        _uiState.update { it.copy(isReading = true) }
        viewModelScope.launch {
            val reading = coroutineScope {
                val lux = async { sensorReader.readLux() }
                val pocket = async { sensorReader.readInPocket() }
                val steps = async { sensorReader.readStepCounter() }
                val pressure = async { sensorReader.readPressure() }
                val movement = async { sensorReader.sampleMovement() }
                val location = async { locationProvider.getCurrentLocation() }
                SensorReading(
                    lux.await(), pocket.await(), steps.await(),
                    pressure.await(), movement.await(), location.await(),
                )
            }
            _uiState.update { it.copy(isReading = false, reading = reading) }
        }
    }
}
