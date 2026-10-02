package com.aira.app.domain.engine

import com.aira.app.domain.model.DiaryEvent

/** What to change in the database so it matches freshly built events. */
data class DiaryPlan(
    val insert: List<DiaryEvent>,
    /** Events that already exist (id kept) but have new end time, summary or temperature. */
    val update: List<DiaryEvent>,
    val deleteIds: List<Long>,
)

/**
 * Compares the saved events of a day with newly built ones. An event that is still the same
 * (same type, same start time) keeps its id, so notes attached to it are not lost when the day is rebuilt.
 */
object DiaryMerge {

    fun plan(existing: List<DiaryEvent>, fresh: List<DiaryEvent>): DiaryPlan {
        val existingByKey = existing.associateBy { it.type to it.startTime }
        val freshKeys = fresh.map { it.type to it.startTime }.toSet()

        val insert = mutableListOf<DiaryEvent>()
        val update = mutableListOf<DiaryEvent>()
        for (event in fresh) {
            val saved = existingByKey[event.type to event.startTime]
            when {
                saved == null -> insert += event
                saved.copy(id = 0) != event.copy(id = 0) -> update += event.copy(id = saved.id)
            }
        }
        val deleteIds = existing.filter { (it.type to it.startTime) !in freshKeys }.map { it.id }
        return DiaryPlan(insert, update, deleteIds)
    }
}
