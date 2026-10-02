package com.aira.app.domain.usecase

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherNow
import com.aira.app.domain.model.WeatherTask

// Small interfaces so the use cases can be tested on the JVM with fake versions.

interface SensorSource {
    suspend fun readLux(): Float?
    suspend fun readInPocket(): Boolean?
    suspend fun readStepCounter(): Long?
    suspend fun readPressure(): Float?
}

interface LocationSource {
    suspend fun getCurrentLocation(): GeoPoint?
}

interface WeatherSource {
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherNow>
}

/** The hourly forecast (today and tomorrow) for a location. */
interface ForecastSource {
    suspend fun getForecast(latitude: Double, longitude: Double): Result<List<HourlyForecast>>
}

interface SnapshotStore {
    suspend fun getLatest(): Snapshot?
    suspend fun save(snapshot: Snapshot): Long
    suspend fun getBetween(from: Long, to: Long): List<Snapshot>
}

/** Where fired alerts are saved. */
interface AlertStore {
    suspend fun insert(alert: WeatherAlert): Long
    suspend fun get(id: Long): WeatherAlert?
    suspend fun dismiss(id: Long)
}

/** Where tasks are saved. */
interface TaskStore {
    suspend fun insert(task: WeatherTask): Long
    suspend fun update(task: WeatherTask)
    suspend fun delete(id: Long)
    suspend fun get(id: Long): WeatherTask?
    suspend fun all(): List<WeatherTask>
}

/** Sets up (or cancels) the background reminders of a task. */
interface ReminderScheduler {
    fun schedule(task: WeatherTask)
    fun cancel(taskId: Long)
}

/** The settings the alert check needs, and the record of when each alert was last sent. */
interface AlertSettingsSource {
    /** The master switch for all alerts. */
    suspend fun alertsEnabled(): Boolean

    /** The switch of one alert type. */
    suspend fun isTypeEnabled(type: AlertType): Boolean

    /** The minimum time between two alerts of the same type. */
    suspend fun cooldownMs(): Long

    suspend fun useFahrenheit(): Boolean
    suspend fun lastSentAt(type: AlertType): Long?
    suspend fun setLastSentAt(type: AlertType, millis: Long)
}

/** Shows an alert to the user (a notification), with the tasks linked to that alert type. */
interface AlertNotifier {
    fun show(alert: WeatherAlert, tasks: List<WeatherTask>, useFahrenheit: Boolean)
}
