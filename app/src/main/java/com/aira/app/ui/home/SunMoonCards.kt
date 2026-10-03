package com.aira.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.app.R
import com.aira.app.domain.engine.MoonInfo
import com.aira.app.domain.engine.MoonPhase
import com.aira.app.domain.engine.SkyEvents
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.SectionLabel
import com.aira.app.ui.theme.SunAmber
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun timeText(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

// ---- Sun ----

/**
 * The sun's path today: a wave from sunrise (left) to sunset (right) over a dashed horizon. The part already
 * travelled is amber, the rest faint, and a glowing sun moves along it to where it is now when the card appears.
 */
@Composable
fun SunCard(sunrise: Long, sunset: Long) {
    val target = SkyEvents.sunProgress(System.currentTimeMillis(), sunrise, sunset) ?: return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) { progress.animateTo(target, tween(1600, easing = FastOutSlowInEasing)) }
    val glow = rememberInfiniteTransition(label = "sunGlow")
        .animateFloat(0.25f, 0.5f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "glow")
    val amber = SunAmber
    val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val horizon = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    AiraCard {
        SectionLabel(stringResource(R.string.sun_title))
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val horizonY = size.height * 0.72f
            val amplitude = size.height * 0.6f
            drawLine(horizon, Offset(0f, horizonY), Offset(size.width, horizonY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            // The wave runs a little past sunrise and sunset so it dips below the horizon at both ends.
            fun point(t: Float) = Offset(size.width * (0.08f + 0.84f * t), horizonY - amplitude * sin(PI * t).toFloat())
            val path = Path().apply {
                moveTo(0f, horizonY + amplitude * 0.25f)
                for (i in -10..110) {
                    val t = i / 100f
                    val p = point(t)
                    lineTo(p.x, p.y)
                }
                lineTo(size.width, horizonY + amplitude * 0.25f)
            }
            val split = point(progress.value).x
            clipRect(right = split) { drawPath(path, amber, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
            clipRect(left = split) { drawPath(path, faint, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
            val sun = point(progress.value)
            drawCircle(Brush.radialGradient(listOf(amber.copy(alpha = glow.value), Color.Transparent), sun, 26.dp.toPx()), 26.dp.toPx(), sun)
            drawCircle(amber, 9.dp.toPx(), sun)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TimeLabel(stringResource(R.string.sun_rise), timeText(sunrise), Alignment.Start)
            TimeLabel(stringResource(R.string.sun_set), timeText(sunset), Alignment.End)
        }
    }
}

@Composable
private fun TimeLabel(label: String, time: String, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ---- Moon ----

/** Today's moon: a drawing of its phase with a soft glow, the phase name, and about when it rises and sets. */
@Composable
fun MoonCard(sunrise: Long?, sunset: Long?) {
    val now = System.currentTimeMillis()
    val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val moon = remember(sunrise, sunset) { SkyEvents.moon(now, sunrise, sunset, dayStart) }
    val glow = rememberInfiniteTransition(label = "moonGlow")
        .animateFloat(0.10f, 0.28f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "glow")

    AiraCard {
        SectionLabel(stringResource(R.string.moon_title))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(modifier = Modifier.size(104.dp)) { drawMoon(moon, glow.value) }
                Text(stringResource(phaseName(moon.phase)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                moon.rise?.let { TimeLabel(stringResource(R.string.moon_rise), timeText(it), Alignment.Start) }
                moon.set?.let { TimeLabel(stringResource(R.string.moon_set), timeText(it), Alignment.Start) }
                Text(
                    stringResource(R.string.moon_lit, (moon.illumination * 100).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The moon disc: a dim full circle, then the lit part on top. The lit part is half the disc on the lit side plus
 * (or minus) a half-ellipse whose width follows cos(age angle): a thin crescent, a half at the quarters, nearly
 * full near full moon. A waning moon is the same shape mirrored.
 */
private fun DrawScope.drawMoon(moon: MoonInfo, glow: Float) {
    val radius = size.minDimension / 2 * 0.82f
    val c = center
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = glow), Color.Transparent), c, radius * 1.25f), radius * 1.25f, c)
    drawCircle(Color(0xFF3A4458), radius, c)

    val fraction = moon.ageDays / SkyEvents.SYNODIC_MONTH_DAYS
    val shapeFraction = if (moon.waxing) fraction else 1 - fraction
    val k = cos(2 * PI * shapeFraction).toFloat()
    val disc = Rect(c, radius)
    val ellipse = Rect(c.x - radius * abs(k), c.y - radius, c.x + radius * abs(k), c.y + radius)
    val lit = Path().apply {
        arcTo(disc, -90f, 180f, forceMoveTo = true)
        // Crescent (k > 0): the edge curves back on the lit side; gibbous (k < 0): it bulges into the dark side.
        arcTo(ellipse, 90f, if (k > 0) -180f else 180f, forceMoveTo = false)
        close()
    }
    scale(if (moon.waxing) 1f else -1f, 1f, pivot = c) {
        drawPath(lit, Color(0xFFF1F3F8))
    }
    // A few soft craters.
    val crater = Color(0xFF8A93A6).copy(alpha = 0.22f)
    drawCircle(crater, radius * 0.16f, Offset(c.x - radius * 0.3f, c.y - radius * 0.25f))
    drawCircle(crater, radius * 0.11f, Offset(c.x + radius * 0.28f, c.y + radius * 0.3f))
    drawCircle(crater, radius * 0.08f, Offset(c.x + radius * 0.05f, c.y - radius * 0.5f))
}

private fun phaseName(phase: MoonPhase): Int = when (phase) {
    MoonPhase.NEW -> R.string.moon_new
    MoonPhase.WAXING_CRESCENT -> R.string.moon_waxing_crescent
    MoonPhase.FIRST_QUARTER -> R.string.moon_first_quarter
    MoonPhase.WAXING_GIBBOUS -> R.string.moon_waxing_gibbous
    MoonPhase.FULL -> R.string.moon_full
    MoonPhase.WANING_GIBBOUS -> R.string.moon_waning_gibbous
    MoonPhase.LAST_QUARTER -> R.string.moon_last_quarter
    MoonPhase.WANING_CRESCENT -> R.string.moon_waning_crescent
}
