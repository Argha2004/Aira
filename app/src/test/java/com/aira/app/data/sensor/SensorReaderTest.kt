package com.aira.app.data.sensor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The important promise of SensorReader: whatever happens (value, timeout, cancel, missing sensor),
 * no sensor listener is left registered, because that would drain the battery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorReaderTest {

    /** A pretend phone: counts how many listeners are registered and lets the test send values. */
    private class FakeSubscriber : SensorSubscriber {
        val available = SensorKind.entries.toMutableSet()
        var maxRange: Float? = 5f
        var active = 0
        var everSubscribed = 0
        private val listeners = mutableMapOf<SensorKind, MutableList<(FloatArray) -> Unit>>()

        override fun subscribe(kind: SensorKind, onValues: (FloatArray) -> Unit): Subscription? {
            if (kind !in available) return null
            active++
            everSubscribed++
            listeners.getOrPut(kind) { mutableListOf() }.add(onValues)
            return Subscription {
                active--
                listeners[kind]?.remove(onValues)
            }
        }

        override fun maximumRange(kind: SensorKind): Float? = if (kind in available) maxRange else null

        fun emit(kind: SensorKind, vararg values: Float) {
            listeners[kind]?.toList()?.forEach { it(values) }
        }
    }

    private val phone = FakeSubscriber()
    private val reader = SensorReader(phone)

    @Test
    fun `a value is returned and the listener is unregistered`() = runTest {
        val result = async { reader.readLux() }
        runCurrent()
        assertEquals(1, phone.active)
        phone.emit(SensorKind.LIGHT, 1234f)
        advanceUntilIdle()
        assertEquals(1234f, result.await())
        assertEquals(0, phone.active)
    }

    @Test
    fun `only the first value is used`() = runTest {
        val result = async { reader.readLux() }
        runCurrent()
        phone.emit(SensorKind.LIGHT, 10f)
        phone.emit(SensorKind.LIGHT, 99f)
        advanceUntilIdle()
        assertEquals(10f, result.await())
    }

    @Test
    fun `no value in 3 seconds gives null and unregisters the listener`() = runTest {
        val result = async { reader.readLux() }
        runCurrent()
        assertEquals(1, phone.active)
        advanceTimeBy(3_001)
        advanceUntilIdle()
        assertNull(result.await())
        assertEquals(0, phone.active)
    }

    @Test
    fun `cancelling while waiting unregisters the listener`() = runTest {
        val job = launch { reader.readLux() }
        runCurrent()
        assertEquals(1, phone.active)
        job.cancel()
        advanceUntilIdle()
        assertEquals(0, phone.active)
    }

    @Test
    fun `a missing sensor gives null and registers nothing`() = runTest {
        phone.available.remove(SensorKind.LIGHT)
        assertNull(reader.readLux())
        assertEquals(0, phone.everSubscribed)
        assertEquals(0, phone.active)
    }

    @Test
    fun `step counter and pressure are read`() = runTest {
        val steps = async { reader.readStepCounter() }
        val pressure = async { reader.readPressure() }
        runCurrent()
        phone.emit(SensorKind.STEP_COUNTER, 81_727f)
        phone.emit(SensorKind.PRESSURE, 1008.5f)
        advanceUntilIdle()
        assertEquals(81_727L, steps.await())
        assertEquals(1008.5f, pressure.await())
        assertEquals(0, phone.active)
    }

    @Test
    fun `the step counter is given more than 3 seconds`() = runTest {
        val steps = async { reader.readStepCounter() }
        runCurrent()
        advanceTimeBy(5_000) // a light sensor would have given up by now
        phone.emit(SensorKind.STEP_COUNTER, 81_800f)
        advanceUntilIdle()
        assertEquals(81_800L, steps.await())
        assertEquals(0, phone.active)
    }

    @Test
    fun `the step counter also gives up eventually and unregisters`() = runTest {
        val steps = async { reader.readStepCounter() }
        runCurrent()
        advanceTimeBy(8_001)
        advanceUntilIdle()
        assertNull(steps.await())
        assertEquals(0, phone.active)
    }

    @Test
    fun `a phone without a barometer gives null pressure`() = runTest {
        phone.available.remove(SensorKind.PRESSURE)
        assertNull(reader.readPressure())
    }

    // ---- proximity: in pocket ----

    @Test
    fun `covered proximity sensor means in pocket`() = runTest {
        val result = async { reader.readInPocket() }
        runCurrent()
        phone.emit(SensorKind.PROXIMITY, 0f)
        advanceUntilIdle()
        assertEquals(true, result.await())
    }

    @Test
    fun `proximity at the maximum range means not in pocket`() = runTest {
        val result = async { reader.readInPocket() }
        runCurrent()
        phone.emit(SensorKind.PROXIMITY, 5f)
        advanceUntilIdle()
        assertEquals(false, result.await())
    }

    @Test
    fun `no proximity sensor gives null`() = runTest {
        phone.available.remove(SensorKind.PROXIMITY)
        assertNull(reader.readInPocket())
    }

    @Test
    fun `unknown maximum range gives null`() = runTest {
        phone.maxRange = null
        assertNull(reader.readInPocket())
        assertEquals(0, phone.everSubscribed)
    }

    // ---- movement ----

    @Test
    fun `movement is the average acceleration without gravity`() = runTest {
        val result = async { reader.sampleMovement(1000) }
        runCurrent()
        phone.emit(SensorKind.ACCELEROMETER, 0f, 0f, 9.80665f) // lying still: 0
        phone.emit(SensorKind.ACCELEROMETER, 0f, 0f, 11.80665f) // 2 above gravity
        advanceTimeBy(1_001)
        advanceUntilIdle()
        assertEquals(1.0f, result.await()!!, 0.01f)
        assertEquals(0, phone.active)
    }

    @Test
    fun `movement with no samples is null and unregisters`() = runTest {
        val result = async { reader.sampleMovement(1000) }
        runCurrent()
        assertEquals(1, phone.active)
        advanceTimeBy(1_001)
        advanceUntilIdle()
        assertNull(result.await())
        assertEquals(0, phone.active)
    }

    @Test
    fun `movement without an accelerometer is null`() = runTest {
        phone.available.remove(SensorKind.ACCELEROMETER)
        assertNull(reader.sampleMovement(1000))
        assertEquals(0, phone.everSubscribed)
    }

    @Test
    fun `after several reads nothing is left registered`() = runTest {
        repeat(3) {
            val lux = async { reader.readLux() }
            runCurrent()
            phone.emit(SensorKind.LIGHT, 1f)
            advanceUntilIdle()
            lux.await()
        }
        val pressure = async { reader.readPressure() } // nothing arrives: this one times out
        advanceTimeBy(3_001)
        advanceUntilIdle()
        assertNull(pressure.await())
        assertEquals(4, phone.everSubscribed)
        assertEquals(0, phone.active)
    }
}
