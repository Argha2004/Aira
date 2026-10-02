package com.aira.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Slide transitions between screens.
 * - Tab to tab: slides left or right, depending on whether the new tab is to the right or left in the bottom bar.
 * - Opening Settings or Sensor Status: slides in from the right; going back slides it out to the right.
 */
object Transitions {
    private const val DURATION_MS = 350
    private val spec = tween<androidx.compose.ui.unit.IntOffset>(DURATION_MS, easing = FastOutSlowInEasing)
    private val fade = tween<Float>(DURATION_MS)

    /** Position of a route in the bottom bar, or -1 for screens that are not tabs. */
    private fun tabIndex(route: String?): Int = Tab.entries.indexOfFirst { it.route == route }

    /** +1 when moving to the right (forward), -1 when moving to the left (back). */
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int {
        val from = tabIndex(initialState.destination.route)
        val to = tabIndex(targetState.destination.route)
        return when {
            initialState.destination.route == ONBOARDING_ROUTE -> 1 // finishing onboarding goes forward to Home
            from >= 0 && to >= 0 -> if (to > from) 1 else -1
            to < 0 -> 1 // opening a screen that is not a tab (Settings, Sensor Status)
            else -> -1 // coming back to a tab
        }
    }

    /** Pages opened from a tab (Settings, Sensor Status, Add location): they slide in over the full width. */
    private fun isSubPage(route: String?): Boolean = route in setOf(SETTINGS_ROUTE, SENSOR_STATUS_ROUTE, LOCATION_PICKER_ROUTE)

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val dir = direction()
        // A sub-page is pushed in from the right edge over the full width; tabs move a third, with a fade.
        if (isSubPage(targetState.destination.route)) {
            slideInHorizontally(spec) { width -> width }
        } else {
            slideInHorizontally(spec) { width -> dir * width / 3 } + fadeIn(fade)
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val dir = direction()
        slideOutHorizontally(spec) { width -> -dir * width / 3 } + fadeOut(fade)
    }

    /**
     * Closing a page (back button, back gesture or back arrow), for every page: the page slides all the way out to
     * the right (left to right) and the page below slides in from the left, right next to it. Moving side by side
     * matters because Android draws the page we go back to on top; this way neither hides the other.
     */
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(spec) { width -> -width }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(spec) { width -> width }
    }
}
