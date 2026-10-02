package com.aira.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.repository.AlertRepository
import com.aira.app.data.repository.TaskRepository
import com.aira.app.data.sensor.SensorAvailability
import com.aira.app.data.location.LocationProvider
import com.aira.app.data.repository.WeatherRepository
import com.aira.app.domain.engine.ForecastChange
import com.aira.app.domain.engine.ForecastLookup
import com.aira.app.domain.engine.TaskGrouper
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.engine.TaskGroups
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.usecase.TaskActions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksUiState(
    /** How many alerts are active (last 24 hours, not dismissed): the "Active triggers" card. */
    val activeAlerts: Int = 0,
    val groups: TaskGroups = TaskGroups(emptyList(), emptyList(), emptyList()),
    val done: List<WeatherTask> = emptyList(),
    /** The pending task with the soonest due time; shown in the "Next change" card when the forecast has none. */
    val nextDue: WeatherTask? = null,
    /** The next rainy or very hot hour in the forecast, for the "Next change" card; null if none is coming. */
    val nextChange: ForecastChange? = null,
    /** The "when falling pressure" choice is only offered on phones with a barometer. */
    val hasBarometer: Boolean = true,
    /** True until the first data has arrived, so the empty messages are not shown by mistake. */
    val isLoading: Boolean = false,
    /** True if the tasks and alerts could not be read. */
    val hasError: Boolean = false,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    alerts: AlertRepository,
    tasks: TaskRepository,
    availability: SensorAvailability,
    private val actions: TaskActions,
    private val weather: WeatherRepository,
    private val locations: LocationProvider,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val hasBarometer = availability.status().pressure

    private val nextChange = MutableStateFlow<ForecastChange?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        alerts.observeActive(),
        tasks.observePending(),
        tasks.observeDone(),
        nextChange,
    ) { activeAlerts, pending, done, change ->
        val now = System.currentTimeMillis()
        TasksUiState(
            activeAlerts = activeAlerts.size,
            groups = TaskGrouper.group(pending, now, zone),
            done = done,
            nextDue = pending.filter { (it.dueTime ?: 0L) >= now }.minByOrNull { it.dueTime!! },
            hasBarometer = hasBarometer,
            nextChange = change,
        )
    }
        .catch { emit(TasksUiState(hasBarometer = hasBarometer, hasError = true)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TasksUiState(hasBarometer = hasBarometer, isLoading = true),
        )

    init {
        // The forecast usually comes from the cache filled by Home, so this rarely needs the network.
        viewModelScope.launch {
            val place = locations.getCurrentLocation() ?: DEFAULT_LOCATION
            weather.getForecast(place.latitude, place.longitude).onSuccess {
                nextChange.value = ForecastLookup.nextChange(it, System.currentTimeMillis())
            }
        }
    }

    fun markDone(taskId: Long) = launch { actions.markDone(taskId) }

    fun reopen(taskId: Long) = launch { actions.reopen(taskId) }

    /** Saves a new task (id 0) or the changes to an existing one. */
    fun save(task: WeatherTask) = launch { if (task.id == 0L) actions.add(task) else actions.update(task) }

    fun delete(taskId: Long) = launch { actions.delete(taskId) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        /** The same fallback as Home when there is no location permission. */
        val DEFAULT_LOCATION = GeoPoint(22.57, 88.36)
    }
}
