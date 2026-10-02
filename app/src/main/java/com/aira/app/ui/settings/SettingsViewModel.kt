package com.aira.app.ui.settings

import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.export.DataExporter
import com.aira.app.data.location.LocationProvider
import com.aira.app.data.repository.AlertRepository
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.LocationsRepository
import com.aira.app.domain.model.CustomLocation
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.engine.DemoData
import com.aira.app.domain.engine.LoggingReadiness
import com.aira.app.domain.engine.LoggingStatus
import com.aira.app.domain.engine.PlaceCandidate
import com.aira.app.domain.engine.PlaceSuggester
import com.aira.app.domain.engine.PlaceSuggestions
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.SavedPlace
import com.aira.app.domain.usecase.CheckAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A short message shown in the Data section. */
sealed interface DataMessage {
    data class Exported(val count: Int) : DataMessage
    data object ExportFailed : DataMessage
    data object Deleted : DataMessage
}

/** What the Settings screen shows. */
data class SettingsUiState(
    val loggingEnabled: Boolean = true,
    val intervalMinutes: Int = SettingsStore.DEFAULT_INTERVAL_MINUTES,
    val nightPauseEnabled: Boolean = true,
    val batterySaverPercent: Int = 15,
    val useFahrenheit: Boolean = false,
    val alertsEnabled: Boolean = true,
    /** Which alert types are switched on (all of them by default). */
    val alertTypesEnabled: Map<AlertType, Boolean> = AlertType.entries.associateWith { true },
    /** Minimum minutes between two alerts of the same type: 60, 180 or 360. */
    val cooldownMinutes: Int = 180,
    /** Time of the newest snapshot in epoch millis, or null if there is none yet. */
    val lastSnapshotMillis: Long? = null,
    val home: SavedPlace? = null,
    val college: SavedPlace? = null,
    val suggestions: PlaceSuggestions = PlaceSuggestions(null, null),
    /** True when "Set to current location" could not get a location. */
    val locationFailed: Boolean = false,
    val dataMessage: DataMessage? = null,
    /** True after demo data was generated (debug builds only). */
    val demoDataAdded: Boolean = false,
    /** Whether background logging can really record; the screen warns if it cannot. */
    val loggingStatus: LoggingStatus = LoggingStatus.OK,
    /** The name for the Home greeting; empty if not set. */
    val userName: String = "",
    /** The places saved on the map (at most 5), listed under Places. */
    val savedLocations: List<CustomLocation> = emptyList(),
)

