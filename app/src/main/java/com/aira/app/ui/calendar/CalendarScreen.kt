package com.aira.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.aira.app.ui.components.LocalOnSky
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.ui.components.pageTextColor
import com.aira.app.ui.components.pageMutedColor
import com.aira.app.domain.engine.DailyStats
import com.aira.app.domain.engine.DayClassifier
import com.aira.app.domain.engine.DayCondition
import com.aira.app.domain.engine.DaySummary
import com.aira.app.domain.engine.DayWeather
import com.aira.app.domain.engine.UvBand
import com.aira.app.domain.engine.HomeBands
import com.aira.app.domain.engine.WindLevel
import com.aira.app.domain.engine.formatMinutesShort
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.ErrorMessage
import com.aira.app.ui.components.IconTile
import com.aira.app.ui.components.LoadingIndicator
import com.aira.app.ui.components.SlideContent
import com.aira.app.ui.components.formatDegrees
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.home.DiaryRow
import com.aira.app.ui.lognote.LogSkyNoteButton
import com.aira.app.ui.theme.AiraTheme
import com.aira.app.ui.theme.HeatCoral
import com.aira.app.ui.theme.RainBlue
import com.aira.app.ui.theme.SunAmber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** Connects the screen to its ViewModel. [onOpenTimeline] switches to the Timeline tab. */
@Composable
fun CalendarRoute(
    onOpenTimeline: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToday = viewModel::goToToday,
        onDayClick = viewModel::select,
        onOpenDay = {
            viewModel.openDay(it)
            onOpenTimeline()
        },
        modifier = modifier,
    )
}

/**
 * Stateless screen: month and navigation, the month grid (tap a day to choose it), the chosen day's card,
 * its logged moments, and the "Log Sky Note" button.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onToday: () -> Unit = {},
    onOpenDay: (LocalDate) -> Unit = {},
    logButton: @Composable () -> Unit = { LogSkyNoteButton() },
) {
    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MonthHeader(state, onPreviousMonth, onNextMonth, onToday)
        when {
            state.isLoading -> LoadingIndicator(Modifier.height(240.dp))
            state.hasError -> ErrorMessage(Modifier.height(240.dp))
            else -> {
                SlideContent(target = state, key = { it.month }, forward = { from, to -> to.month > from.month }) { shown ->
                    AiraCard { MonthGrid(shown, onDayClick) }
                }
                state.selected?.let { day ->
                    DayCard(day, state.summaries[day])
                    LoggedMoments(state.events, onOpenDay = { onOpenDay(day) })
                }
            }
        }
        logButton()
    }
}

// ---- Header ----

@Composable
private fun MonthHeader(state: CalendarUiState, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Row(
            modifier = Modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Smaller arrow buttons leave room for the month name on narrow phones.
            IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month)) }
            Text(
                stringResource(R.string.today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(CircleShape)
                    .clickable(onClick = onToday).padding(horizontal = 10.dp, vertical = 8.dp),
                maxLines = 1,
            )
            IconButton(onClick = onNext, enabled = !state.isCurrentMonth, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
            }
        }
    }
}

// ---- Month grid ----

/** One cell of the grid: a day of this month, or a greyed day of the month before or after. */
private data class Cell(val date: LocalDate, val inMonth: Boolean)

