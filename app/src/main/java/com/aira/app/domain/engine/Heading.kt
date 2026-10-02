package com.aira.app.domain.engine

/** Compass maths. Headings are degrees clockwise from north, 0 up to (not including) 360. */
object Heading {

    /** Any angle brought into 0..360, e.g. -90 → 270, 370 → 10. */
    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    /**
     * Moves the shown heading [previous] a little towards the new reading [reading] ([alpha] of the way), so the
     * dial does not jitter. Takes the short way round: from 350° to 10° it turns 20°, not 340°.
     */
    fun smooth(previous: Float?, reading: Float, alpha: Float = 0.15f): Float {
        if (previous == null) return normalize(reading)
        val difference = ((reading - previous + 540f) % 360f) - 180f
        return normalize(previous + alpha * difference)
    }
}
