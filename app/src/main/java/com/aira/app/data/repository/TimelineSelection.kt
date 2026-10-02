package com.aira.app.data.repository

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries "show this day on the Timeline" from the Calendar to the Timeline screen.
 * The Calendar calls [select]; the Timeline takes the date and clears it with [consume].
 */
@Singleton
class TimelineSelection @Inject constructor() {
    private val _requested = MutableStateFlow<LocalDate?>(null)
    val requested: StateFlow<LocalDate?> = _requested.asStateFlow()

    fun select(date: LocalDate) {
        _requested.value = date
    }

    fun consume() {
        _requested.value = null
    }
}
