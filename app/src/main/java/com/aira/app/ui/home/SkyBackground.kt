package com.aira.app.ui.home

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.aira.app.ui.components.glassSurface
import androidx.core.view.WindowCompat

/**
 * The sky behind the top of Home: two colours for the gradient and one for the soft glow (the sun, the moon, or
 * the light in the clouds). Picked from the weather code, day or night, and the hour (dawn and dusk are warmer).
 */
@Immutable
data class SkyPalette(val top: Color, val bottom: Color, val glow: Color)

private val ClearDay = SkyPalette(Color(0xFF2F80D8), Color(0xFF64B5F6), Color(0xFFFFD36E))
private val ClearNight = SkyPalette(Color(0xFF0E1A3D), Color(0xFF2B3D7A), Color(0xFF9FB4FF))
private val Golden = SkyPalette(Color(0xFFF2785C), Color(0xFF7E57C2), Color(0xFFFFC56E))
private val CloudyDay = SkyPalette(Color(0xFF557394), Color(0xFF8BA4BE), Color(0xFFE3ECF5))
private val CloudyNight = SkyPalette(Color(0xFF1F2939), Color(0xFF3B4960), Color(0xFF8A9BB8))
private val Rain = SkyPalette(Color(0xFF26415F), Color(0xFF4D6D8E), Color(0xFF7FB3E6))
private val Storm = SkyPalette(Color(0xFF221D38), Color(0xFF4A3D6E), Color(0xFFB39DDB))
private val Snow = SkyPalette(Color(0xFF5F81A6), Color(0xFF9DB8D4), Color(0xFFFFFFFF))

/** The sky for an Open-Meteo weather [code]; [hour] is the local hour (0–23). */
fun skyPalette(code: Int, isDay: Boolean, hour: Int): SkyPalette = when (code) {
    in 95..99 -> Storm
    in 71..77, 85, 86 -> Snow
    in 51..67, in 80..82 -> Rain
    1, 2, 3, 45, 48 -> if (isDay) CloudyDay else CloudyNight
    else -> when {
        !isDay -> ClearNight
        hour in 5..7 || hour in 17..19 -> Golden
        else -> ClearDay
    }
}

/**
 * Fills the whole screen with the sky: the gradient from top to bottom, a soft glow top right (sun, moon or cloud
 * light) and a fainter one lower left. Colours change smoothly with the weather.
 */
@Composable
fun Modifier.skyBackground(palette: SkyPalette): Modifier {
    val top by animateColorAsState(palette.top, tween(800), label = "skyTop")
    val bottom by animateColorAsState(palette.bottom, tween(800), label = "skyBottom")
    val glow by animateColorAsState(palette.glow, tween(800), label = "skyGlow")
    return drawBehind {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        val radius = size.width * 0.75f
        val sun = Offset(size.width * 0.85f, size.height * 0.12f)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.65f), glow.copy(alpha = 0f)), sun, radius), radius, sun)
        val haze = Offset(size.width * 0.1f, size.height * 0.55f)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.22f), glow.copy(alpha = 0f)), haze, radius), radius, haze)
    }
}

/** White status bar icons while Home's sky is shown; the theme's own setting comes back when Home leaves. */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val before = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
}

/**
 * A frosted-glass card: a see-through white fill, brighter at the top left, with a thin light edge. The text
 * inside is white, so it reads well on every sky.
 */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val white = Color.White
    val glassColors = MaterialTheme.colorScheme.copy(
        onSurface = white,
        onSurfaceVariant = white.copy(alpha = 0.82f),
        primary = white,
    )
    MaterialTheme(colorScheme = glassColors, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(LocalContentColor provides white) {
            Column(
                modifier = modifier.fillMaxWidth().glassSurface(shape).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}
