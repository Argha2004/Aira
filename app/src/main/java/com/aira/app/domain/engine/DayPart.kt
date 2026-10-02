package com.aira.app.domain.engine

/** The part of the day, used to name diary events ("Morning commute"). */
enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

object DayParts {
    /** 05:00 up to 12:00 is morning, up to 17:00 afternoon, up to 21:00 evening, the rest night. */
    fun of(hour: Int): DayPart = when (hour) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        in 17..20 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }
}
