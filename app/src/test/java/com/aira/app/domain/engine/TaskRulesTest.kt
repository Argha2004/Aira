package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherTask
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun task(
    id: Long = 1,
    title: String = "Task $id",
    trigger: AlertType? = null,
    status: TaskStatus = TaskStatus.PENDING,
    repeat: TaskRepeat = TaskRepeat.ONCE,
    due: Long? = null,
    snoozedUntil: Long? = null,
    outdoor: Boolean = false,
    createdAt: Long = id,
    completedAt: Long? = null,
) = WeatherTask(
    id = id, title = title, trigger = trigger, dueTime = due, isOutdoor = outdoor, repeat = repeat, status = status,
    snoozedUntil = snoozedUntil, createdAt = createdAt, completedAt = completedAt,
)

class TaskTriggerEngineTest {

    private val now = 10_000_000L
    private val hour = 60 * 60 * 1000L

    private fun fired(type: AlertType, vararg tasks: WeatherTask) = TaskTriggerEngine.onAlertFired(type, tasks.toList(), now)

    @Test
    fun `pending tasks linked to the alert are shown`() {
        val a = task(1, trigger = AlertType.RAIN_SOON)
        val b = task(2, trigger = AlertType.RAIN_SOON)
        assertEquals(listOf(a, b), fired(AlertType.RAIN_SOON, a, b).toShow)
    }

    @Test
    fun `tasks for other alerts and plain tasks are not shown`() {
        val result = fired(
            AlertType.RAIN_SOON,
            task(1, trigger = AlertType.HEAT),
            task(2, trigger = null),
            task(3, trigger = AlertType.RAIN_NOW),
        )
        assertTrue(result.toShow.isEmpty())
        assertTrue(result.toUpdate.isEmpty())
    }

    @Test
    fun `a done once-only task stays done and is not shown`() {
        val result = fired(AlertType.HEAT, task(1, trigger = AlertType.HEAT, status = TaskStatus.DONE, completedAt = 5))
        assertTrue(result.toShow.isEmpty())
        assertTrue(result.toUpdate.isEmpty())
    }

    @Test
    fun `a done every-time task comes back as pending and is shown`() {
        val done = task(1, trigger = AlertType.HEAT, status = TaskStatus.DONE, repeat = TaskRepeat.EVERY_TIME, completedAt = 5)
        val result = fired(AlertType.HEAT, done)
        val back = result.toShow.single()
        assertEquals(TaskStatus.PENDING, back.status)
        assertNull(back.completedAt)
        assertEquals(listOf(back), result.toUpdate)
    }

    @Test
    fun `an every-time task that is still pending is shown without being changed`() {
        val pending = task(1, trigger = AlertType.HEAT, repeat = TaskRepeat.EVERY_TIME)
        val result = fired(AlertType.HEAT, pending)
        assertEquals(listOf(pending), result.toShow)
        assertTrue(result.toUpdate.isEmpty())
    }

    @Test
    fun `a snoozed task whose snooze ended is pending again and shown`() {
        val snoozed = task(1, trigger = AlertType.HEAT, status = TaskStatus.SNOOZED, snoozedUntil = now - 1)
        val result = fired(AlertType.HEAT, snoozed)
        assertEquals(TaskStatus.PENDING, result.toShow.single().status)
        assertNull(result.toShow.single().snoozedUntil)
        assertEquals(1, result.toUpdate.size)
    }

    @Test
    fun `a snoozed task that is still snoozed is not shown`() {
        val snoozed = task(1, trigger = AlertType.HEAT, status = TaskStatus.SNOOZED, snoozedUntil = now + hour)
        val result = fired(AlertType.HEAT, snoozed)
        assertTrue(result.toShow.isEmpty())
        assertTrue(result.toUpdate.isEmpty())
    }

    // ---- isActive ----

    @Test fun `pending is active`() = assertTrue(TaskTriggerEngine.isActive(task(), now))
    @Test fun `done is not active`() = assertFalse(TaskTriggerEngine.isActive(task(status = TaskStatus.DONE), now))

