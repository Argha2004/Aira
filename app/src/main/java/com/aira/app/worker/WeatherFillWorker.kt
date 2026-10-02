package com.aira.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.repository.WeatherRepository
import com.aira.app.domain.engine.WeatherFillPlanner
import com.aira.app.domain.model.forTimestamp
import com.aira.app.domain.usecase.runCatchingCancellable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.round

/**
 * Runs hourly. Snapshots saved while offline have no weather yet; this asks Open-Meteo for
 * yesterday's and today's hourly weather and copies the matching hour into each snapshot.
 * Snapshots older than yesterday cannot be filled (the API only returns past_days=1) and stay pending.
 */
@HiltWorker
class WeatherFillWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val snapshots: SnapshotRepository,
    private val weather: WeatherRepository,
    private val diary: DiaryRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val zone = ZoneId.systemDefault()
        // Older ones can never be filled (see WeatherFillPlanner), so they are not asked for again.
        val pending = WeatherFillPlanner.fillable(snapshots.pendingWeather(), System.currentTimeMillis(), zone)
        if (pending.isEmpty()) return Result.success()

        var networkFailed = false
        val filledDays = mutableSetOf<LocalDate>()
        // One request per (rounded) location instead of one per snapshot.
        for ((location, group) in pending.groupBy { round2(it.latitude) to round2(it.longitude) }) {
            val hours = weather.getRecentHours(location.first, location.second).getOrNull()
            if (hours == null) {
                networkFailed = true
                continue
            }
            for (snapshot in group) {
                hours.forTimestamp(snapshot.timestamp)?.let {
                    snapshots.fillWeather(snapshot.id, it)
                    filledDays += Instant.ofEpochMilli(snapshot.timestamp).atZone(zone).toLocalDate()
                }
            }
        }
        // Weather changes what the diary says (rain, heat), so rebuild the days that were filled.
        filledDays.forEach { day ->
            runCatchingCancellable { diary.rebuildDay(day, zone) }.onFailure { Log.e(TAG, "Could not rebuild $day", it) }
        }
        return if (networkFailed && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    private fun round2(value: Double) = round(value * 100) / 100

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val TAG = "WeatherFillWorker"
    }
}
