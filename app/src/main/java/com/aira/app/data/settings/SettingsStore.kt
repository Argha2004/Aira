package com.aira.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aira.app.domain.engine.Thresholds
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.SavedPlace
import com.aira.app.domain.usecase.AlertSettingsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The user's settings, saved with DataStore. Also remembers when each alert was last sent. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) : AlertSettingsSource {

    /** Night pause: no background snapshots from 23:00 to 06:00. On by default. */
    val nightPauseEnabled: Flow<Boolean> = context.dataStore.data.map { it[NIGHT_PAUSE] ?: true }

    /** Show temperatures in °F instead of °C (data is always stored in °C). */
    val useFahrenheit: Flow<Boolean> = context.dataStore.data.map { it[USE_FAHRENHEIT] ?: false }

    val alertsEnabled: Flow<Boolean> = context.dataStore.data.map { it[ALERTS_ENABLED] ?: true }

    /** Below this battery percent (and not charging) the background job does not read sensors. */
    val batterySaverThreshold: Flow<Int> =
        context.dataStore.data.map { it[BATTERY_THRESHOLD] ?: Thresholds.DEFAULT_BATTERY_SAVER_PERCENT }

    /** The saved place Home shows, or null for the live location. */
    val selectedLocationId: Flow<Long?> = context.dataStore.data.map { it[SELECTED_LOCATION] }

    suspend fun selectedLocationIdNow(): Long? = selectedLocationId.first()

    suspend fun setSelectedLocationId(id: Long?) = context.dataStore.edit {
        if (id == null) it.remove(SELECTED_LOCATION) else it[SELECTED_LOCATION] = id
    }

    /** The name shown in the Home greeting. Empty when the user has not entered one. */
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "" }

    suspend fun setUserName(name: String) = context.dataStore.edit { it[USER_NAME] = name.trim() }

    suspend fun setNightPauseEnabled(enabled: Boolean) = context.dataStore.edit { it[NIGHT_PAUSE] = enabled }

    suspend fun setUseFahrenheit(enabled: Boolean) = context.dataStore.edit { it[USE_FAHRENHEIT] = enabled }

    suspend fun setAlertsEnabled(enabled: Boolean) = context.dataStore.edit { it[ALERTS_ENABLED] = enabled }

    suspend fun setBatterySaverThreshold(percent: Int) = context.dataStore.edit { it[BATTERY_THRESHOLD] = percent }

    /** Which alert types are switched on. All are on by default. */
    val alertTypesEnabled: Flow<Map<AlertType, Boolean>> = context.dataStore.data.map { prefs ->
        AlertType.entries.associateWith { prefs[typeEnabledKey(it)] ?: true }
    }

    /** The minimum time between two alerts of the same type, in minutes (1 h, 3 h or 6 h). */
    val cooldownMinutes: Flow<Int> = context.dataStore.data.map { it[COOLDOWN_MINUTES] ?: DEFAULT_COOLDOWN_MINUTES }

    suspend fun setAlertTypeEnabled(type: AlertType, enabled: Boolean) =
        context.dataStore.edit { it[typeEnabledKey(type)] = enabled }

    suspend fun setCooldownMinutes(minutes: Int) = context.dataStore.edit { it[COOLDOWN_MINUTES] = minutes }

    private fun typeEnabledKey(type: AlertType) = booleanPreferencesKey("alert_enabled_${type.name}")

    // ---- AlertSettingsSource: used by CheckAlertsUseCase ----

    override suspend fun isTypeEnabled(type: AlertType): Boolean = alertTypesEnabled.first().getValue(type)

    override suspend fun cooldownMs(): Long = cooldownMinutes.first() * 60_000L

    override suspend fun alertsEnabled(): Boolean = alertsEnabled.first()

    override suspend fun useFahrenheit(): Boolean = useFahrenheit.first()

    override suspend fun lastSentAt(type: AlertType): Long? = context.dataStore.data.first()[lastSentKey(type)]

    override suspend fun setLastSentAt(type: AlertType, millis: Long) {
        context.dataStore.edit { it[lastSentKey(type)] = millis }
    }

    /** Forgets when alerts were sent (used by "Delete all data"). */
    suspend fun clearAlertHistory() {
        context.dataStore.edit { prefs -> AlertType.entries.forEach { prefs.remove(lastSentKey(it)) } }
    }

    private fun lastSentKey(type: AlertType) = longPreferencesKey("alert_last_sent_${type.name}")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    val loggingEnabled: Flow<Boolean> = context.dataStore.data.map { it[LOGGING_ENABLED] ?: true }

    /** How often background snapshots are taken. */
    val intervalMinutes: Flow<Int> = context.dataStore.data.map { it[INTERVAL_MINUTES] ?: DEFAULT_INTERVAL_MINUTES }

    /** The place the user calls Home, or null if not set. */
    val home: Flow<SavedPlace?> = context.dataStore.data.map { it.placeFor(HOME_KEYS) }

    /** The place the user calls College, or null if not set. */
    val college: Flow<SavedPlace?> = context.dataStore.data.map { it.placeFor(COLLEGE_KEYS) }

    suspend fun setHome(place: SavedPlace) = context.dataStore.edit { it.savePlace(HOME_KEYS, place) }

    suspend fun setCollege(place: SavedPlace) = context.dataStore.edit { it.savePlace(COLLEGE_KEYS, place) }

    private class PlaceKeys(
        val label: Preferences.Key<String>,
        val latitude: Preferences.Key<Double>,
        val longitude: Preferences.Key<Double>,
    )

    private fun Preferences.placeFor(keys: PlaceKeys): SavedPlace? {
        val latitude = this[keys.latitude] ?: return null
        val longitude = this[keys.longitude] ?: return null
        return SavedPlace(this[keys.label] ?: "", latitude, longitude)
    }

    private fun MutablePreferences.savePlace(keys: PlaceKeys, place: SavedPlace) {
        this[keys.label] = place.label
        this[keys.latitude] = place.latitude
        this[keys.longitude] = place.longitude
    }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[ONBOARDING_DONE] = done }

    suspend fun setLoggingEnabled(enabled: Boolean) = context.dataStore.edit { it[LOGGING_ENABLED] = enabled }

    suspend fun setIntervalMinutes(minutes: Int) = context.dataStore.edit { it[INTERVAL_MINUTES] = minutes }

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 5
        const val DEFAULT_COOLDOWN_MINUTES = 180
        private val COOLDOWN_MINUTES = intPreferencesKey("alert_cooldown_minutes")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val LOGGING_ENABLED = booleanPreferencesKey("logging_enabled")
        private val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val SELECTED_LOCATION = longPreferencesKey("selected_location_id")
        private val NIGHT_PAUSE =booleanPreferencesKey("night_pause")
        private val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        private val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val BATTERY_THRESHOLD = intPreferencesKey("battery_saver_threshold")
        private val HOME_KEYS = PlaceKeys(
            stringPreferencesKey("home_label"), doublePreferencesKey("home_lat"), doublePreferencesKey("home_lon"),
        )
        private val COLLEGE_KEYS = PlaceKeys(
            stringPreferencesKey("college_label"), doublePreferencesKey("college_lat"), doublePreferencesKey("college_lon"),
        )
    }
}
