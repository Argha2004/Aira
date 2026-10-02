package com.aira.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val SLIDE_MS = 300

/**
 * The slide used inside screens (onboarding steps, days, months, filters, Week/Month/Year): going forward the new
 * content comes in from the right and the old leaves to the left; going back it is the other way round.
 */
fun <S> AnimatedContentTransitionScope<S>.slideBetween(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (
        slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { width -> direction * width / 3 } +
            fadeIn(tween(SLIDE_MS))
        ) togetherWith (
        slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { width -> -direction * width / 3 } +
            fadeOut(tween(SLIDE_MS))
        )
}

/**
 * Shows [content] for [target] and slides to the new content when [key] of the target changes (e.g. another day
 * or another filter chip). [forward] says, from the old target to the new one, whether it is a step forward.
 * Changes that keep the same key (new data for the same day) update in place without sliding.
 */
@Composable
fun <T> SlideContent(
    target: T,
    forward: (from: T, to: T) -> Boolean,
    modifier: Modifier = Modifier,
    key: (T) -> Any? = { it },
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        contentKey = key,
        transitionSpec = { slideBetween(forward(initialState, targetState)) },
        label = "slide",
    ) { content(it) }
}
