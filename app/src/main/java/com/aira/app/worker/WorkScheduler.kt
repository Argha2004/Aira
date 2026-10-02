package com.aira.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and stops the background jobs. Safe to call repeatedly: the same job is updated, not duplicated.
 *
 * Android runs a repeating job at most every 15 minutes. For shorter intervals (5 minutes) each check
 * schedules the next one ("chain"); the 15-minute repeating job stays on as a safety net that restarts the
 * chain if it ever stops (for example after the phone was in deep sleep).
 */
@Singleton
class WorkScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val workManager get() = WorkManager.getInstance(context)

    private val batteryNotLow = Constraints.Builder().setRequiresBatteryNotLow(true).build()

    /** Takes a snapshot every [intervalMinutes]: a repeating job, plus the 5-minute chain below 15 minutes. */
    fun scheduleSnapshots(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<SnapshotWorker>(
            intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES).toLong(),
            TimeUnit.MINUTES,
        ).setConstraints(batteryNotLow).build()
        workManager.enqueueUniquePeriodicWork(SNAPSHOT_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        if (intervalMinutes < MIN_INTERVAL_MINUTES) {
            scheduleNextCheck(intervalMinutes, ExistingWorkPolicy.KEEP)
        } else {
            workManager.cancelUniqueWork(CHAIN_WORK)
        }
    }

    /**
     * The next link of the chain: one check [minutes] from now. A link calls this with APPEND_OR_REPLACE (the next
     * one waits until it has finished); the safety job and the settings use KEEP (only if no link is waiting).
     */
    fun scheduleNextCheck(minutes: Int, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SnapshotWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_CHAIN to true))
            .setConstraints(batteryNotLow)
            .build()
        workManager.enqueueUniqueWork(CHAIN_WORK, policy, request)
    }

    /** Once an hour, fills in weather for snapshots saved while offline. */
    fun scheduleWeatherFill() {
        val request = PeriodicWorkRequestBuilder<WeatherFillWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(WEATHER_FILL_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(SNAPSHOT_WORK)
        workManager.cancelUniqueWork(CHAIN_WORK)
        workManager.cancelUniqueWork(WEATHER_FILL_WORK)
    }

    companion object {
        const val SNAPSHOT_WORK = "aira_snapshots"
        const val CHAIN_WORK = "aira_snapshot_chain"
        const val WEATHER_FILL_WORK = "aira_weather_fill"
        const val MIN_INTERVAL_MINUTES = 15
        /** Input flag that marks a run as a link of the 5-minute chain. */
        const val KEY_CHAIN = "chain"
    }
}
