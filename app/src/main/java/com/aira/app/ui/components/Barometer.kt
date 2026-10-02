package com.aira.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** The phone's barometer: whether it has one, and the newest reading in hPa (null until the first one comes). */
data class BarometerReading(val available: Boolean, val pressureHpa: Float?)

/** Listens to the pressure sensor while the screen is shown, for the live altitude. */
@Composable
fun rememberBarometer(): BarometerReading {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) }
        ?: return BarometerReading(available = false, pressureHpa = null)
    var pressure by remember { mutableStateOf<Float?>(null) }
    LifecycleResumeEffect(sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                pressure = event.values[0]
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onPauseOrDispose { sensorManager.unregisterListener(listener) }
    }
    return BarometerReading(available = true, pressureHpa = pressure)
}
