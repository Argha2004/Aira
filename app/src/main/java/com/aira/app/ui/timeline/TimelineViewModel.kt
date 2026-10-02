package com.aira.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.repository.AlertRepository
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.TaskRepository
import com.aira.app.data.repository.TimelineSelection
import com.aira.app.domain.engine.DailyStats
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.Note
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.usecase.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An event with the notes the user wrote for it. */
data class TimelineItem(val event: DiaryEvent, val notes: List<Note>)

/** One entry of the day's timeline: a diary event, an alert that fired, or a task that was ticked off. */
sealed interface TimelineEntry {
    /** When it happened (epoch millis); the entries are shown in this order. */
    val time: Long
    /** Distinguishes entries in the list, since events, alerts and tasks have separate id numbers. */
    val key: String

    data class Event(val item: TimelineItem) : TimelineEntry {
        override val time get() = item.event.startTime
        override val key get() = "event-${item.event.id}"
    }

    data class Alert(val alert: WeatherAlert) : TimelineEntry {
        override val time get() = alert.time
        override val key get() = "alert-${alert.id}"
    }

    data class TaskDone(val task: WeatherTask) : TimelineEntry {
        override val time get() = task.completedAt ?: 0L
        override val key get() = "task-${task.id}"
    }
}

data class TimelineUiState(
    val date: LocalDate,
    val isToday: Boolean,
    val entries: List<TimelineEntry>,
    /** True until the day's data has arrived, so "no events" is not shown by mistake. */
    val isLoading: Boolean = false,
    /** True if the day's data could not be read. */
    val hasError: Boolean = false,
    /** Sun, rain and outdoor time of the day, for the summary card. */
    val stats: DailyStats = DailyStats.EMPTY,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val diary: DiaryRepository,
    alerts: AlertRepository,
    tasks: TaskRepository,
    private val selection: TimelineSelection,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val date = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TimelineUiState> = date.flatMapLatest { day ->
        val range = DayRange.of(day, zone)
        combine(
            diary.observeEvents(range.first, range.last),
            diary.observeNotesInRange(range.first, range.last),
            alerts.observeBetween(range.first, range.last),
            tasks.observeCompletedBetween(range.first, range.last),
            diary.observeDailyStats(day, zone),
        ) { events, notes, dayAlerts, doneTasks, dayStats ->
            val notesByEvent = notes.groupBy { it.eventId }
            val entries = events.map { TimelineEntry.Event(TimelineItem(it, notesByEvent[it.id].orEmpty())) } +
                dayAlerts.map { TimelineEntry.Alert(it) } +
                doneTasks.map { TimelineEntry.TaskDone(it) }
            TimelineUiState(
                date = day,
                isToday = day == LocalDate.now(),
                entries = entries.sortedBy { it.time },
                stats = dayStats,
            )
        }.catch { emit(TimelineUiState(day, day == LocalDate.now(), emptyList(), hasError = true)) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TimelineUiState(date.value, true, emptyList(), isLoading = true),
    )

    init {
        // Make sure the events of the shown day are up to date, e.g. after "Take snapshot now".
        // A failed rebuild only means the shown events may be a little out of date; it must not crash the app.
        viewModelScope.launch { date.collect { runCatchingCancellable { diary.rebuildDay(it, zone) } } }
        // A day picked on the Calendar becomes the shown day.
        viewModelScope.launch {
            selection.requested.filterNotNull().collect {
                date.value = it
                selection.consume()
            }
        }
    }

    fun previousDay() = date.update { it.minusDays(1) }

    /** Going past today is not possible. */
    fun nextDay() = date.update { if (it < LocalDate.now()) it.plusDays(1) else it }

    fun addNote(eventId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            diary.addNote(Note(eventId = eventId, text = trimmed, createdAt = System.currentTimeMillis()))
        }
    }
}
