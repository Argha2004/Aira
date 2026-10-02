package com.aira.app.domain.engine

import java.time.LocalDate
import java.time.ZoneId

object DayRange {

    /** First and last millisecond of [date] in [zone], for "BETWEEN start AND end" queries. */
    fun of(date: LocalDate, zone: ZoneId): LongRange {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val nextStart = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until nextStart
    }
}
