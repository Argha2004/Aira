package com.aira.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aira.app.domain.engine.Heading
import kotlin.math.cos
import kotlin.math.sin

/**
 * The phone's compass heading in degrees (0 = north), smoothed, or null when the phone has no rotation-vector
 * sensor (no magnetometer). The sensor only runs while the screen is visible.
 */
@Composable
fun rememberHeading(): Float? {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } ?: return null
    var heading by remember { mutableStateOf<Float?>(null) }
    LifecycleResumeEffect(sensor) {
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                heading = Heading.smooth(heading, azimuth)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose { sensorManager.unregisterListener(listener) }
    }
    return heading
}

/**
 * A compass dial that turns with the phone: ticks and N/E/S/W rotate so north stays north, the red needle at the
 * top is where the phone points. [windFrom] (degrees) adds a small blue marker for where the wind comes from.
 */
@Composable
fun CompassDial(heading: Float, modifier: Modifier = Modifier, size: Dp = 120.dp, windFrom: Int? = null) {
    val ring = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val wind = MaterialTheme.colorScheme.primary
    val needle = Color(0xFFE5484D)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2
            drawCircle(ring, radius - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            rotate(-heading) {
                for (degree in 0 until 360 step 15) {
                    val long = degree % 90 == 0
                    val angle = Math.toRadians(degree.toDouble() - 90)
                    val outer = radius - 4.dp.toPx()
                    val inner = outer - (if (long) 10.dp else 5.dp).toPx()
                    drawLine(
                        tick.copy(alpha = if (long) 1f else 0.5f),
                        Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                        Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                        strokeWidth = (if (long) 2.dp else 1.dp).toPx(),
                    )
                }
                if (windFrom != null) {
                    val angle = Math.toRadians(windFrom.toDouble() - 90)
                    val r = radius - 22.dp.toPx()
                    drawCircle(wind, 4.dp.toPx(), Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat()))
                }
            }
            // The needle: where the top of the phone points.
            val path = Path().apply {
                moveTo(center.x, center.y - radius + 14.dp.toPx())
                lineTo(center.x - 6.dp.toPx(), center.y)
                lineTo(center.x + 6.dp.toPx(), center.y)
                close()
            }
            drawPath(path, needle)
            drawCircle(tick, 3.dp.toPx(), center)
        }
        // The letters turn with the dial but stay upright.
        listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (letter, degrees) ->
            val angle = Math.toRadians((degrees - heading).toDouble() - 90)
            val r = size.value / 2 - 28
            Text(
                letter,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (letter == "N") FontWeight.Bold else FontWeight.Normal,
                color = if (letter == "N") needle else tick,
                modifier = Modifier.align(Alignment.Center).offset((r * cos(angle)).toFloat().dp, (r * sin(angle)).toFloat().dp),
            )
        }
    }
}