private const val HOME_LABEL = "Home"
private const val COLLEGE_LABEL = "College"
private const val SUGGESTION_LOOKBACK_MS = 30L * 24 * 60 * 60 * 1000

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val snapshots: SnapshotRepository,
    private val diary: DiaryRepository,
    private val locationProvider: LocationProvider,
    private val exporter: DataExporter,
    private val checkAlerts: CheckAlertsUseCase,
    private val alertRepository: AlertRepository,
    private val locations: LocationsRepository,
) : ViewModel() {

    /** Things that are shown once after an action, not saved settings. */
    private data class Transient(
        val locationFailed: Boolean = false,
        val dataMessage: DataMessage? = null,
        val demoDataAdded: Boolean = false,
    )

    private val transient = MutableStateFlow(Transient())
    private val candidates = MutableStateFlow<List<PlaceCandidate>>(emptyList())

    private val preferences = combine(
        settings.loggingEnabled,
        settings.intervalMinutes,
        settings.nightPauseEnabled,
        settings.useFahrenheit,
        settings.alertsEnabled,
    ) { logging, interval, nightPause, fahrenheit, alerts ->
        SettingsUiState(
            loggingEnabled = logging,
            intervalMinutes = interval,
            nightPauseEnabled = nightPause,
            useFahrenheit = fahrenheit,
            alertsEnabled = alerts,
        )
    }.combine(settings.batterySaverThreshold) { state, battery -> state.copy(batterySaverPercent = battery) }
        .combine(settings.alertTypesEnabled) { state, types -> state.copy(alertTypesEnabled = types) }
        .combine(settings.cooldownMinutes) { state, cooldown -> state.copy(cooldownMinutes = cooldown) }
        .combine(settings.userName) { state, name -> state.copy(userName = name) }

    private val places = combine(settings.home, settings.college, candidates) { home, college, found ->
        Triple(home, college, PlaceSuggester.suggestFor(found, home?.point, college?.point))
    }

    /** (location granted, "all the time" granted). The user can change these outside the app. */
    private val permissions = MutableStateFlow(readPermissions())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences,
        snapshots.observeLatest(),
        places,
        transient,
        permissions,
    ) { prefs, latest, (home, college, suggestions), extra, (location, background) ->
        prefs.copy(
            lastSnapshotMillis = latest?.timestamp,
            home = home,
            college = college,
            suggestions = suggestions,
            locationFailed = extra.locationFailed,
            dataMessage = extra.dataMessage,
            demoDataAdded = extra.demoDataAdded,
            loggingStatus = LoggingReadiness.check(prefs.loggingEnabled, location, background, Build.VERSION.SDK_INT),
        )
    }.combine(locations.saved) { state, saved -> state.copy(savedLocations = saved) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Deletes a place saved on the map (Home goes back to the live location if it was showing it). */
    fun deleteSavedLocation(id: Long) = launch { locations.delete(id) }

    private fun readPermissions() = locationProvider.hasPermission() to locationProvider.hasBackgroundPermission()

    /** Called when the screen comes back to the front, e.g. after the user changed a permission in system settings. */
    fun refreshPermissions() {
        permissions.value = readPermissions()
    }

    init {
        // The most visited spots of the last 30 days become the Home/College suggestions.
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            candidates.value = PlaceSuggester.candidates(snapshots.getBetween(now - SUGGESTION_LOOKBACK_MS, now))
        }
    }

    // ---- simple settings. Logging and the interval also start/stop/reschedule the background job (RootViewModel). ----

    fun setLoggingEnabled(enabled: Boolean) = launch { settings.setLoggingEnabled(enabled) }

    fun setIntervalMinutes(minutes: Int) = launch { settings.setIntervalMinutes(minutes) }

    fun setNightPauseEnabled(enabled: Boolean) = launch { settings.setNightPauseEnabled(enabled) }

    fun setBatterySaverPercent(percent: Int) = launch { settings.setBatterySaverThreshold(percent) }

    fun setUseFahrenheit(enabled: Boolean) = launch { settings.setUseFahrenheit(enabled) }

    fun setAlertsEnabled(enabled: Boolean) = launch { settings.setAlertsEnabled(enabled) }

    fun setAlertTypeEnabled(type: AlertType, enabled: Boolean) = launch { settings.setAlertTypeEnabled(type, enabled) }

    fun setCooldownMinutes(minutes: Int) = launch { settings.setCooldownMinutes(minutes) }

    fun setUserName(name: String) = launch { settings.setUserName(name) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // ---- places ----

    fun setHomeHere() = setPlaceHere(HOME_LABEL)

    fun setCollegeHere() = setPlaceHere(COLLEGE_LABEL)

    fun useHomeSuggestion(point: GeoPoint) = savePlace(SavedPlace(HOME_LABEL, point.latitude, point.longitude))

    fun useCollegeSuggestion(point: GeoPoint) = savePlace(SavedPlace(COLLEGE_LABEL, point.latitude, point.longitude))

    private fun setPlaceHere(label: String) {
        viewModelScope.launch {
            val here = locationProvider.getCurrentLocation()
            transient.update { it.copy(locationFailed = here == null) }
            if (here != null) store(SavedPlace(label, here.latitude, here.longitude))
        }
    }

    private fun savePlace(place: SavedPlace) {
        viewModelScope.launch {
            transient.update { it.copy(locationFailed = false) }
            store(place)
        }
    }

    /** Saves the place, then rebuilds today so a commute between Home and College shows up at once. */
    private suspend fun store(place: SavedPlace) {
        if (place.label == HOME_LABEL) settings.setHome(place) else settings.setCollege(place)
        diary.rebuildDay(LocalDate.now())
    }

    // ---- data ----

    /** Writes all snapshots as CSV to the file the user chose. */
    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val message = exporter.export(uri).fold(
                onSuccess = { DataMessage.Exported(it) },
                onFailure = { DataMessage.ExportFailed },
            )
            transient.update { it.copy(dataMessage = message) }
        }
    }

    /** Deletes all recorded data. Settings and places are kept. */
    fun deleteAllData() {
        viewModelScope.launch {
            diary.deleteAll()
            snapshots.deleteAll()
            alertRepository.deleteAll()
            settings.clearAlertHistory()
            transient.update { it.copy(dataMessage = DataMessage.Deleted, demoDataAdded = false) }
        }
    }

    // ---- debug tools (shown in debug builds only) ----

    /**
     * Fires an alert of [type] right away, exactly as a real one: it is saved, the tasks linked to that
     * type are listed, and the notification is shown. The conditions and the cooldown are skipped.
     */
    fun fireTestAlert(type: AlertType) {
        viewModelScope.launch { checkAlerts.fireTest(type) }
    }

    /**
     * Replaces today's snapshots with a made-up day, sets Home and College to the demo's places if they
     * are not set, and rebuilds today's timeline and commutes.
     */
    fun generateDemoData() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val range = DayRange.of(today, zone)
            snapshots.replaceRange(range.first, range.last, DemoData.day(today, zone))
            if (settings.home.first() == null) {
                settings.setHome(SavedPlace(HOME_LABEL, DemoData.HOME.latitude, DemoData.HOME.longitude))
            }
            if (settings.college.first() == null) {
                settings.setCollege(SavedPlace(COLLEGE_LABEL, DemoData.COLLEGE.latitude, DemoData.COLLEGE.longitude))
            }
            diary.rebuildDay(today, zone)
            transient.update { it.copy(demoDataAdded = true) }
        }
    }
}
