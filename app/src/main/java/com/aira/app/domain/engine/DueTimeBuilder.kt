package com.aira.app.domain.engine

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Turns the optional date and time picked in the task form into one due time, and back. Pure Kotlin. */
object DueTimeBuilder {

    const val DEFAULT_HOUR = 9

    /**
     * Neither picked: no due time. Date only: that day at 09:00. Time only: today at that time,
     * or tomorrow if that time has already passed. Both: exactly that.
     */
    fun build(date: LocalDate?, time: LocalTime?, now: Long, zone: ZoneId): Long? {
        if (date == null && time == null) return null
        val nowLocal = Instant.ofEpochMilli(now).atZone(zone)
        val day = date ?: nowLocal.toLocalDate().let { today ->
            if (time!!.isAfter(nowLocal.toLocalTime())) today else today.plusDays(1)
        }
        val at = time ?: LocalTime.of(DEFAULT_HOUR, 0)
        return day.atTime(at).atZone(zone).toInstant().toEpochMilli()
    }

    /** The date and time of an existing due time, for filling in the form when editing. */
    fun split(dueTime: Long?, zone: ZoneId): Pair<LocalDate?, LocalTime?> {
        if (dueTime == null) return null to null
        val local = Instant.ofEpochMilli(dueTime).atZone(zone)
        return local.toLocalDate() to local.toLocalTime().withSecond(0).withNano(0)
    }
}
