package com.aira.app.domain.engine

import kotlin.math.round

/** The rules for saved locations. */
object LocationRules {
    /** Besides the live location, the user can save at most this many places. */
    const val MAX_CUSTOM_LOCATIONS = 5

    /** Longest name a saved place can have. */
    const val MAX_NAME_LENGTH = 30

    /** Whether one more place may be saved when [count] are saved already. */
    fun canAdd(count: Int): Boolean = count < MAX_CUSTOM_LOCATIONS

    /**
     * Coordinates are kept to 2 decimals (about 1 km), the same precision that is sent to the weather service,
     * so a saved place never stores a more exact position than needed.
     */
    fun round2(value: Double): Double = round(value * 100) / 100

    /** The name to save: trimmed and cut to [MAX_NAME_LENGTH]; [fallback] when it is blank. */
    fun cleanName(name: String, fallback: String): String =
        name.trim().take(MAX_NAME_LENGTH).ifEmpty { fallback.trim().take(MAX_NAME_LENGTH) }
}
