package com.aira.app.domain.usecase

import com.aira.app.domain.engine.ContextEngine
import com.aira.app.domain.engine.GeoMath
import com.aira.app.domain.engine.NightPause
import com.aira.app.domain.engine.Thresholds
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.Snapshot
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class NoLocationException : Exception("Location is unavailable. Allow location access and try again.")

/**
 * Takes one snapshot: reads location and sensors, gets the weather, works out place and
 * movement with the rules in the engine, and saves the result.
 *
 * If the weather cannot be fetched (offline) the snapshot is still saved with
 * `weatherPending = true`; a later job fills in the weather. Without a location nothing is saved.
 */
class TakeSnapshotUseCase(
    private val sensors: SensorSource,
    private val locations: LocationSource,
    private val weather: WeatherSource,
    private val store: SnapshotStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val nowMillis: () -> Long,
) {
    @Inject
    constructor(
        sensors: SensorSource,
        locations: LocationSource,
        weather: WeatherSource,
        store: SnapshotStore,
    ) : this(sensors, locations, weather, store, ZoneId.systemDefault(), System::currentTimeMillis)

    /** Returns the saved snapshot, or a failure (for example no location). Saves nothing on failure. */
    suspend operator fun invoke(): Result<Snapshot> = try {
        Result.success(take())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun take(): Snapshot = coroutineScope {
        val now = nowMillis()
        val lux = async { sensors.readLux() }
        val inPocket = async { sensors.readInPocket() }
        val totalSteps = async { sensors.readStepCounter() }
        val pressure = async { sensors.readPressure() }
        val location = async { locations.getCurrentLocation() }

        // A snapshot from long ago says nothing about movement since, so it is ignored.
        val previous = store.getLatest()?.takeIf { now - it.timestamp <= Thresholds.MAX_SNAPSHOT_GAP_MS }
        val point = location.await() ?: throw NoLocationException()
        val weatherNow = weather.getWeather(point.latitude, point.longitude).getOrNull()

        val steps = totalSteps.await()
        // If the previous snapshot has no step total (its step read timed out), use the last total we do know.
        val stepBaseline = previous?.totalSteps ?: lastKnownTotalSteps(now)
        val stepsDelta = ContextEngine.stepsSince(steps, stepBaseline)
        val distanceKm = previous?.let { GeoMath.distanceKm(GeoPoint(it.latitude, it.longitude), point) }
        val movement = ContextEngine.classifyMovement(stepsDelta, distanceKm)
        val luxValue = lux.await()
        val pocket = inPocket.await()
        val isDaytime = weatherNow?.isDay ?: NightPause.isDaytimeByClock(hourOf(now))
        val place = ContextEngine.classifyPlace(luxValue, pocket, isDaytime, previous?.place, movement)

        val snapshot = Snapshot(
            timestamp = now,
            latitude = point.latitude,
            longitude = point.longitude,
            // Offline: zeros are placeholders until the weather is filled in.
            temperature = weatherNow?.tempC ?: 0.0,
            feelsLike = weatherNow?.feelsLikeC ?: 0.0,
            humidity = weatherNow?.humidity ?: 0,
            rainMm = weatherNow?.rainMm ?: 0.0,
            uvIndex = weatherNow?.uvIndex ?: 0.0,
            windSpeed = weatherNow?.windKmh ?: 0.0,
            lux = luxValue,
            stepsDelta = stepsDelta,
            totalSteps = steps,
            pressure = pressure.await(),
            inPocket = pocket,
            place = place,
            movement = movement,
            weatherPending = weatherNow == null,
        )
        snapshot.copy(id = store.save(snapshot))
    }

    /** The step total of the newest snapshot of the last 2 hours that has one, or null. */
    private suspend fun lastKnownTotalSteps(now: Long): Long? =
        store.getBetween(now - Thresholds.MAX_SNAPSHOT_GAP_MS, now)
            .filter { it.totalSteps != null }
            .maxByOrNull { it.timestamp }
            ?.totalSteps

    private fun hourOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).hour
}
