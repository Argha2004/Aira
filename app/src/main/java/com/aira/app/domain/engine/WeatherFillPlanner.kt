package com.aira.app.domain.engine

import com.aira.app.domain.model.Snapshot
import java.time.Instant
import java.time.ZoneId

object WeatherFillPlanner {

    /**
     * The snapshots the weather service can still fill in. Its history request covers yesterday and today
     * (past_days=1), so a snapshot from the day before yesterday or older can never be filled: asking again
     * every hour would only waste battery and data. Those stay marked as "weather pending" and are simply
     * left out of the weather statistics.
     */
    fun fillable(pending: List<Snapshot>, now: Long, zone: ZoneId): List<Snapshot> {
        val startOfYesterday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return pending.filter { it.timestamp >= startOfYesterday }
    }
}
