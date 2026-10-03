package com.aira.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aira.app.data.battery.BatteryMonitor
import androidx.work.ExistingWorkPolicy
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.domain.engine.SnapshotTiming
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.BatteryRules
import com.aira.app.domain.engine.NightPause
import com.aira.app.domain.usecase.CheckAlertsUseCase
import com.aira.app.domain.usecase.NoLocationException
import com.aira.app.domain.usecase.TakeSnapshotUseCase
import com.aira.app.domain.usecase.runCatchingCancellable
import com.aira.app.widget.WeatherWidgetProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first

/**
 * Runs every 5-60 minutes in the background and saves one snapshot, then updates today's timeline
 * and checks for weather alerts.
 *
 * It skips (successfully) when logging is off, during the night pause (23:00 to 06:00, if enabled),
 * or when the battery is below the saver threshold and the phone is not charging.
 * A missing location is not retried, because retrying cannot fix a missing permission.
 * Being offline is not an error here: the snapshot is saved and the weather is filled in later.
 */
@HiltWorker
class SnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val takeSnapshot: TakeSnapshotUseCase,
    private val checkAlerts: CheckAlertsUseCase,
    private val settings: SettingsStore,
    private val diary: DiaryRepository,
    private val battery: BatteryMonitor,
    private val snapshots: SnapshotRepository,
    private val scheduler: WorkScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settings.loggingEnabled.first()) return Result.success()
        keepChainGoing()
        if (settings.nightPauseEnabled.first() && NightPause.isPaused(LocalTime.now().hour)) return Result.success()
        if (batteryTooLow()) return Result.success()
        // The chain and the safety job can meet; one fresh snapshot is enough.
        if (SnapshotTiming.tooSoon(snapshots.getLatest()?.timestamp, System.currentTimeMillis())) return Result.success()

        return takeSnapshot().fold(
            onSuccess = {
                // The snapshot is already saved. If the timeline rebuild fails, the alerts are still checked
                // (and the other way round): one side job must not stop the other.
                runCatchingCancellable { diary.rebuildDay(LocalDate.now()) }
                    .onFailure { Log.e(TAG, "Could not rebuild today's timeline", it) }
                runCatchingCancellable { checkAlerts() }
                    .onFailure { Log.e(TAG, "Could not check for alerts", it) }
                // The home-screen widget follows every snapshot.
                runCatchingCancellable { WeatherWidgetProvider.refreshAll(applicationContext) }
                    .onFailure { Log.e(TAG, "Could not update the widget", it) }
                Result.success()
            },
            onFailure = { error ->
                if (error is NoLocationException || runAttemptCount >= MAX_ATTEMPTS) Result.success()
                else Result.retry()
            },
        )
    }

    /**
     * In 5-minute mode, makes sure the next check is planned: a chain link plans the one after it; the 15-minute
     * safety job only restarts the chain when no link is waiting.
     */
    private suspend fun keepChainGoing() {
        val minutes = settings.intervalMinutes.first()
        if (minutes >= WorkScheduler.MIN_INTERVAL_MINUTES) return
        val isLink = inputData.getBoolean(WorkScheduler.KEY_CHAIN, false)
        scheduler.scheduleNextCheck(minutes, if (isLink) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP)
    }

    private suspend fun batteryTooLow(): Boolean {
        val status = battery.read()
        return BatteryRules.shouldSkip(status.levelPercent, status.charging, settings.batterySaverThreshold.first())
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val TAG = "SnapshotWorker"
    }
}
