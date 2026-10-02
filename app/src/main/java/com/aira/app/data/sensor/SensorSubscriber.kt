package com.aira.app.data.sensor

enum class SensorKind { LIGHT, PROXIMITY, STEP_COUNTER, PRESSURE, ACCELEROMETER }

/** A running listener. Calling [cancel] unregisters it from the phone. */
fun interface Subscription {
    fun cancel()
}

/**
 * The only place that touches the phone's SensorManager (see [AndroidSensorSubscriber]).
 * Keeping it behind this small interface lets [SensorReader] be tested on the JVM with a fake.
 */
interface SensorSubscriber {
    /** Starts listening. Returns null when the sensor is missing or cannot be registered. */
    fun subscribe(kind: SensorKind, onValues: (FloatArray) -> Unit): Subscription?

    /** The largest value the sensor reports, or null if the sensor is missing. */
    fun maximumRange(kind: SensorKind): Float?
}
