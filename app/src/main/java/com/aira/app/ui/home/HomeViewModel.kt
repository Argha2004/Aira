package com.aira.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.location.LocationProvider
import com.aira.app.data.repository.AirQualityRepository
import com.aira.app.data.repository.AlertRepository
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.LocationsRepository
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.repository.TaskRepository
import com.aira.app.data.repository.WeatherRepository
import com.aira.app.domain.engine.DailyStats
import com.aira.app.domain.engine.DailyStatsCalculator
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.engine.ForecastLookup
import com.aira.app.domain.engine.TaskSuggestions
import com.aira.app.domain.engine.TaskTriggerEngine
import com.aira.app.domain.model.AirQuality
import com.aira.app.domain.model.CustomLocation
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherNow
import com.aira.app.domain.model.WeatherTask
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.usecase.TakeSnapshotUseCase
import com.aira.app.domain.usecase.TaskActions
import com.aira.app.domain.usecase.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The weather part of Home: loading, loaded, or failed. */
sealed interface WeatherState {
    data object Loading : WeatherState
    data class Success(
        val weather: WeatherNow,
        val location: GeoPoint,
        val isDefaultLocation: Boolean,
        /** Null while loading or when the air quality service has no data. */
        val air: AirQuality? = null,
        /** The next hours of the forecast, starting with the current hour. */
        val hours: List<HourlyForecast> = emptyList(),
        /** When the weather was loaded (epoch millis), for "Synced 2m ago". */
        val updatedAt: Long = 0,
        /** The name of the saved place shown, or null when this is the live location. */
        val placeName: String? = null,
    ) : WeatherState
    data class Error(val message: String?) : WeatherState
}

/**
 * What the Home screen shows: current weather and the next hours, today's exposure numbers, the newest
 * active alert with its tasks, and today's diary events.
 */
