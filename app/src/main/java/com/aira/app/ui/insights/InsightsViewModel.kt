package com.aira.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.domain.engine.ChartData
import com.aira.app.domain.engine.DayAggregator
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.engine.DaySummary
import com.aira.app.domain.engine.InsightGenerator
import com.aira.app.domain.engine.PeriodReflection
import com.aira.app.domain.engine.PeriodReflections
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** What the charts show: the last 7 days, the last 30 days, or the last 12 months. */
enum class InsightRange { WEEK, MONTH, YEAR }

data class InsightsUiState(
    val range: InsightRange = InsightRange.WEEK,
    /** 0 = the period ending today; 1 = the one before it; and so on (the arrows next to the dates). */
    val offset: Int = 0,
    val chart: ChartData? = null,
    /** This period compared with the one before it. */
    val reflection: PeriodReflection? = null,
    /** Plain-language insights, e.g. "It rained on 5 of your 18 commutes this month". */
    val sentences: List<String> = emptyList(),
    /** True until the first data has been worked out. */
    val isLoading: Boolean = true,
    /** True if the data could not be read. */
    val hasError: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    snapshots: SnapshotRepository,
    diary: DiaryRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val selection = MutableStateFlow(InsightRange.WEEK to 0)

    val uiState: StateFlow<InsightsUiState> = selection.flatMapLatest { (range, offset) ->
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        // Enough history for the shown period and the one before it (for the "vs before" numbers).
        val from = when (range) {
            InsightRange.WEEK -> today.minusDays(7L * (offset + 2))
            InsightRange.MONTH -> today.minusDays(30L * (offset + 2))
            InsightRange.YEAR -> month.minusMonths(12L * (offset + 2)).atDay(1)
        }
        val fromMillis = DayRange.of(from, zone).first
        val toMillis = DayRange.of(today, zone).last

        combine(
            snapshots.observeBetween(fromMillis, toMillis).map { DayAggregator.summarize(it, zone) },
            diary.observeCommutes(DayRange.of(month.atDay(1), zone).first, DayRange.of(month.atEndOfMonth(), zone).last),
        ) { summaries, commutes ->
            val (chart, previous) = when (range) {
                InsightRange.WEEK -> lastDays(summaries, today, 7, offset)
                InsightRange.MONTH -> lastDays(summaries, today, 30, offset)
                InsightRange.YEAR -> {
                    val end = month.minusMonths(12L * offset)
                    ChartData.lastMonths(summaries, end, 12) to ChartData.lastMonths(summaries, end.minusMonths(12), 12)
                }
            }
            InsightsUiState(
                range = range,
                offset = offset,
                chart = chart,
                reflection = PeriodReflections.of(chart, previous),
                sentences = if (offset == 0) InsightGenerator.generate(summaries, commutes, today) else emptyList(),
                isLoading = false,
            )
        }
            // Up to two years of snapshots are summed up here: do it on a background thread, not on the main
            // (screen) thread, and again each time a new snapshot arrives.
            .flowOn(Dispatchers.Default)
            .catch { emit(InsightsUiState(range = range, offset = offset, isLoading = false, hasError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    /** This period of [days] days ([offset] periods back from today) and the period just before it. */
    private fun lastDays(summaries: List<DaySummary>, today: LocalDate, days: Int, offset: Int) =
        today.minusDays(days.toLong() * offset).let { end ->
            ChartData.lastDays(summaries, end, days) to ChartData.lastDays(summaries, end.minusDays(days.toLong()), days)
        }

    fun setRange(newRange: InsightRange) {
        selection.value = newRange to 0
    }

    /** The left arrow: one period further back. */
    fun previousPeriod() = selection.update { (range, offset) -> range to offset + 1 }

    /** The right arrow: one period closer to today (not past today). */
    fun nextPeriod() = selection.update { (range, offset) -> range to (offset - 1).coerceAtLeast(0) }
}