    @Test
    fun `snoozed is active exactly when the snooze time has come`() {
        assertFalse(TaskTriggerEngine.isActive(task(status = TaskStatus.SNOOZED, snoozedUntil = now + 1), now))
        assertTrue(TaskTriggerEngine.isActive(task(status = TaskStatus.SNOOZED, snoozedUntil = now), now))
    }

    // ---- done / snooze / reopen ----

    @Test
    fun `marking done sets the status and the time and clears a snooze`() {
        val done = TaskTriggerEngine.markDone(task(status = TaskStatus.SNOOZED, snoozedUntil = now + hour), now)
        assertEquals(TaskStatus.DONE, done.status)
        assertEquals(now, done.completedAt)
        assertNull(done.snoozedUntil)
    }

    @Test
    fun `snooze hides the task for one hour`() {
        val snoozed = TaskTriggerEngine.snooze(task(), now)
        assertEquals(TaskStatus.SNOOZED, snoozed.status)
        assertEquals(now + hour, snoozed.snoozedUntil)
    }

    @Test
    fun `snooze can use another length`() =
        assertEquals(now + 5, TaskTriggerEngine.snooze(task(), now, durationMs = 5).snoozedUntil)

    @Test
    fun `reopen takes a done task back to pending`() {
        val reopened = TaskTriggerEngine.reopen(task(status = TaskStatus.DONE, completedAt = 3))
        assertEquals(TaskStatus.PENDING, reopened.status)
        assertNull(reopened.completedAt)
    }

    // ---- Home card ----

    @Test
    fun `tasks for active alerts are those linked to any active type and still active`() {
        val rain = task(1, trigger = AlertType.RAIN_SOON)
        val heat = task(2, trigger = AlertType.HEAT)
        val plain = task(3)
        val sleeping = task(4, trigger = AlertType.RAIN_SOON, status = TaskStatus.SNOOZED, snoozedUntil = now + hour)
        val shown = TaskTriggerEngine.tasksForAlerts(setOf(AlertType.RAIN_SOON), listOf(rain, heat, plain, sleeping), now)
        assertEquals(listOf(rain), shown)
    }

    @Test
    fun `no active alerts means no tasks`() =
        assertTrue(TaskTriggerEngine.tasksForAlerts(emptySet(), listOf(task(1, trigger = AlertType.HEAT)), now).isEmpty())
}

class ReminderPlanTest {
    private val now = 1_000_000_000L
    private val hour = 60 * 60 * 1000L

    @Test
    fun `a task without a due time needs no reminder`() = assertTrue(ReminderPlan.jobs(task(), now).isEmpty())

    @Test
    fun `a done task needs no reminder`() =
        assertTrue(ReminderPlan.jobs(task(due = now + hour, status = TaskStatus.DONE), now).isEmpty())

    @Test
    fun `a task past its due time needs no reminder`() = assertTrue(ReminderPlan.jobs(task(due = now - 1), now).isEmpty())

    @Test
    fun `a timed task gets a due reminder at its due time`() =
        assertEquals(listOf(ReminderJob(ReminderKind.DUE, now + 5 * hour)), ReminderPlan.jobs(task(due = now + 5 * hour), now))

    @Test
    fun `an outdoor task also gets a forecast check 3 hours before`() {
        val jobs = ReminderPlan.jobs(task(due = now + 5 * hour, outdoor = true), now)
        assertEquals(
            listOf(ReminderJob(ReminderKind.DUE, now + 5 * hour), ReminderJob(ReminderKind.PLAN_CHECK, now + 2 * hour)),
            jobs,
        )
    }

    @Test
    fun `an outdoor task due within 3 hours is checked right away`() {
        val jobs = ReminderPlan.jobs(task(due = now + hour, outdoor = true), now)
        assertEquals(ReminderJob(ReminderKind.PLAN_CHECK, now), jobs.single { it.kind == ReminderKind.PLAN_CHECK })
    }

