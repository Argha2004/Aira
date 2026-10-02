package com.aira.app.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aira.app.domain.engine.ReminderJob
import com.aira.app.domain.engine.ReminderKind
import com.aira.app.domain.engine.ReminderPlan
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.usecase.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets up the one-time background jobs of a task: a "due" reminder and, for outdoor tasks, a forecast
 * check 3 hours earlier. What jobs a task needs is decided by [ReminderPlan]. Scheduling again for the
 * same task replaces its earlier jobs.
 */
@Singleton
class TaskScheduler @Inject constructor(@ApplicationContext private val context: Context) : ReminderScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    override fun schedule(task: WeatherTask) {
        val now = System.currentTimeMillis()
        val jobs = ReminderPlan.jobs(task, now)
        ReminderKind.entries.forEach { kind ->
            val job = jobs.firstOrNull { it.kind == kind }
            if (job == null) workManager.cancelUniqueWork(workName(task.id, kind)) else enqueue(task.id, job, now)
        }
    }

    override fun cancel(taskId: Long) {
        ReminderKind.entries.forEach { workManager.cancelUniqueWork(workName(taskId, it)) }
    }

    private fun enqueue(taskId: Long, job: ReminderJob, now: Long) {
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay((job.atMillis - now).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(TaskReminderWorker.KEY_TASK_ID, taskId)
                    .putString(TaskReminderWorker.KEY_KIND, job.kind.name)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork(workName(taskId, job.kind), ExistingWorkPolicy.REPLACE, request)
    }

    private fun workName(taskId: Long, kind: ReminderKind) = "aira_task_${kind.name.lowercase()}_$taskId"
}
