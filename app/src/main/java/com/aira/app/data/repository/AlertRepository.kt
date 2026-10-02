package com.aira.app.data.repository

import com.aira.app.data.local.AlertDao
import com.aira.app.data.local.toDomain
import com.aira.app.data.local.toEntity
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.usecase.AlertStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves fired alerts and reads them back. */
@Singleton
class AlertRepository @Inject constructor(private val dao: AlertDao) : AlertStore {

    override suspend fun insert(alert: WeatherAlert): Long = dao.insert(alert.toEntity())

    override suspend fun get(id: Long): WeatherAlert? = dao.get(id)?.toDomain()

    override suspend fun dismiss(id: Long) = dao.dismiss(id)

    /** Alerts of the last 24 hours that the user has not dismissed, newest first. */
    fun observeActive(now: Long = System.currentTimeMillis()): Flow<List<WeatherAlert>> =
        dao.observeActive(now - DAY_MS).map { list -> list.mapNotNull { it.toDomain() } }

    /** Every alert ever fired, newest first. */
    fun observeHistory(): Flow<List<WeatherAlert>> = dao.observeHistory().map { list -> list.mapNotNull { it.toDomain() } }

    /** Alerts that fired between [from] and [to], e.g. one day, for the timeline. */
    fun observeBetween(from: Long, to: Long): Flow<List<WeatherAlert>> =
        dao.observeBetween(from, to).map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun deleteAll() = dao.deleteAll()

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