data class HomeUiState(
    val weather: WeatherState = WeatherState.Loading,
    val today: DailyStats = DailyStats.EMPTY,
    val alertTasks: List<WeatherTask> = emptyList(),
    /** Outdoor minutes yesterday, to compare with today. */
    val yesterdayOutdoorMinutes: Int = 0,
    /** The newest active alert (last 24 hours, not dismissed). */
    val activeAlert: WeatherAlert? = null,
    /** Suggested tasks for the active alert that are not already tasks. */
    val suggestions: List<String> = emptyList(),
    val events: List<DiaryEvent> = emptyList(),
    /** Average humidity of yesterday's snapshots, or null if yesterday has none. */
    val yesterdayHumidity: Double? = null,
    /** The newest saved snapshot (always from the live location), for the "Right now" card; null if none yet. */
    val latest: Snapshot? = null,
    /** The places saved on the map (at most 5), for the locations sheet. */
    val places: List<CustomLocation> = emptyList(),
    /** The saved place Home shows, or null for the live location. */
    val selectedPlaceId: Long? = null,
    /** The current temperature of each saved place (by id), filled in when the locations sheet opens. */
    val placeTemps: Map<Long, Double> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val airQuality: AirQualityRepository,
    private val locationProvider: LocationProvider,
    private val diaryRepository: DiaryRepository,
    private val snapshots: SnapshotRepository,
    alerts: AlertRepository,
    tasks: TaskRepository,
    private val taskActions: TaskActions,
    private val locations: LocationsRepository,
    private val takeSnapshot: TakeSnapshotUseCase,
    private val settings: SettingsStore,
) : ViewModel() {

    private val weather = MutableStateFlow<WeatherState>(WeatherState.Loading)
    private val today = MutableStateFlow(LocalDate.now())
    private val zone = ZoneId.systemDefault()

    private val activeAlert = alerts.observeActive().map { it.firstOrNull() }

    /** Tasks linked to the types of the alerts that are active (last 24 hours, not dismissed). */
    private val alertTasks = combine(alerts.observeActive(), tasks.observePending()) { activeAlerts, pending ->
        TaskTriggerEngine.tasksForAlerts(activeAlerts.map { it.type }.toSet(), pending, System.currentTimeMillis())
    }

    private val main = combine(
        weather,
        today.flatMapLatest { diaryRepository.observeDailyStats(it) },
        alertTasks,
        activeAlert,
    ) { weatherState, stats, tasksForWeather, alert ->
        val existing = tasksForWeather.map { it.title.lowercase() }.toSet()
        HomeUiState(
            weather = weatherState,
            today = stats,
            alertTasks = tasksForWeather,
            activeAlert = alert,
            suggestions = alert?.let { TaskSuggestions.forAlert(it.type).filter { s -> s.lowercase() !in existing } }.orEmpty(),
        )
    }

    /** Yesterday's outdoor minutes and average humidity (snapshots still waiting for weather are left out). */
    private val yesterday = today.flatMapLatest { date ->
        val range = DayRange.of(date.minusDays(1), zone)
        snapshots.observeBetween(range.first, range.last).map { list ->
            val humidity = list.filter { !it.weatherPending }.map { it.humidity }.takeIf { it.isNotEmpty() }?.average()
            humidity to DailyStatsCalculator.calculate(list).outdoorMinutes
        }
    }

    private val events = today.flatMapLatest { date ->
        DayRange.of(date, zone).let { diaryRepository.observeEvents(it.first, it.last) }
    }

    private val placeTemps = MutableStateFlow<Map<Long, Double>>(emptyMap())

    private val places = combine(locations.saved, locations.selectedId, placeTemps) { saved, selected, temps ->
        Triple(saved, selected, temps)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        main,
        yesterday,
        events,
        places,
        snapshots.observeLatest(),
    ) { base, (humidity, outdoor), dayEvents, (saved, selected, temps), newest ->
        base.copy(
            latest = newest,
            yesterdayHumidity = humidity,
            yesterdayOutdoorMinutes = outdoor,
            events = dayEvents,
            places = saved,
            selectedPlaceId = selected,
            placeTemps = temps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // When another place is chosen (here or after adding one on the map), load its weather.
        viewModelScope.launch { locations.selectedId.distinctUntilChanged().drop(1).collect { load() } }
    }

    private var liveJob: Job? = null

    /**
     * While Home is on screen: takes a snapshot whenever the newest one is [LIVE_REFRESH_MS] old (at once if it
     * is older already), then updates today's timeline. So the "Right now" card and the Timeline follow the user
     * every 5 minutes without waiting for the background job. Stops in [stopLive]. Does nothing when logging is off.
     */
    fun startLive() {
        if (liveJob?.isActive == true) return
        liveJob = viewModelScope.launch {
            while (isActive) {
                val last = snapshots.getLatest()?.timestamp
                val age = last?.let { System.currentTimeMillis() - it } ?: Long.MAX_VALUE
                if (age >= LIVE_REFRESH_MS && settings.loggingEnabled.first()) {
                    takeSnapshot().onSuccess { runCatchingCancellable { diaryRepository.rebuildDay(LocalDate.now()) } }
                    delay(LIVE_REFRESH_MS)
                } else {
                    delay((LIVE_REFRESH_MS - age).coerceIn(MIN_WAIT_MS, LIVE_REFRESH_MS))
                }
            }
        }
    }

    fun stopLive() {
        liveJob?.cancel()
        liveJob = null
    }

    /** Shows a saved place on Home, or the live location for null. */
    fun selectPlace(id: Long?) {
        viewModelScope.launch { locations.select(id) }
    }

    fun deletePlace(id: Long) {
        viewModelScope.launch { locations.delete(id) }
    }

    /** Loads the temperature of every saved place for the locations sheet (from the cache when it is fresh). */
    fun loadPlaceTemps() {
        viewModelScope.launch {
            locations.saved.first().forEach { place ->
                weatherRepository.getWeather(place.latitude, place.longitude).onSuccess { now ->
                    placeTemps.update { it + (place.id to now.tempC) }
                }
            }
        }
    }

    /** "Acknowledge" on the alert card: dismisses the alert. */
    fun dismissAlert(alertId: Long) {
        viewModelScope.launch { taskActions.dismissAlert(alertId) }
    }

    /**
     * "Remind in 30m" on the alert card: snoozes the alert's first task for 30 minutes, or, if it has none,
     * adds the first suggested task due in 30 minutes (so its reminder comes then).
     */
    fun remindLater(alertId: Long, taskId: Long?, suggestion: String?) {
        viewModelScope.launch {
            when {
                taskId != null -> taskActions.snooze(taskId, REMIND_LATER_MS)
                suggestion != null -> taskActions.addSuggestion(alertId, suggestion, System.currentTimeMillis() + REMIND_LATER_MS)
            }
        }
    }

    /**
     * Loads the weather for the chosen saved place, or for the phone's location (Kolkata if there is no
     * location permission). Called when the screen appears, when another place is chosen, and by Retry.
     */
    fun load() {
        today.value = LocalDate.now()
        if (weather.value !is WeatherState.Success) weather.value = WeatherState.Loading
        viewModelScope.launch {
            val chosen = locations.selectedId.first()?.let { locations.get(it) }
            val deviceLocation = if (chosen == null) locationProvider.getCurrentLocation() else null
            val location = chosen?.let { GeoPoint(it.latitude, it.longitude) } ?: deviceLocation ?: KOLKATA
            weatherRepository.getBundle(location.latitude, location.longitude)
                .onSuccess {
                    // Keep the air quality only while it is still the same place.
                    val previousAir = (weather.value as? WeatherState.Success)?.takeIf { old -> old.location == location }?.air
                    val now = System.currentTimeMillis()
                    weather.value = WeatherState.Success(
                        weather = it.weather,
                        location = location,
                        isDefaultLocation = chosen == null && deviceLocation == null,
                        placeName = chosen?.name,
                        air = previousAir,
                        hours = ForecastLookup.nextHours(it.forecast, now, HOURS_SHOWN),
                        updatedAt = now,
                    )
                    loadAirQuality(location)
                }
                .onFailure { weather.value = WeatherState.Error(it.message) }
        }
    }

    /** The air quality arrives after the weather; if it fails only its card is hidden. */
    private suspend fun loadAirQuality(location: GeoPoint) {
        val air = airQuality.get(location.latitude, location.longitude) ?: return
        (weather.value as? WeatherState.Success)?.let { weather.value = it.copy(air = air) }
    }

    private companion object {
        val KOLKATA = GeoPoint(22.57, 88.36)
        const val HOURS_SHOWN = 24
        const val REMIND_LATER_MS = 30 * 60 * 1000L
        const val LIVE_REFRESH_MS = 5 * 60 * 1000L
        const val MIN_WAIT_MS = 10 * 1000L
    }
}
