package com.aira.app.domain.usecase

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskActionsTest {

    private val hour = 60 * 60 * 1000L
    private val now = 50_000_000L

    /** Records what the use case asked the reminder scheduler to do. */
    private class FakeReminders : ReminderScheduler {
        val calls = mutableListOf<String>()
        val scheduled = mutableListOf<WeatherTask>()
        override fun schedule(task: WeatherTask) {
            calls += "schedule:${task.id}"
            scheduled += task
        }
        override fun cancel(taskId: Long) {
            calls += "cancel:$taskId"
        }
    }

    private val tasks = FakeTaskStore()
    private val alerts = FakeAlertStore()
    private val reminders = FakeReminders()
    private val actions = TaskActions(tasks, alerts, reminders) { now }

    private suspend fun alertOfType(type: AlertType, message: String = "Rain likely around 4 PM") =
        alerts.insert(WeatherAlert(type = type, time = now, message = message))

    // ---- add / update / delete ----

    @Test
    fun `add saves the task with the creation time and schedules its reminders`() = runTest {
        val id = actions.add(WeatherTask(title = "Go to market", dueTime = now + hour))
        val saved = tasks.tasks.single()
        assertEquals(id, saved.id)
        assertEquals(now, saved.createdAt)
        assertEquals(listOf("schedule:$id"), reminders.calls)
        assertEquals(saved, reminders.scheduled.single())
    }

    @Test
    fun `update saves the change and refreshes the reminders`() = runTest {
        val id = actions.add(WeatherTask(title = "Old"))
        reminders.calls.clear()
        actions.update(tasks.get(id)!!.copy(title = "New"))
        assertEquals("New", tasks.get(id)!!.title)
        assertEquals(listOf("cancel:$id", "schedule:$id"), reminders.calls)
    }

    @Test
    fun `delete removes the task and its reminders`() = runTest {
        val id = actions.add(WeatherTask(title = "Temporary"))
        reminders.calls.clear()
        actions.delete(id)
        assertTrue(tasks.tasks.isEmpty())
        assertEquals(listOf("cancel:$id"), reminders.calls)
    }

    // ---- done / reopen / snooze / move ----

    @Test
    fun `done marks the task, records the time and cancels its reminders`() = runTest {
        val id = actions.add(WeatherTask(title = "Task", dueTime = now + hour))
        reminders.calls.clear()
        actions.markDone(id)
        val done = tasks.get(id)!!
        assertEquals(TaskStatus.DONE, done.status)
        assertEquals(now, done.completedAt)
        assertEquals(listOf("cancel:$id"), reminders.calls)
    }

    @Test
    fun `done on a task that no longer exists does nothing`() = runTest {
        actions.markDone(999)
        assertTrue(reminders.calls.isEmpty())
    }

    @Test
    fun `reopen makes a done task pending again and schedules it`() = runTest {
        val id = actions.add(WeatherTask(title = "Task", dueTime = now + hour))
        actions.markDone(id)
        reminders.calls.clear()
        actions.reopen(id)
        assertEquals(TaskStatus.PENDING, tasks.get(id)!!.status)
        assertNull(tasks.get(id)!!.completedAt)
        assertEquals(listOf("schedule:$id"), reminders.calls)
    }

    @Test
    fun `snooze can be shorter, like the 30 minute reminder on Home`() = runTest {
        val id = actions.add(WeatherTask(title = "Bring clothes inside"))
        actions.snooze(id, durationMs = 30 * 60 * 1000L)
        assertEquals(now + 30 * 60 * 1000L, tasks.get(id)!!.snoozedUntil)
    }

    @Test
    fun `snooze hides the task for an hour and reschedules its reminder`() = runTest {
        val id = actions.add(WeatherTask(title = "Task", dueTime = now + hour))
        reminders.calls.clear()
        actions.snooze(id)
        val snoozed = tasks.get(id)!!
        assertEquals(TaskStatus.SNOOZED, snoozed.status)
        assertEquals(now + hour, snoozed.snoozedUntil)
        assertEquals(listOf("cancel:$id", "schedule:$id"), reminders.calls)
        assertEquals(snoozed, reminders.scheduled.last())
    }

    @Test
    fun `move changes the due time and reschedules`() = runTest {
        val id = actions.add(WeatherTask(title = "Go to market", dueTime = now + 5 * hour, isOutdoor = true))
        reminders.calls.clear()
        actions.moveTo(id, now + 2 * hour)
        assertEquals(now + 2 * hour, tasks.get(id)!!.dueTime)
        assertEquals(listOf("cancel:$id", "schedule:$id"), reminders.calls)
    }

    // ---- suggestions from alerts ----

    @Test
    fun `adding a suggestion makes a plain task linked back to the alert`() = runTest {
        val alertId = alertOfType(AlertType.RAIN_SOON)
        val taskId = actions.addSuggestion(alertId, "Cover bike seat")!!
        val task = tasks.get(taskId)!!
        assertEquals("Cover bike seat", task.title)
        assertEquals(alertId, task.sourceAlertId)
        assertNull(task.trigger)
        assertTrue("Rain likely around 4 PM" in task.note)
    }

    @Test
    fun `a suggestion can be added with a due time, so its reminder comes then`() = runTest {
        val alertId = alertOfType(AlertType.RAIN_SOON)
        val due = now + 30 * 60 * 1000L
        val task = tasks.get(actions.addSuggestion(alertId, "Bring clothes inside", dueTime = due)!!)!!
        assertEquals(due, task.dueTime)
    }

    @Test
    fun `the notification button adds the first suggestion for the alert type`() = runTest {
        val alertId = alertOfType(AlertType.RAIN_SOON)
        val task = tasks.get(actions.addFirstSuggestion(alertId)!!)!!
        assertEquals("Carry an umbrella / raincoat", task.title)
        assertEquals(alertId, task.sourceAlertId)
    }

    @Test
    fun `each alert type adds its own first suggestion`() = runTest {
        val heat = tasks.get(actions.addFirstSuggestion(alertOfType(AlertType.HEAT, "Feels like 40 °C"))!!)!!
        val sun = tasks.get(actions.addFirstSuggestion(alertOfType(AlertType.STRONG_SUN, "45 minutes in strong sun"))!!)!!
        assertEquals("Drink water", heat.title)
        assertEquals("Apply sunscreen", sun.title)
    }

    @Test
    fun `a suggestion for an alert that is gone adds nothing`() = runTest {
        assertNull(actions.addSuggestion(999, "Anything"))
        assertNull(actions.addFirstSuggestion(999))
        assertTrue(tasks.tasks.isEmpty())
    }

    @Test
    fun `dismissing an alert marks it dismissed`() = runTest {
        val alertId = alertOfType(AlertType.HEAT)
        assertFalse(alerts.get(alertId)!!.dismissed)
        actions.dismissAlert(alertId)
        assertTrue(alerts.get(alertId)!!.dismissed)
    }
}
