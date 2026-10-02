package com.aira.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aira.app.ui.theme.RainBlue
import com.aira.app.ui.theme.SunAmber

/** The scene the animated icon draws for a weather code. */
private enum class Scene { SUN, MOON, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

private fun sceneFor(code: Int, isDay: Boolean): Scene = when (code) {
    0, 1 -> if (isDay) Scene.SUN else Scene.MOON
    2 -> if (isDay) Scene.PARTLY else Scene.MOON
    3 -> Scene.CLOUDY
    45, 48 -> Scene.FOG
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Scene.RAIN
    71, 73, 75, 77, 85, 86 -> Scene.SNOW
    95, 96, 99 -> Scene.STORM
    else -> Scene.CLOUDY
}

/**
 * A small looping weather animation for the Home card: the sun slowly turns and glows, clouds drift,
 * rain and snow fall, lightning flashes, the moon gently floats. Built from Material icons, so it needs no
 * image files.
 */
@Composable
fun AnimatedWeatherIcon(code: Int, isDay: Boolean, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val loop = rememberInfiniteTransition(label = "weather")
    // 0 → 1 over 20 s, for the sun's slow turn.
    val turn by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "turn")
    // -1 → 1 and back, for drifting clouds, the floating moon and the sun's glow.
    val sway by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    // 0 → 1 over 1.2 s, for falling drops and flakes.
    val fall by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1_200, easing = LinearEasing)), label = "fall")

    val scene = sceneFor(code, isDay)
    val boxSize = size
    val cloudColor = Color(0xFFDCE6F5)
    Box(modifier = modifier.size(size)) {
        when (scene) {
            Scene.SUN -> Icon(
                Icons.Filled.WbSunny, null, tint = SunAmber,
                modifier = Modifier.size(size * 0.8f).align(Alignment.Center).graphicsLayer {
                    rotationZ = turn
                    val pulse = 1f + 0.05f * sway
                    scaleX = pulse
                    scaleY = pulse
                },
            )
            Scene.MOON -> Icon(
                Icons.Filled.NightsStay, null, tint = Color(0xFFFFE082),
                modifier = Modifier.size(size * 0.8f).align(Alignment.Center).graphicsLayer {
                    translationY = sway * 6.dp.toPx()
                    rotationZ = sway * 6f
                },
            )
            Scene.PARTLY -> {
                Icon(
                    Icons.Filled.WbSunny, null, tint = SunAmber,
                    modifier = Modifier.size(size * 0.62f).align(Alignment.TopEnd).graphicsLayer { rotationZ = turn },
                )
                Icon(
                    Icons.Filled.Cloud, null, tint = cloudColor,
                    modifier = Modifier.size(size * 0.7f).align(Alignment.BottomStart).graphicsLayer {
                        translationX = sway * 8.dp.toPx()
                    },
                )
            }
            Scene.CLOUDY -> {
                Icon(
                    Icons.Filled.Cloud, null, tint = cloudColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(size * 0.55f).align(Alignment.TopEnd).graphicsLayer { translationX = -sway * 6.dp.toPx() },
                )
                Icon(
                    Icons.Filled.Cloud, null, tint = cloudColor,
                    modifier = Modifier.size(size * 0.72f).align(Alignment.BottomStart).graphicsLayer { translationX = sway * 8.dp.toPx() },
                )
            }
            Scene.FOG -> Icon(
                Icons.Filled.Dehaze, null, tint = cloudColor,
                modifier = Modifier.size(size * 0.75f).align(Alignment.Center).graphicsLayer {
                    translationX = sway * 10.dp.toPx()
                    alpha = 0.75f + 0.25f * sway
                },
            )
            Scene.RAIN, Scene.SNOW, Scene.STORM -> {
                Icon(
                    Icons.Filled.Cloud, null, tint = cloudColor,
                    modifier = Modifier.size(size * 0.72f).align(Alignment.TopCenter).graphicsLayer { translationX = sway * 4.dp.toPx() },
                )
                if (scene == Scene.STORM) {
                    Icon(
                        Icons.Filled.Bolt, null, tint = SunAmber,
                        // Flashes on briefly once per drop cycle.
                        modifier = Modifier.size(size * 0.35f).align(Alignment.BottomCenter).graphicsLayer { alpha = if (fall < 0.15f) 1f else 0.25f },
                    )
                }
                // Three drops (or flakes), each a third of a cycle behind the one before.
                val drop = if (scene == Scene.SNOW) Icons.Filled.AcUnit else Icons.Filled.WaterDrop
                val tint = if (scene == Scene.SNOW) Color.White else RainBlue
                listOf(0.28f, 0.5f, 0.72f).forEachIndexed { index, x ->
                    val phase = (fall + index / 3f) % 1f
                    Icon(
                        drop, null, tint = tint,
                        modifier = Modifier.size(size * 0.14f).align(Alignment.TopStart).graphicsLayer {
                            // Inside graphicsLayer "size" is the layer's own size, so the box size is boxPx.
                            val boxPx = boxSize.toPx()
                            translationX = boxPx * (x - 0.07f)
                            translationY = boxPx * (0.55f + 0.35f * phase)
                            alpha = 1f - phase
                        },
                    )
                }
            }
        }
    }
}
