package com.aira.app.data.sensor

import com.aira.app.domain.usecase.SensorSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reads each sensor once on demand. Every function returns null when the sensor is
 * missing, the permission is denied, or no value arrives in time, so it never crashes.
 * A listener is always unregistered when the read ends, times out or is cancelled (see [values]).
 * This class has no Android imports: the phone is reached through [SensorSubscriber].
 */
@Singleton
class SensorReader @Inject constructor(private val subscriber: SensorSubscriber) : SensorSource {

    /** Ambient light in lux. */
    override suspend fun readLux(): Float? = readFirst(SensorKind.LIGHT)?.get(0)

    /** True when the proximity sensor is covered (phone in pocket or bag). */
    override suspend fun readInPocket(): Boolean? {
        val maxRange = subscriber.maximumRange(SensorKind.PROXIMITY) ?: return null
        val distance = readFirst(SensorKind.PROXIMITY)?.get(0) ?: return null
        return distance < maxRange
    }

    /** Total steps since the phone was last restarted. */
    override suspend fun readStepCounter(): Long? = readFirst(SensorKind.STEP_COUNTER)?.get(0)?.toLong()

    /** Air pressure in hPa, or null if the phone has no barometer. */
    override suspend fun readPressure(): Float? = readFirst(SensorKind.PRESSURE)?.get(0)

    /**
     * Average size of the acceleration with gravity removed, measured for [durationMs].
     * Near 0 = phone lying still; larger = the phone is being moved or carried while walking.
     */
    suspend fun sampleMovement(durationMs: Long = 3000): Float? {
        var sum = 0.0
        var count = 0
        withTimeoutOrNull(durationMs) {
            values(SensorKind.ACCELEROMETER).collect { v ->
                sum += abs(sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) - GRAVITY)
                count++
            }
        }
        return if (count == 0) null else (sum / count).toFloat()
    }

    private suspend fun readFirst(kind: SensorKind): FloatArray? =
        withTimeoutOrNull(timeoutFor(kind)) { values(kind).firstOrNull() }

    /**
     * The step counter only reports when it has something to say (or after a batching delay), so it gets more
     * time than the other sensors. A missing total would also leave the next snapshot without a step count.
     */
    private fun timeoutFor(kind: SensorKind): Long =
        if (kind == SensorKind.STEP_COUNTER) STEP_COUNTER_TIMEOUT_MS else TIMEOUT_MS

    /** Emits sensor values; the listener is unregistered as soon as the collector stops. */
    private fun values(kind: SensorKind): Flow<FloatArray> = callbackFlow {
        val subscription = subscriber.subscribe(kind) { trySend(it.clone()) }
        if (subscription == null) close() // sensor missing: the flow ends without values
        awaitClose { subscription?.cancel() }
    }

    private companion object {
        const val TIMEOUT_MS = 3000L
        const val STEP_COUNTER_TIMEOUT_MS = 8000L
        const val GRAVITY = 9.80665f
    }
}
