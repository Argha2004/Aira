package com.aira.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.repository.TimelineSelection
import com.aira.app.domain.engine.DayAggregator
import com.aira.app.domain.engine.DayClassifier
import com.aira.app.domain.engine.DayCondition
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.engine.DaySummary
import com.aira.app.domain.model.DiaryEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The month shown, the icon condition of each day that has data (days not in [days] have no data), the
 * summaries behind them, the days with notes, and the chosen day with its events.
 */
data class CalendarUiState(
    val month: YearMonth,
    val days: Map<LocalDate, DayCondition>,
    val isCurrentMonth: Boolean,
    /** True until the month's data has been worked out. */
    val isLoading: Boolean = false,
    /** True if the data could not be read. */
    val hasError: Boolean = false,
    val summaries: Map<LocalDate, DaySummary> = emptyMap(),
    /** Days on which the user wrote at least one note, with how many. */
    val notes: Map<LocalDate, Int> = emptyMap(),
    /** The day shown in the day card, or null if the month has no data. */
    val selected: LocalDate? = null,
    /** The diary events of the chosen day ("Logged moments"). */
    val events: List<DiaryEvent> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    snapshots: SnapshotRepository,
    diary: DiaryRepository,
    private val selection: TimelineSelection,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val month = MutableStateFlow(YearMonth.now())
    private val chosen = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<CalendarUiState> = month.flatMapLatest { shown ->
        val from = DayRange.of(shown.atDay(1), zone).first
        val to = DayRange.of(shown.atEndOfMonth(), zone).last
        combine(
            snapshots.observeBetween(from, to),
            diary.observeNotesInRange(from, to),
            chosen,
        ) { list, notes, picked ->
            val summaries = DayAggregator.summarize(list, zone)
            val isCurrent = shown == YearMonth.now()
            val today = LocalDate.now()
            // The chosen day, else today (in the current month), else the last day with data.
            val selected = picked?.takeIf { YearMonth.from(it) == shown }
                ?: today.takeIf { isCurrent }
                ?: summaries.lastOrNull()?.date
            CalendarUiState(
                month = shown,
                days = summaries.associate { it.date to DayClassifier.classify(it) },
                isCurrentMonth = isCurrent,
                summaries = summaries.associateBy { it.date },
                notes = notes.groupingBy { dateOf(it.createdAt) }.eachCount(),
                selected = selected,
            )
        }
            // Then the events of the chosen day, for "Logged moments".
            .flatMapLatest { state ->
                val day = state.selected ?: return@flatMapLatest flowOf(state)
                val range = DayRange.of(day, zone)
                diary.observeEvents(range.first, range.last).map { state.copy(events = it.sortedBy { e -> e.startTime }) }
            }
            .flowOn(Dispatchers.Default) // summing up a month is done off the main thread
            .catch { emit(CalendarUiState(shown, emptyMap(), shown == YearMonth.now(), hasError = true)) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(month.value, emptyMap(), isCurrentMonth = true, isLoading = true),
    )

    private fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun previousMonth() = month.update { it.minusMonths(1) }

    /** Going past the current month is not possible. */
    fun nextMonth() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }

    /** Back to the current month, with today chosen. */
    fun goToToday() {
        month.value = YearMonth.now()
        chosen.value = LocalDate.now()
    }

    /** Shows [date] in the "active day" card. */
    fun select(date: LocalDate) {
        chosen.value = date
    }

    /** Tells the Timeline which day to show. The screen then switches to the Timeline tab. */
    fun openDay(date: LocalDate) = selection.select(date)
}
