package com.aira.app.domain.engine

/** Spots a falling barometer. Pure Kotlin. */
object PressureTrend {

    /**
     * True when pressure fell by more than 3 hPa within the last 3 hours.
     * [readings] are (time in epoch millis, pressure in hPa) pairs in any order.
     */
    fun detectDrop(readings: List<Pair<Long, Float>>): Boolean = dropAmount(readings) > Thresholds.PRESSURE_DROP_HPA

    /**
     * How far pressure has fallen in the 3 hours up to the newest reading: the highest reading
     * in that window minus the newest reading. 0 when there are fewer than 2 readings.
     */
    fun dropAmount(readings: List<Pair<Long, Float>>): Float {
        if (readings.size < 2) return 0f
        val newest = readings.maxBy { it.first }
        val windowStart = newest.first - Thresholds.PRESSURE_WINDOW_MS
        val highest = readings.filter { it.first >= windowStart }.maxOf { it.second }
        return highest - newest.second
    }
}
