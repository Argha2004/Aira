package com.aira.app.data.repository

import com.aira.app.data.local.TaskDao
import com.aira.app.data.local.toDomain
import com.aira.app.data.local.toEntity
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.usecase.TaskStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves tasks and reads them back. */
@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) : TaskStore {

    override suspend fun insert(task: WeatherTask): Long = dao.insert(task.toEntity())

    override suspend fun update(task: WeatherTask) = dao.update(task.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun get(id: Long): WeatherTask? = dao.get(id)?.toDomain()

    override suspend fun all(): List<WeatherTask> = dao.getAll().map { it.toDomain() }

    /** Tasks that are not done (pending and snoozed). */
    fun observePending(): Flow<List<WeatherTask>> = dao.observePending().map { list -> list.map { it.toDomain() } }

    /** Done tasks, most recently done first. */
    fun observeDone(): Flow<List<WeatherTask>> = dao.observeDone().map { list -> list.map { it.toDomain() } }

    /** Tasks ticked off between [from] and [to], e.g. one day, for the timeline. */
    fun observeCompletedBetween(from: Long, to: Long): Flow<List<WeatherTask>> =
        dao.observeCompletedBetween(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun deleteAll() = dao.deleteAll()
}
