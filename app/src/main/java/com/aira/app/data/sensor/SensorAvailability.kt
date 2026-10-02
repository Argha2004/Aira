package com.aira.app.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Which of the sensors Aira uses exist on this phone. */
data class SensorStatus(
    val light: Boolean,
    val proximity: Boolean,
    val stepCounter: Boolean,
    val accelerometer: Boolean,
    val pressure: Boolean,
)

@Singleton
class SensorAvailability @Inject constructor(@ApplicationContext context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun isAvailable(sensorType: Int): Boolean = sensorManager?.getDefaultSensor(sensorType) != null

    fun status() = SensorStatus(
        light = isAvailable(Sensor.TYPE_LIGHT),
        proximity = isAvailable(Sensor.TYPE_PROXIMITY),
        stepCounter = isAvailable(Sensor.TYPE_STEP_COUNTER),
        accelerometer = isAvailable(Sensor.TYPE_ACCELEROMETER),
        pressure = isAvailable(Sensor.TYPE_PRESSURE),
    )
}
