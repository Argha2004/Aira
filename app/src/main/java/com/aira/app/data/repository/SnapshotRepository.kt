package com.aira.app.data.repository

import com.aira.app.data.local.SnapshotDao
import com.aira.app.data.local.toDomain
import com.aira.app.data.local.toEntity
import com.aira.app.domain.model.HourlyWeather
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.usecase.SnapshotStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves and reads snapshots. Times are epoch millis. */
@Singleton
class SnapshotRepository @Inject constructor(private val dao: SnapshotDao) : SnapshotStore {

    override suspend fun save(snapshot: Snapshot): Long = dao.insert(snapshot.toEntity())

    fun observeBetween(from: Long, to: Long): Flow<List<Snapshot>> =
        dao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun getBetween(from: Long, to: Long): List<Snapshot> =
        dao.getBetween(from, to).map { it.toDomain() }

    /** Every snapshot ever saved, oldest first (used by the CSV export). */
    suspend fun getAll(): List<Snapshot> = getBetween(0, Long.MAX_VALUE)

    fun observeLatest(): Flow<Snapshot?> = dao.observeLatest().map { it?.toDomain() }

    override suspend fun getLatest(): Snapshot? = dao.getLatest()?.toDomain()

    suspend fun count(from: Long, to: Long): Int = dao.countBetween(from, to)

    suspend fun countOutdoor(from: Long, to: Long): Int = dao.countOutdoorBetween(from, to)

    /** Snapshots taken outdoors while it rained more than [minRainMm]. */
    suspend fun countOutdoorRain(from: Long, to: Long, minRainMm: Double): Int =
        dao.countOutdoorRainBetween(from, to, minRainMm)

    /** Snapshots saved offline that still wait for their weather. */
    suspend fun pendingWeather(): List<Snapshot> = dao.getPendingWeather().map { it.toDomain() }

    /** Writes the weather of [hour] into snapshot [id] and clears its pending flag. */
    suspend fun fillWeather(id: Long, hour: HourlyWeather) = dao.fillWeather(
        id, hour.tempC, hour.feelsLikeC, hour.humidity, hour.rainMm, hour.uvIndex, hour.windKmh,
    )

    /** Replaces all snapshots between [from] and [to] with [snapshots] (used by the demo data). */
    suspend fun replaceRange(from: Long, to: Long, snapshots: List<Snapshot>) {
        dao.deleteBetween(from, to)
        dao.insertAll(snapshots.map { it.toEntity() })
    }

    suspend fun deleteAll() = dao.deleteAll()
}
