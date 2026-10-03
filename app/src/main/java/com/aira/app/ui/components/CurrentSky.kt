package com.aira.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The weather the sky behind the tabs is drawn from: an Open-Meteo weather code and day or night. */
data class SkyCondition(val weatherCode: Int, val isDay: Boolean)

/**
 * The weather Home last loaded, shared with every tab so they all show the same sky. Null until the first
 * weather arrives (the tabs then use the plain background).
 */
@Singleton
class CurrentSky @Inject constructor() {
    private val condition = MutableStateFlow<SkyCondition?>(null)
    val current: StateFlow<SkyCondition?> = condition.asStateFlow()

    fun update(value: SkyCondition) {
        condition.value = value
    }
}

/**
 * Room to keep free at the bottom of a tab's scrolling content: the floating bottom bar (and the phone's navigation
 * bar). Pages are full screen and scroll behind the bar; their last item stops above it.
 */
val LocalBottomInset = androidx.compose.runtime.compositionLocalOf { androidx.compose.ui.unit.Dp(0f) }

/** True on a page drawn on the sky: text placed straight on the page (not in a card) is white. */
val LocalOnSky = staticCompositionLocalOf { false }

/** Colour for text placed straight on a page: white on the sky, the normal text colour otherwise. */
@Composable
fun pageTextColor(): Color = if (LocalOnSky.current) Color.White else MaterialTheme.colorScheme.onSurface

/** Colour for grey text placed straight on a page: soft white on the sky, the normal grey otherwise. */
@Composable
fun pageMutedColor(): Color = if (LocalOnSky.current) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

/** The app's normal colours, kept while a tab is drawn with the glass colours (for dialogs and sheets). */
val LocalPlainColors = staticCompositionLocalOf<ColorScheme?> { null }

private val GlassInk = Color(0xFF14233A)

/**
 * The colours of a page on the sky: every surface is see-through white, so all cards, chips and bars are glass,
 * and text and icons are white. Filled buttons become white with dark text.
 */
fun glassColors(base: ColorScheme): ColorScheme = base.copy(
    background = Color.Transparent,
    onBackground = Color.White,
    surface = Color.White.copy(alpha = 0.16f),
    onSurface = Color.White,
    surfaceVariant = Color.White.copy(alpha = 0.12f),
    onSurfaceVariant = Color.White.copy(alpha = 0.80f),
    surfaceContainer = Color.White.copy(alpha = 0.16f),
    surfaceContainerLow = Color.White.copy(alpha = 0.12f),
    surfaceContainerHigh = Color.White.copy(alpha = 0.20f),
    surfaceContainerHighest = Color.White.copy(alpha = 0.24f),
    outline = Color.White.copy(alpha = 0.55f),
    outlineVariant = Color.White.copy(alpha = 0.28f),
    primary = Color.White,
    onPrimary = GlassInk,
    primaryContainer = Color.White.copy(alpha = 0.22f),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color.White.copy(alpha = 0.22f),
    onSecondaryContainer = Color.White,
)

/** The frosted look: a see-through white fill, brighter at the top left, with a thin light edge. */
fun Modifier.glassSurface(shape: Shape): Modifier = clip(shape)
    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.09f))))
    .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f)))), shape)

/** Draws [content] (a dialog or a sheet) with the app's normal colours, even on a glass page. */
@Composable
fun PlainTheme(content: @Composable () -> Unit) {
    val plain = LocalPlainColors.current ?: return content()
    MaterialTheme(colorScheme = plain, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(LocalOnSky provides false, LocalContentColor provides plain.onSurface, content = content)
    }
}