    @Test
    fun `a snoozed task is reminded when the snooze ends`() {
        val snoozed = task(due = now + 9 * hour, status = TaskStatus.SNOOZED, snoozedUntil = now + hour)
        assertEquals(listOf(ReminderJob(ReminderKind.DUE, now + hour)), ReminderPlan.jobs(snoozed, now))
    }

    @Test
    fun `a snooze that has already ended reminds right away`() {
        val snoozed = task(status = TaskStatus.SNOOZED, snoozedUntil = now - hour)
        assertEquals(listOf(ReminderJob(ReminderKind.DUE, now)), ReminderPlan.jobs(snoozed, now))
    }
}

class TaskGrouperTest {
    private val utc = ZoneOffset.UTC
    private fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 9, day, hour, 0).toInstant(utc).toEpochMilli()
    private val now = at(29, 12)

    private fun group(vararg tasks: WeatherTask) = TaskGrouper.group(tasks.toList(), now, utc)

    @Test
    fun `no tasks give empty groups`() = assertTrue(group().isEmpty)

    @Test
    fun `tasks with an alert type are linked to alerts whatever their due time`() {
        val linked = task(1, trigger = AlertType.HEAT)
        val linkedTimed = task(2, trigger = AlertType.RAIN_SOON, due = at(30, 9))
        assertEquals(listOf(linked, linkedTimed), group(linked, linkedTimed).linkedToAlerts)
    }

    @Test
    fun `a task without a due time is under today`() {
        val undated = task(1)
        assertEquals(listOf(undated), group(undated).today)
    }

    @Test
    fun `a task due later today is under today, ordered by time`() {
        val late = task(1, due = at(29, 20))
        val early = task(2, due = at(29, 15))
        assertEquals(listOf(early, late), group(late, early).today)
    }

    @Test
    fun `an overdue task is still under today`() {
        val overdue = task(1, due = at(27, 9))
        assertEquals(listOf(overdue), group(overdue).today)
    }

    @Test
    fun `a task due on a later day is upcoming, ordered by time`() {
        val later = task(1, due = at(30, 18))
        val sooner = task(2, due = at(30, 8))
        assertEquals(listOf(sooner, later), group(later, sooner).upcoming)
        assertTrue(group(later, sooner).today.isEmpty())
    }

    @Test
    fun `undated tasks come after timed ones under today`() {
        val undated = task(1)
        val timed = task(2, due = at(29, 15))
        assertEquals(listOf(timed, undated), group(undated, timed).today)
    }
}

class DueTimeBuilderTest {
    private val utc = ZoneOffset.UTC
    private fun millis(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 9, day, hour, minute).toInstant(utc).toEpochMilli()
    private val now = millis(29, 12) // Tuesday 12:00
    private val date = LocalDate.of(2026, 10, 2)

    @Test
    fun `neither date nor time gives no due time`() = assertNull(DueTimeBuilder.build(null, null, now, utc))

    @Test
    fun `date and time give exactly that moment`() =
        assertEquals(
            LocalDateTime.of(2026, 10, 2, 17, 30).toInstant(utc).toEpochMilli(),
            DueTimeBuilder.build(date, LocalTime.of(17, 30), now, utc),
        )

    @Test
    fun `date only means 9 in the morning`() =
        assertEquals(
            LocalDateTime.of(2026, 10, 2, 9, 0).toInstant(utc).toEpochMilli(),
            DueTimeBuilder.build(date, null, now, utc),
        )

    @Test
    fun `time only later today means today`() =
        assertEquals(millis(29, 17), DueTimeBuilder.build(null, LocalTime.of(17, 0), now, utc))

    @Test
    fun `time only that has passed today means tomorrow`() =
        assertEquals(millis(30, 8), DueTimeBuilder.build(null, LocalTime.of(8, 0), now, utc))

    @Test
    fun `a due time splits back into its date and time`() {
        val (d, t) = DueTimeBuilder.split(millis(30, 14, 45), utc)
        assertEquals(LocalDate.of(2026, 9, 30), d)
        assertEquals(LocalTime.of(14, 45), t)
    }

    @Test
    fun `no due time splits into nothing`() = assertEquals(null to null, DueTimeBuilder.split(null, utc))
}
