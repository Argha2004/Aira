package com.aira.app.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [SensorSubscriber] backed by the real SensorManager. */
@Singleton
class AndroidSensorSubscriber @Inject constructor(
    @ApplicationContext context: Context,
) : SensorSubscriber {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private fun typeOf(kind: SensorKind) = when (kind) {
        SensorKind.LIGHT -> Sensor.TYPE_LIGHT
        SensorKind.PROXIMITY -> Sensor.TYPE_PROXIMITY
        SensorKind.STEP_COUNTER -> Sensor.TYPE_STEP_COUNTER
        SensorKind.PRESSURE -> Sensor.TYPE_PRESSURE
        SensorKind.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
    }

    private fun sensorFor(kind: SensorKind): Sensor? = sensorManager?.getDefaultSensor(typeOf(kind))

    override fun maximumRange(kind: SensorKind): Float? = sensorFor(kind)?.maximumRange

    override fun subscribe(kind: SensorKind, onValues: (FloatArray) -> Unit): Subscription? {
        val manager = sensorManager ?: return null
        val sensor = sensorFor(kind) ?: return null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = onValues(event.values)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // The accelerometer is sampled quickly for a few seconds; the others report rarely.
        val delay = if (kind == SensorKind.ACCELEROMETER) SensorManager.SENSOR_DELAY_GAME else SensorManager.SENSOR_DELAY_NORMAL
        val registered = try {
            manager.registerListener(listener, sensor, delay)
        } catch (e: SecurityException) {
            false // e.g. the step counter without the activity-recognition permission
        }
        return if (registered) Subscription { manager.unregisterListener(listener) } else null
    }
}
