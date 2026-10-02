package com.aira.app.domain.engine

import com.aira.app.domain.model.WeatherTask
import java.time.Instant
import java.time.ZoneId

/** The three groups of pending tasks on the Tasks tab. */
data class TaskGroups(
    val linkedToAlerts: List<WeatherTask>,
    val today: List<WeatherTask>,
    val upcoming: List<WeatherTask>,
) {
    val isEmpty: Boolean get() = linkedToAlerts.isEmpty() && today.isEmpty() && upcoming.isEmpty()
}

object TaskGrouper {

    /**
     * Linked to alerts: tasks with an alert type. The rest: no due time, or due today or earlier, go under
     * Today; due on a later day goes under Upcoming. Timed tasks are ordered by time.
     */
    fun group(pending: List<WeatherTask>, now: Long, zone: ZoneId): TaskGroups {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val (linked, plain) = pending.partition { it.trigger != null }
        val (upcoming, todayOrEarlier) = plain.partition { task ->
            task.dueTime?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().isAfter(today) } == true
        }
        return TaskGroups(
            linkedToAlerts = linked.sortedBy { it.createdAt },
            today = todayOrEarlier.sortedWith(compareBy(nullsLast<Long>()) { it.dueTime }),
            upcoming = upcoming.sortedBy { it.dueTime },
        )
    }
}