@Composable
private fun MonthGrid(state: CalendarUiState, onDayClick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val first = state.month.atDay(1)
    // Monday first: start on the Monday on or before the 1st, and fill whole weeks.
    val start = first.minusDays((first.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val last = state.month.atEndOfMonth()
    val weeks = ((last.toEpochDay() - start.toEpochDay()) / 7 + 1).toInt()
    val cells = (0 until weeks * 7).map { start.plusDays(it.toLong()) }.map { Cell(it, YearMonth.from(it) == state.month) }

    WeekdayRow()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    DayCell(
                        cell = cell,
                        condition = if (cell.inMonth) state.days[cell.date] ?: DayCondition.NO_DATA else DayCondition.NO_DATA,
                        isToday = cell.date == today,
                        isSelected = cell.inMonth && cell.date == state.selected,
                        hasNote = cell.inMonth && (state.notes[cell.date] ?: 0) > 0,
                        enabled = cell.inMonth && !cell.date.isAfter(today),
                        onClick = { onDayClick(cell.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    if (state.days.isEmpty()) {
        Text(
            stringResource(R.string.calendar_month_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekdayRow() {
    // The composition's locale, so the names follow the phone's language if it changes.
    val locale = LocalLocale.current.platformLocale
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(
    cell: Cell,
    condition: DayCondition,
    isToday: Boolean,
    isSelected: Boolean,
    hasNote: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.aspectRatio(0.9f).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (isToday && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(cell.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = content)
                condition.icon()?.let {
                    // Here the icon is the only thing that says what kind of day it was, so it is described.
                    Icon(
                        it,
                        contentDescription = stringResource(condition.labelRes()),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else condition.tint(),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            if (hasNote) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(5.dp).clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

private fun DayCondition.labelRes(): Int = when (this) {
    DayCondition.SUNNY -> R.string.legend_sunny
    DayCondition.RAINY -> R.string.legend_rainy
    DayCondition.HOT -> R.string.legend_hot
    DayCondition.MOSTLY_INDOORS -> R.string.legend_indoors
    DayCondition.NO_DATA -> R.string.calendar_no_data
}

private fun DayCondition.icon(): ImageVector? = when (this) {
    DayCondition.SUNNY -> Icons.Filled.WbSunny
    DayCondition.RAINY -> Icons.Filled.WaterDrop
    DayCondition.HOT -> Icons.Filled.Thermostat
    DayCondition.MOSTLY_INDOORS -> Icons.Filled.Home
    DayCondition.NO_DATA -> null
}

@Composable
private fun DayCondition.tint(): Color = when (this) {
    DayCondition.SUNNY -> SunAmber
    DayCondition.RAINY -> RainBlue
    DayCondition.HOT -> HeatCoral
    DayCondition.MOSTLY_INDOORS -> if (LocalOnSky.current) Color(0xFFDDE6EE) else Color(0xFF78909C)
    DayCondition.NO_DATA -> Color.Transparent
}

// ---- Chosen day ----

/**
 * The chosen day: its average temperature and kind of day, feels-like with the high and low, then rain
 * encounters, time outdoors, and the "atmosphere" (how windy it was).
 */
@Composable
private fun DayCard(date: LocalDate, summary: DaySummary?) {
    val condition = DayClassifier.classify(summary)
    val weather = summary?.day
    AiraCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        weather?.let { formatDegrees(it.avgTempC) } ?: stringResource(R.string.ins_none),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(condition.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp).weight(1f, fill = false),
                    )
                }
                Text(
                    if (weather == null) {
                        stringResource(R.string.cal_day_empty)
                    } else {
                        stringResource(
                            R.string.cal_feels_line,
                            formatTemperature(weather.avgFeelsLikeC),
                            formatDegrees(weather.maxTempC),
                            formatDegrees(weather.minTempC),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconTile(condition.icon() ?: Icons.Filled.CalendarMonth, condition.icon()?.let { condition.tint() } ?: MaterialTheme.colorScheme.primary, size = 52)
        }
        if (summary != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row {
                DayStat(
                    stringResource(R.string.cal_rain_label),
                    pluralStringResource(R.plurals.cal_times, summary.stats.rainEncounters, summary.stats.rainEncounters),
                    Modifier.weight(1f),
                )
                DayStat(stringResource(R.string.tl_outdoor_time), formatMinutesShort(summary.stats.outdoorMinutes), Modifier.weight(1f))
                DayStat(
                    stringResource(R.string.cal_atmosphere),
                    weather?.let { stringResource(atmosphereText(HomeBands.wind(it.avgWindKmh))) } ?: stringResource(R.string.ins_none),
                    Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun atmosphereText(level: WindLevel): Int = when (level) {
    WindLevel.CALM -> R.string.atm_calm
    WindLevel.GENTLE -> R.string.atm_gentle
    WindLevel.BREEZY -> R.string.atm_breezy
    WindLevel.WINDY -> R.string.atm_windy
}

@Composable
private fun DayStat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LoggedMoments(events: List<DiaryEvent>, onOpenDay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.cal_logged_moments),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = pageTextColor(),
                modifier = Modifier.weight(1f),
            )
            Text(
                pluralStringResource(R.plurals.tl_entries, events.size, events.size),
                style = MaterialTheme.typography.labelMedium,
                color = pageMutedColor(),
            )
        }
        if (events.isEmpty()) {
            Text(stringResource(R.string.cal_no_moments), style = MaterialTheme.typography.bodyMedium, color = pageMutedColor())
        }
        // Tapping a moment opens the day on the Timeline.
        events.forEach { event ->
            DiaryRow(
                event,
                modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onOpenDay)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            )
        }
    }
}

// ---- Previews ----

private fun previewDay(date: LocalDate, sun: Int, rain: Int) = DaySummary(
    date = date,
    stats = DailyStats(sunMinutes = sun, heatMinutes = 0, rainEncounters = rain, uvDoseBand = UvBand.MODERATE, outdoorMinutes = 80, steps = 0),
    avgOutdoorFeelsLike = 28.0,
    hotOutdoorMinutes = 0,
    maxWalkFeelsLike = null,
    day = DayWeather(avgTempC = 28.0, maxTempC = 29.0, minTempC = 21.0, avgFeelsLikeC = 31.0, avgWindKmh = 22.0),
)

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun CalendarPreview() {
    val month = YearMonth.of(2026, 9)
    val summaries = listOf(previewDay(month.atDay(1), 50, 0), previewDay(month.atDay(2), 0, 2), previewDay(month.atDay(3), 10, 0))
    AiraTheme {
        CalendarScreen(
            state = CalendarUiState(
                month = month,
                days = mapOf(
                    month.atDay(1) to DayCondition.SUNNY,
                    month.atDay(2) to DayCondition.RAINY,
                    month.atDay(3) to DayCondition.MOSTLY_INDOORS,
                ),
                isCurrentMonth = false,
                summaries = summaries.associateBy { it.date },
                notes = mapOf(month.atDay(2) to 1),
                selected = month.atDay(2),
                events = listOf(DiaryEvent(1, 1_000_000, 4_000_000, DiaryEventType.RAIN, "Rain while outdoors, 50 min", 24.0)),
            ),
            onPreviousMonth = {}, onNextMonth = {}, onDayClick = {}, logButton = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarLoadingPreview() {
    AiraTheme {
        CalendarScreen(
            state = CalendarUiState(YearMonth.of(2026, 9), emptyMap(), isCurrentMonth = true, isLoading = true),
            onPreviousMonth = {}, onNextMonth = {}, onDayClick = {}, logButton = {},
        )
    }
}
