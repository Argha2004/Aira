package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherTask

/** What to do with the tasks when an alert fires. */
data class TriggerResult(
    /** The tasks to list in the alert notification. */
    val toShow: List<WeatherTask>,
    /** Tasks that changed (reset to pending) and must be saved. */
    val toUpdate: List<WeatherTask>,
)

/** The rules for tasks linked to alerts, and for done and snooze. Pure Kotlin. */
object TaskTriggerEngine {

    const val SNOOZE_MS = 60 * 60 * 1000L

    /** A task is active when it is pending, or snoozed and its snooze time has passed. */
    fun isActive(task: WeatherTask, now: Long): Boolean = when (task.status) {
        TaskStatus.PENDING -> true
        TaskStatus.SNOOZED -> task.snoozedUntil == null || task.snoozedUntil <= now
        TaskStatus.DONE -> false
    }

    /**
     * An alert of type [fired] has just fired. Every active task linked to that type is shown.
     * A task that was done but repeats "every time" goes back to pending, so it comes up again now.
     * A snoozed task whose snooze has run out is pending again. A once-only task that is done stays done.
     */
    fun onAlertFired(fired: AlertType, tasks: List<WeatherTask>, now: Long): TriggerResult {
        val toShow = mutableListOf<WeatherTask>()
        val toUpdate = mutableListOf<WeatherTask>()
        for (task in tasks.filter { it.trigger == fired }) {
            when {
                task.status == TaskStatus.DONE && task.repeat == TaskRepeat.EVERY_TIME -> {
                    val reset = task.copy(status = TaskStatus.PENDING, completedAt = null, snoozedUntil = null)
                    toUpdate += reset
                    toShow += reset
                }
                task.status == TaskStatus.SNOOZED && isActive(task, now) -> {
                    val awake = task.copy(status = TaskStatus.PENDING, snoozedUntil = null)
                    toUpdate += awake
                    toShow += awake
                }
                task.status == TaskStatus.PENDING -> toShow += task
            }
        }
        return TriggerResult(toShow, toUpdate)
    }

    /** The active tasks linked to any of the [alertTypes] (used by the Home card for current alerts). */
    fun tasksForAlerts(alertTypes: Set<AlertType>, tasks: List<WeatherTask>, now: Long): List<WeatherTask> =
        tasks.filter { it.trigger in alertTypes && isActive(it, now) }

    fun markDone(task: WeatherTask, now: Long): WeatherTask =
        task.copy(status = TaskStatus.DONE, completedAt = now, snoozedUntil = null)

    /** Hides the task for an hour (or [durationMs]). */
    fun snooze(task: WeatherTask, now: Long, durationMs: Long = SNOOZE_MS): WeatherTask =
        task.copy(status = TaskStatus.SNOOZED, snoozedUntil = now + durationMs)

    /** Takes a done task back to pending (unticking it in the Done list). */
    fun reopen(task: WeatherTask): WeatherTask =
        task.copy(status = TaskStatus.PENDING, completedAt = null, snoozedUntil = null)
}
