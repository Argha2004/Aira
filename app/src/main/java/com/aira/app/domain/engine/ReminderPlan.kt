package com.aira.app.domain.engine

import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherTask

enum class ReminderKind {
    /** Tell the user the task is due. */
    DUE,

    /** Check the forecast for an outdoor task and warn if the weather looks bad. */
    PLAN_CHECK,
}

data class ReminderJob(val kind: ReminderKind, val atMillis: Long)

/** Decides which background reminders a task needs. Pure Kotlin. */
object ReminderPlan {

    /**
     * - Done task: nothing.
     * - Snoozed task: a "due" reminder when the snooze ends.
     * - Task without a due time, or already past due: nothing.
     * - Otherwise a "due" reminder at the due time and, for an outdoor task, a forecast check
     *   3 hours before (right away if that moment has already passed).
     */
    fun jobs(task: WeatherTask, now: Long): List<ReminderJob> {
        if (task.status == TaskStatus.DONE) return emptyList()
        if (task.status == TaskStatus.SNOOZED && task.snoozedUntil != null) {
            return listOf(ReminderJob(ReminderKind.DUE, maxOf(task.snoozedUntil, now)))
        }
        val due = task.dueTime ?: return emptyList()
        if (due <= now) return emptyList()
        return buildList {
            add(ReminderJob(ReminderKind.DUE, due))
            if (task.isOutdoor) add(ReminderJob(ReminderKind.PLAN_CHECK, maxOf(due - Thresholds.PLAN_CHECK_LEAD_MS, now)))
        }
    }
}
