package com.aira.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.repository.TaskRepository
import com.aira.app.data.repository.WeatherRepository
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.OutdoorPlanChecker
import com.aira.app.domain.engine.ReminderKind
import com.aira.app.domain.engine.TaskTriggerEngine
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherTask
import com.aira.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Runs once for one task, at the moment set by [TaskScheduler]:
 * - DUE: the task's time has come (or its snooze ended): show a reminder with Done and Snooze buttons.
 * - PLAN_CHECK: 3 hours before an outdoor task: look at the forecast for that hour and warn if it looks bad.
 */
@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tasks: TaskRepository,
    private val snapshots: SnapshotRepository,
    private val weather: WeatherRepository,
    private val notifications: NotificationHelper,
    private val settings: SettingsStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val task = tasks.get(inputData.getLong(KEY_TASK_ID, -1)) ?: return Result.success()
        val now = System.currentTimeMillis()
        if (!TaskTriggerEngine.isActive(task, now)) return Result.success()

        return when (inputData.getString(KEY_KIND)) {
            ReminderKind.DUE.name -> {
                remind(task)
                Result.success()
            }
            ReminderKind.PLAN_CHECK.name -> checkPlan(task, now)
            else -> Result.success()
        }
    }

    private suspend fun remind(task: WeatherTask) {
        // A snooze that has ended makes the task pending again.
        if (task.status == TaskStatus.SNOOZED) tasks.update(task.copy(status = TaskStatus.PENDING, snoozedUntil = null))
        notifications.showTaskDue(task)
    }

    private suspend fun checkPlan(task: WeatherTask, now: Long): Result {
        val dueTime = task.dueTime ?: return Result.success()
        if (!task.isOutdoor) return Result.success()
        val place = snapshots.getLatest() ?: return Result.success() // no known location yet
        val forecast = weather.getForecast(place.latitude, place.longitude).getOrNull()
            ?: return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()

        val zone = ZoneId.systemDefault()
        OutdoorPlanChecker.check(dueTime, forecast, now, zone)?.let { warning ->
            notifications.showPlanWarning(task, warning, zone, settings.useFahrenheit.first())
        }
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_KIND = "kind"
        private const val MAX_ATTEMPTS = 3
    }
}
