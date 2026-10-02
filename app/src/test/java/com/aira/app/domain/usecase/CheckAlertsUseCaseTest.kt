package com.aira.app.domain.usecase

import com.aira.app.domain.engine.snap
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Small pretend versions of the app's stores, shared by the use case tests.

internal class FakeSnapshotStore(val snapshots: MutableList<Snapshot> = mutableListOf()) : SnapshotStore {
    override suspend fun getLatest() = snapshots.maxByOrNull { it.timestamp }
    override suspend fun save(snapshot: Snapshot) = 1L.also { snapshots += snapshot }
    override suspend fun getBetween(from: Long, to: Long) = snapshots.filter { it.timestamp in from..to }
}

internal class FakeAlertStore : AlertStore {
    val alerts = mutableListOf<WeatherAlert>()
    override suspend fun insert(alert: WeatherAlert): Long {
        val id = alerts.size + 1L
        alerts += alert.copy(id = id)
        return id
    }
    override suspend fun get(id: Long) = alerts.firstOrNull { it.id == id }
    override suspend fun dismiss(id: Long) {
        alerts.replaceAll { if (it.id == id) it.copy(dismissed = true) else it }
    }
}

internal class FakeTaskStore(vararg initial: WeatherTask) : TaskStore {
    val tasks = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    override suspend fun insert(task: WeatherTask): Long {
        val id = nextId++
        tasks += task.copy(id = id)
        return id
    }
    override suspend fun update(task: WeatherTask) {
        tasks.replaceAll { if (it.id == task.id) task else it }
    }
    override suspend fun delete(id: Long) {
        tasks.removeAll { it.id == id }
    }
    override suspend fun get(id: Long) = tasks.firstOrNull { it.id == id }
    override suspend fun all() = tasks.toList()
}

class CheckAlertsUseCaseTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private var now = LocalDateTime.of(2026, 9, 29, 10, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

    private class FakeForecast(var result: Result<List<HourlyForecast>> = Result.success(emptyList())) : ForecastSource {
        override suspend fun getForecast(latitude: Double, longitude: Double) = result
    }

    private class FakeSettings : AlertSettingsSource {
        var enabled = true
        var fahrenheit = false
        var cooldown = 3 * 60 * 60 * 1000L
        val typeOff = mutableSetOf<AlertType>()
        val lastSent = mutableMapOf<AlertType, Long>()
        override suspend fun alertsEnabled() = enabled
        override suspend fun isTypeEnabled(type: AlertType) = type !in typeOff
        override suspend fun cooldownMs() = cooldown
        override suspend fun useFahrenheit() = fahrenheit
        override suspend fun lastSentAt(type: AlertType) = lastSent[type]
        override suspend fun setLastSentAt(type: AlertType, millis: Long) {
            lastSent[type] = millis
        }
    }

    private class FakeNotifier : AlertNotifier {
        data class Shown(val alert: WeatherAlert, val tasks: List<WeatherTask>, val fahrenheit: Boolean)
        val shown = mutableListOf<Shown>()
        override fun show(alert: WeatherAlert, tasks: List<WeatherTask>, useFahrenheit: Boolean) {
            shown += Shown(alert, tasks, useFahrenheit)
        }
    }

    private val store = FakeSnapshotStore()
    private val forecast = FakeForecast()
    private val settings = FakeSettings()
    private val alerts = FakeAlertStore()
    private val tasks = FakeTaskStore()
    private val notifier = FakeNotifier()

    private fun useCase() = CheckAlertsUseCase(store, forecast, settings, alerts, tasks, notifier, ZoneOffset.UTC) { now }

    private fun snapshot(minutesAgo: Int, place: Place = Place.INDOOR, feels: Double = 30.0, lux: Float? = 200f,
                         pending: Boolean = false) =
        snap(0, place, lux = lux, feels = feels, pending = pending, atMillis = now - minutesAgo * minute)

    private fun hotNow() = snapshot(5, Place.OUTDOOR, feels = 40.0, lux = 5000f)

    private fun forecastHour(hour: Int, rain: Int) = HourlyForecast(
        time = LocalDateTime.of(2026, 9, 29, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        precipProbability = rain, precipitationMm = 0.0, feelsLikeC = 30.0,
    )

    // ---- firing ----

    @Test
    fun `a heat alert is saved, shown and its time remembered`() = runTest {
        store.snapshots += hotNow()
        val fired = useCase()()
        val alert = fired.single()
        assertEquals(AlertType.HEAT, alert.type)
        assertEquals("Feels like 40 °C", alert.message)
        assertEquals(40.0, alert.value!!, 0.0)
        assertEquals(alert, alerts.alerts.single())
        assertEquals(alert, notifier.shown.single().alert)
        assertEquals(now, settings.lastSent[AlertType.HEAT])
    }

    @Test
    fun `rain soon comes from the forecast and names the hour`() = runTest {
        store.snapshots += snapshot(5)
        forecast.result = Result.success(listOf(forecastHour(10, 20), forecastHour(12, 80)))
        val alert = useCase()().single()
        assertEquals(AlertType.RAIN_SOON, alert.type)
        assertEquals("Rain likely around 12 PM", alert.message)
        assertEquals(80.0, alert.value!!, 0.0)
    }

    @Test
    fun `without a forecast there is no rain soon, but other alerts still fire`() = runTest {
        store.snapshots += hotNow()
        forecast.result = Result.failure(java.io.IOException("offline"))
        assertEquals(listOf(AlertType.HEAT), useCase()().map { it.type })
    }

    @Test
    fun `two alerts at once are both fired`() = runTest {
        store.snapshots += snapshot(0, Place.OUTDOOR, feels = 40.0, lux = 5000f).copy(rainMm = 2.0)
        assertTrue(useCase()().map { it.type }.containsAll(listOf(AlertType.HEAT, AlertType.RAIN_NOW)))
        assertEquals(2, settings.lastSent.size)
    }

    @Test
    fun `the temperature unit is passed on to the notification`() = runTest {
        settings.fahrenheit = true
        store.snapshots += hotNow()
        useCase()()
        assertTrue(notifier.shown.single().fahrenheit)
    }

    // ---- cooldown and switches ----

    @Test
    fun `the same alert is not fired again within 3 hours`() = runTest {
        store.snapshots += hotNow()
        useCase()()
        now += 2 * hour
        store.snapshots += snapshot(5, Place.OUTDOOR, feels = 41.0, lux = 5000f)
        assertTrue(useCase()().isEmpty())
        assertEquals(1, alerts.alerts.size)
    }

    @Test
    fun `it can fire again after 3 hours`() = runTest {
        store.snapshots += hotNow()
        useCase()()
        now += 3 * hour
        store.snapshots += snapshot(5, Place.OUTDOOR, feels = 41.0, lux = 5000f)
        assertEquals(1, useCase()().size)
        assertEquals(2, alerts.alerts.size)
    }

    @Test
    fun `a shorter cooldown of one hour is respected`() = runTest {
        settings.cooldown = hour
        store.snapshots += hotNow()
        useCase()()
        now += hour
        store.snapshots += snapshot(5, Place.OUTDOOR, feels = 41.0, lux = 5000f)
        assertEquals(1, useCase()().size)
    }

    @Test
    fun `a longer cooldown of six hours is respected`() = runTest {
        settings.cooldown = 6 * hour
        store.snapshots += hotNow()
        useCase()()
        now += 5 * hour
        store.snapshots += snapshot(5, Place.OUTDOOR, feels = 41.0, lux = 5000f)
        assertTrue(useCase()().isEmpty())
    }

    @Test
    fun `an alert type that is switched off is not fired`() = runTest {
        settings.typeOff += AlertType.HEAT
        store.snapshots += hotNow()
        assertTrue(useCase()().isEmpty())
        assertTrue(alerts.alerts.isEmpty())
    }

    @Test
    fun `other types still fire when one is switched off`() = runTest {
        settings.typeOff += AlertType.HEAT
        store.snapshots += snapshot(0, Place.OUTDOOR, feels = 40.0, lux = 5000f).copy(rainMm = 2.0)
        assertEquals(listOf(AlertType.RAIN_NOW), useCase()().map { it.type })
    }

    @Test
    fun `alerts turned off show nothing and save nothing`() = runTest {
        settings.enabled = false
        store.snapshots += hotNow()
        assertTrue(useCase()().isEmpty())
        assertTrue(alerts.alerts.isEmpty())
        assertTrue(notifier.shown.isEmpty())
        assertTrue(settings.lastSent.isEmpty())
    }

    @Test
    fun `no snapshots fire nothing`() = runTest { assertTrue(useCase()().isEmpty()) }

    @Test
    fun `an old newest snapshot is too stale to alert about`() = runTest {
        store.snapshots += snapshot(150, Place.OUTDOOR, feels = 45.0, lux = 5000f)
        assertTrue(useCase()().isEmpty())
    }

    // ---- tasks linked to the alert ----

    @Test
    fun `pending tasks linked to the alert are passed to the notification`() = runTest {
        tasks.tasks += WeatherTask(1, "Drink water", trigger = AlertType.HEAT)
        tasks.tasks += WeatherTask(2, "Bring clothes inside", trigger = AlertType.RAIN_SOON)
        store.snapshots += hotNow()
        useCase()()
        assertEquals(listOf("Drink water"), notifier.shown.single().tasks.map { it.title })
    }

    @Test
    fun `an every-time task that was done comes back and is saved as pending`() = runTest {
        tasks.tasks += WeatherTask(
            1, "Drink water", trigger = AlertType.HEAT, repeat = TaskRepeat.EVERY_TIME,
            status = TaskStatus.DONE, completedAt = 5,
        )
        store.snapshots += hotNow()
        useCase()()
        assertEquals(TaskStatus.PENDING, tasks.tasks.single().status)
        assertNull(tasks.tasks.single().completedAt)
        assertEquals(1, notifier.shown.single().tasks.size)
    }

    @Test
    fun `a done once-only task is not brought back`() = runTest {
        tasks.tasks += WeatherTask(1, "Drink water", trigger = AlertType.HEAT, status = TaskStatus.DONE, completedAt = 5)
        store.snapshots += hotNow()
        useCase()()
        assertTrue(notifier.shown.single().tasks.isEmpty())
        assertEquals(TaskStatus.DONE, tasks.tasks.single().status)
    }

    // ---- debug test alerts ----

    @Test
    fun `a test alert ignores conditions and cooldown and leaves the cooldown alone`() = runTest {
        settings.lastSent[AlertType.HEAT] = now
        val alert = useCase().fireTest(AlertType.HEAT)
        assertEquals(AlertType.HEAT, alert.type)
        assertEquals(1, alerts.alerts.size)
        assertEquals(1, notifier.shown.size)
        assertEquals(now, settings.lastSent[AlertType.HEAT])
    }

    @Test
    fun `a test rain soon alert names the next hour`() = runTest {
        val alert = useCase().fireTest(AlertType.RAIN_SOON)
        assertEquals("Rain likely around 11 AM", alert.message)
    }

    @Test
    fun `a test alert also lists linked tasks`() = runTest {
        tasks.tasks += WeatherTask(1, "Close windows", trigger = AlertType.PRESSURE_DROP)
        useCase().fireTest(AlertType.PRESSURE_DROP)
        assertEquals(listOf("Close windows"), notifier.shown.single().tasks.map { it.title })
    }
}
