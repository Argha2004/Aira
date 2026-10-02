package com.aira.app.domain.usecase

import com.aira.app.domain.engine.TaskSuggestions
import com.aira.app.domain.engine.TaskTriggerEngine
import com.aira.app.domain.model.WeatherTask
import javax.inject.Inject

/**
 * Everything the user can do with a task or an alert, whether from the Tasks tab or from a notification
 * button. Each action saves the change and keeps the background reminders in step with it.
 */
class TaskActions(
    private val tasks: TaskStore,
    private val alerts: AlertStore,
    private val reminders: ReminderScheduler,
    private val nowMillis: () -> Long,
) {
    @Inject
    constructor(tasks: TaskStore, alerts: AlertStore, reminders: ReminderScheduler) :
        this(tasks, alerts, reminders, System::currentTimeMillis)

    /** Saves a new task and sets up its reminders. Returns the new id. */
    suspend fun add(task: WeatherTask): Long {
        val id = tasks.insert(task.copy(id = 0, createdAt = nowMillis()))
        tasks.get(id)?.let { reminders.schedule(it) }
        return id
    }

    /** Saves changes to an existing task and refreshes its reminders. */
    suspend fun update(task: WeatherTask) {
        tasks.update(task)
        reminders.cancel(task.id)
        reminders.schedule(task)
    }

    suspend fun delete(id: Long) {
        reminders.cancel(id)
        tasks.delete(id)
    }

    /** Ticks a task as done. An "every time" task comes back the next time its alert fires. */
    suspend fun markDone(id: Long) {
        val task = tasks.get(id) ?: return
        tasks.update(TaskTriggerEngine.markDone(task, nowMillis()))
        reminders.cancel(id)
    }

    /** Unticks a done task. */
    suspend fun reopen(id: Long) {
        val task = tasks.get(id) ?: return
        val reopened = TaskTriggerEngine.reopen(task)
        tasks.update(reopened)
        reminders.schedule(reopened)
    }

    /** Hides the task for [durationMs] (an hour unless told otherwise); the reminder comes back afterwards. */
    suspend fun snooze(id: Long, durationMs: Long = HOUR_MS) {
        val task = tasks.get(id) ?: return
        val snoozed = TaskTriggerEngine.snooze(task, nowMillis(), durationMs)
        tasks.update(snoozed)
        reminders.cancel(id)
        reminders.schedule(snoozed)
    }

    /** Moves the task to another time and sets up its reminders again. */
    suspend fun moveTo(id: Long, newDueTime: Long) {
        val task = tasks.get(id) ?: return
        update(task.copy(dueTime = newDueTime))
    }

    /**
     * Adds [title] as a task suggested by alert [alertId] (a plain task, once, linked back to the alert),
     * due at [dueTime] if given (then its reminder comes at that time).
     * Returns the new task id, or null if the alert no longer exists.
     */
    suspend fun addSuggestion(alertId: Long, title: String, dueTime: Long? = null): Long? {
        val alert = alerts.get(alertId) ?: return null
        return add(
            WeatherTask(
                title = title,
                note = alert.message,
                sourceAlertId = alertId,
                dueTime = dueTime,
            ),
        )
    }

    /** The "Add task" notification button: adds the first suggestion for the alert's type. */
    suspend fun addFirstSuggestion(alertId: Long): Long? {
        val alert = alerts.get(alertId) ?: return null
        val title = TaskSuggestions.forAlert(alert.type).firstOrNull() ?: return null
        return addSuggestion(alertId, title)
    }

    suspend fun dismissAlert(alertId: Long) = alerts.dismiss(alertId)

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
    }
}
