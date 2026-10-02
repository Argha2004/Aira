package com.aira.app.domain.engine

/** Decides when background logging sleeps. Pure Kotlin. */
object NightPause {

    /** True from 23:00 until 06:00 (the hour is 0-23, in the user's local time). */
    fun isPaused(hourOfDay: Int): Boolean =
        hourOfDay >= Thresholds.NIGHT_PAUSE_START_HOUR || hourOfDay < Thresholds.NIGHT_PAUSE_END_HOUR

    /** Rough daylight guess from the clock, used only when the weather service was unreachable. */
    fun isDaytimeByClock(hourOfDay: Int): Boolean =
        hourOfDay >= Thresholds.DAY_START_HOUR && hourOfDay < Thresholds.DAY_END_HOUR
}
