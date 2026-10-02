package com.aira.app.domain.usecase

import com.aira.app.domain.engine.AlertCooldown
import com.aira.app.domain.engine.AlertEngine
import com.aira.app.domain.engine.AlertMessages
import com.aira.app.domain.engine.PressureTrend
import com.aira.app.domain.engine.TaskTriggerEngine
import com.aira.app.domain.engine.Thresholds
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.model.WeatherAlert
import java.time.ZoneId
import javax.inject.Inject

/**
 * Runs at the end of every snapshot. Looks at the last 3 hours and the hourly forecast, asks the
 * [AlertEngine] which alerts apply, drops the alert types the user turned off and those sent within the
 * cooldown, and fires the rest: the alert is saved, the tasks linked to it are collected (an "every time"
 * task that was done comes back), and a notification is shown.
 * It does nothing when alerts are off, or when the newest snapshot is too old to say anything about "now".
 */
class CheckAlertsUseCase(
    private val store: SnapshotStore,
    private val forecasts: ForecastSource,
    private val settings: AlertSettingsSource,
    private val alerts: AlertStore,
    private val tasks: TaskStore,
    private val notifier: AlertNotifier,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long,
) {
    @Inject
    constructor(
        store: SnapshotStore,
        forecasts: ForecastSource,
        settings: AlertSettingsSource,
        alerts: AlertStore,
        tasks: TaskStore,
        notifier: AlertNotifier,
    ) : this(store, forecasts, settings, alerts, tasks, notifier, ZoneId.systemDefault(), System::currentTimeMillis)

    /** Returns the alerts that were fired. */
    suspend operator fun invoke(): List<WeatherAlert> {
        if (!settings.alertsEnabled()) return emptyList()
        val now = nowMillis()
        val recent = store.getBetween(now - Thresholds.ALERT_LOOKBACK_MS, now)
        val newest = recent.maxByOrNull { it.timestamp } ?: return emptyList()
        if (now - newest.timestamp > Thresholds.MAX_SNAPSHOT_GAP_MS) return emptyList()

        val forecast = forecasts.getForecast(newest.latitude, newest.longitude).getOrNull().orEmpty()
        val enabled = AlertType.entries.filter { settings.isTypeEnabled(it) }.toSet()
        val lastSent = buildMap {
            AlertType.entries.forEach { type -> settings.lastSentAt(type)?.let { put(type, it) } }
        }
        val due = AlertCooldown
            .filter(AlertEngine.evaluate(recent, forecast, now).filter { it in enabled }, lastSent, now, settings.cooldownMs())

        return due.map { type ->
            fire(type, valueFor(type, recent, forecast, newest, now), forecast, now).also { settings.setLastSentAt(type, now) }
        }
    }

    /**
     * Debug only: fires an alert of [type] with made-up numbers, ignoring the conditions and the cooldown,
     * so the notification and the linked tasks can be checked at any time.
     */
    suspend fun fireTest(type: AlertType): WeatherAlert {
        val now = nowMillis()
        val nextHour = HourlyForecast(now + 60 * 60 * 1000L, 80, 1.0, 30.0)
        val value = when (type) {
            AlertType.RAIN_SOON -> 80.0
            AlertType.RAIN_NOW -> 1.0
            AlertType.HEAT -> 40.0
            AlertType.STRONG_SUN -> 45.0
            AlertType.PRESSURE_DROP -> 4.0
        }
        return fire(type, value, listOf(nextHour), now)
    }

    private suspend fun fire(type: AlertType, value: Double?, forecast: List<HourlyForecast>, now: Long): WeatherAlert {
        val hourText = AlertEngine.rainSoonHour(forecast, now)?.let { AlertMessages.hourText(it.time, zone) }
        val alert = WeatherAlert(
            type = type,
            time = now,
            message = AlertMessages.headline(type, value, hourText),
            value = value,
        )
        val saved = alert.copy(id = alerts.insert(alert))

        val result = TaskTriggerEngine.onAlertFired(type, tasks.all(), now)
        result.toUpdate.forEach { tasks.update(it) }
        notifier.show(saved, result.toShow, settings.useFahrenheit())
        return saved
    }

    /** The number behind an alert: probability, rain, feels-like, minutes in the sun, or hPa fallen. */
    private fun valueFor(
        type: AlertType,
        recent: List<Snapshot>,
        forecast: List<HourlyForecast>,
        newest: Snapshot,
        now: Long,
    ): Double? = when (type) {
        AlertType.RAIN_SOON -> AlertEngine.rainSoonHour(forecast, now)?.precipProbability?.toDouble()
        AlertType.RAIN_NOW -> newest.rainMm
        AlertType.HEAT -> newest.feelsLike
        AlertType.STRONG_SUN -> AlertEngine.minutesInStrongSun(recent.sortedBy { it.timestamp })
        AlertType.PRESSURE_DROP ->
            PressureTrend.dropAmount(recent.mapNotNull { s -> s.pressure?.let { s.timestamp to it } }).toDouble()
    }
}
