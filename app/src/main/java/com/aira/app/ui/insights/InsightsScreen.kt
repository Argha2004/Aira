package com.aira.app.ui.insights

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.domain.engine.ChartData
import com.aira.app.domain.engine.PeriodReflection
import com.aira.app.domain.engine.PeriodReflections
import com.aira.app.domain.engine.formatMinutes
import com.aira.app.domain.engine.formatMinutesShort
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.ErrorMessage
import com.aira.app.ui.components.ExposureTile
import com.aira.app.ui.components.LoadingIndicator
import com.aira.app.ui.components.SectionLabel
import com.aira.app.ui.components.SlideContent
import com.aira.app.ui.components.StatCard
import com.aira.app.ui.components.WideButton
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.components.localizeTemperatures
import com.aira.app.ui.theme.AiraTheme
import com.aira.app.ui.theme.HeatCoral
import com.aira.app.ui.theme.LiveGreen
import com.aira.app.ui.theme.RainBlue
import com.aira.app.ui.theme.SunAmber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SUN_GOAL_MINUTES = 60
private const val HEAT_CAP_MINUTES = 30

/** Connects the screen to its ViewModel. */
@Composable
fun InsightsRoute(modifier: Modifier = Modifier, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onRangeChange = viewModel::setRange,
        onPrevious = viewModel::previousPeriod,
        onNext = viewModel::nextPeriod,
        modifier = modifier,
    )
}

/**
 * Stateless screen: Week / Month / Year with the dates (arrows step back through earlier periods), the
 * conditions line, four stat cards, the exposure summary, the sunlight and rain charts, the diary insights,
 * and "Share summary".
 */
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onRangeChange: (InsightRange) -> Unit,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RangeBar(state, onRangeChange, onPrevious, onNext)
        val chart = state.chart
        val reflection = state.reflection
        when {
            state.isLoading -> LoadingIndicator()
            state.hasError -> ErrorMessage()
            chart == null || reflection == null -> Unit
            else -> SlideContent(
                target = state,
                key = { it.range to it.offset },
                // Week → Month → Year is forward; so is going to a later period (a smaller offset).
                forward = { from, to -> if (to.range != from.range) to.range > from.range else to.offset < from.offset },
            ) { shown ->
                val shownChart = shown.chart
                val shownReflection = shown.reflection
                if (shownChart != null && shownReflection != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatCards(shown.range, shownReflection)
                        ExposureSummary(shown.range, shownReflection)
                        SunChart(shown.range, shownChart, shownReflection)
                        RainChart(shown.range, shownChart, shownReflection)
                        ShareButton(shown.range, shownChart, shownReflection, shown.sentences)
                    }
                }
            }
        }
    }
}

// ---- Header ----

@Composable
private fun RangeBar(state: InsightsUiState, onChange: (InsightRange) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // A pill with the three choices; the chosen one is filled blue.
        Row(
            modifier = Modifier.weight(1f).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)).padding(4.dp),
        ) {
            InsightRange.entries.forEach { range ->
                val selected = range == state.range
                Text(
                    stringResource(rangeLabel(range)),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onChange(range) }.padding(vertical = 8.dp),
                )
            }
        }
        val dates = state.chart?.dates
        Row(
            modifier = Modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.ins_previous))
            }
            if (!dates.isNullOrEmpty()) Text(periodText(state.range, dates), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            IconButton(onClick = onNext, enabled = state.offset > 0, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.ins_next))
            }
        }
    }
}

private fun rangeLabel(range: InsightRange): Int = when (range) {
    InsightRange.WEEK -> R.string.range_week
    InsightRange.MONTH -> R.string.range_month
    InsightRange.YEAR -> R.string.range_year
}

/** "Sep 23 – Sep 29" for days, "Oct 2025 – Sep 2026" for the year view. */
private fun periodText(range: InsightRange, dates: List<LocalDate>): String {
    val format = DateTimeFormatter.ofPattern(if (range == InsightRange.YEAR) "MMM yy" else "MMM d")
    return "${dates.first().format(format)} – ${dates.last().format(format)}"
}

// ---- Stat cards ----

@Composable
private fun StatCards(range: InsightRange, r: PeriodReflection) {
    val before = stringResource(previousLabel(range))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.ins_daylight_avg),
                icon = Icons.Filled.WbSunny,
                tint = SunAmber,
                value = formatMinutesShort(r.avgSunMinutes),
                caption = changeText(r.sunChangePercent, before),
                captionColor = changeColor(r.sunChangePercent),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.tl_outdoor_time),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                tint = MaterialTheme.colorScheme.primary,
                value = formatMinutesShort(r.outdoorMinutes),
                caption = changeText(r.outdoorChangePercent, before),
                captionColor = changeColor(r.outdoorChangePercent),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.cal_rain_label),
                icon = Icons.Filled.Umbrella,
                tint = RainBlue,
                value = stringResource(R.string.exp_times, r.rainEncounters),
                caption = pluralStringResource(if (range == InsightRange.YEAR) R.plurals.ins_rain_months else R.plurals.ins_rain_days, r.rainDays, r.rainDays),
                modifier = Modifier.weight(1f),
            )
            val warmest = r.warmestFeelsLike
            val date = r.warmestDate
            StatCard(
                label = stringResource(R.string.ins_thermal_peak),
                icon = Icons.Filled.Thermostat,
                tint = HeatCoral,
                value = warmest?.let { formatTemperature(it) } ?: stringResource(R.string.ins_none),
                caption = date?.format(DateTimeFormatter.ofPattern(if (range == InsightRange.YEAR) "MMMM yyyy" else "EEEE, MMM d")) ?: "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun previousLabel(range: InsightRange): Int = when (range) {
    InsightRange.WEEK -> R.string.ins_last_week
    InsightRange.MONTH -> R.string.ins_last_month
    InsightRange.YEAR -> R.string.ins_last_year
}

/** "+15% vs last week", or "No earlier data". */
@Composable
private fun changeText(percent: Int?, before: String): String =
    if (percent == null) stringResource(R.string.ins_no_earlier) else stringResource(R.string.ins_change, percent, before)

@Composable
private fun changeColor(percent: Int?): Color = when {
    percent == null -> MaterialTheme.colorScheme.onSurfaceVariant
    percent >= 0 -> LiveGreen
    else -> HeatCoral
}

// ---- Exposure summary ----

@Composable
private fun ExposureSummary(range: InsightRange, r: PeriodReflection) {
    AiraCard {
        SectionLabel(stringResource(R.string.ins_exposure_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposureTile(
                label = stringResource(R.string.exp_daylight),
                value = stringResource(R.string.exp_minutes_short, r.avgSunMinutes),
                caption = stringResource(R.string.exp_goal, SUN_GOAL_MINUTES),
                progressValue = r.avgSunMinutes,
                goal = SUN_GOAL_MINUTES,
                color = SunAmber,
                modifier = Modifier.weight(1f),
            )
            ExposureTile(
                label = stringResource(R.string.exp_heat),
                value = stringResource(R.string.exp_minutes_short, r.avgHeatMinutes),
                caption = stringResource(R.string.exp_cap, HEAT_CAP_MINUTES),
                progressValue = r.avgHeatMinutes,
                goal = HEAT_CAP_MINUTES,
                color = HeatCoral,
                modifier = Modifier.weight(1f),
            )
            ExposureTile(
                label = stringResource(R.string.exp_rain),
                value = stringResource(R.string.exp_times, r.rainEncounters),
                caption = stringResource(R.string.ins_in_total),
                progressValue = r.rainEncounters,
                goal = maxOf(r.rainEncounters, 1),
                color = RainBlue,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.ins_total_outdoor, stringResource(periodWord(range)), formatMinutesShort(r.outdoorMinutes)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            r.outdoorChangePercent?.let {
                Text(
                    stringResource(R.string.ins_change, it, stringResource(previousLabel(range))),
                    style = MaterialTheme.typography.labelLarge,
                    color = changeColor(it),
                )
            }
        }
    }
}

private fun periodWord(range: InsightRange): Int = when (range) {
    InsightRange.WEEK -> R.string.ins_this_week
    InsightRange.MONTH -> R.string.ins_this_month
    InsightRange.YEAR -> R.string.ins_this_year
}

// ---- Charts ----

/** The label under each bar: weekday letter (week), day of the month every 5th day (month), month letter (year). */
private fun barLabels(range: InsightRange, dates: List<LocalDate>): List<String> = dates.mapIndexed { index, date ->
    when (range) {
        InsightRange.WEEK -> date.format(DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()))
        InsightRange.MONTH -> if (index % 5 == 0) date.dayOfMonth.toString() else ""
        InsightRange.YEAR -> date.format(DateTimeFormatter.ofPattern("MMMMM", Locale.getDefault()))
    }
}

/** A chart card: coloured tag and title on the left, a big number with a caption on the right. */
@Composable
private fun ChartCard(tag: String, title: String, tint: Color, value: String, valueCaption: String, content: @Composable () -> Unit) {
    AiraCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.uppercase(), style = MaterialTheme.typography.labelMedium, color = tint)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = tint)
                Text(valueCaption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun SunChart(range: InsightRange, chart: ChartData, r: PeriodReflection) {
    val year = range == InsightRange.YEAR
    ChartCard(
        tag = stringResource(rhythmsTag(range)),
        title = stringResource(if (year) R.string.ins_sun_title_year else R.string.ins_sun_title),
        tint = MaterialTheme.colorScheme.primary,
        value = formatMinutesShort(r.avgSunMinutes),
        valueCaption = stringResource(R.string.ins_daily_avg),
    ) {
        BarChart(
            values = chart.sunMinutes,
            labels = barLabels(range, chart.dates),
            color = MaterialTheme.colorScheme.primary,
            // Days that reached the daylight goal are drawn strong; in the year view, the sunniest month.
            highlight = if (year) { v -> v > 0 && v == chart.sunMinutes.max() } else { v -> v >= SUN_GOAL_MINUTES },
            showValues = chart.sunMinutes.size <= 12,
            target = if (year) null else SUN_GOAL_MINUTES,
            valueSuffix = stringResource(R.string.unit_minutes_short),
            targetLabel = stringResource(R.string.ins_target, SUN_GOAL_MINUTES),
        )
        if (!year) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ins_sun_target), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RainChart(range: InsightRange, chart: ChartData, r: PeriodReflection) {
    ChartCard(
        tag = stringResource(R.string.ins_rain_tag),
        title = stringResource(R.string.ins_rain_title),
        tint = RainBlue,
        value = pluralStringResource(R.plurals.ins_total_value, r.rainEncounters, r.rainEncounters),
        valueCaption = stringResource(R.string.ins_events),
    ) {
        val most = chart.rainEncounters.maxOrNull() ?: 0
        BarChart(
            values = chart.rainEncounters,
            labels = barLabels(range, chart.dates),
            color = RainBlue,
            highlight = { it > 0 && it == most },
            showValues = chart.rainEncounters.size <= 12,
            height = 110.dp,
        )
    }
}

// ---- Share ----

/** Shares the period's numbers as a short text through the phone's share sheet. */
@Composable
private fun ShareButton(range: InsightRange, chart: ChartData, r: PeriodReflection, sentences: List<String>) {
    val context = LocalContext.current
    val text = buildString {
        appendLine(stringResource(R.string.ins_share_heading, periodText(range, chart.dates)))
        appendLine(stringResource(R.string.ins_share_sun, formatMinutes(r.avgSunMinutes)))
        appendLine(stringResource(R.string.ins_share_outdoor, formatMinutes(r.outdoorMinutes)))
        appendLine(stringResource(R.string.ins_share_rain, r.rainEncounters))
        r.warmestFeelsLike?.let { appendLine(stringResource(R.string.ins_share_peak, formatTemperature(it))) }
        sentences.forEach { appendLine(localizeTemperatures(it)) }
        append(stringResource(R.string.year_app_name))
    }
    val chooser = stringResource(R.string.ins_share_chooser)
    WideButton(onClick = {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, chooser))
    }) {
        Icon(Icons.Filled.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(shareLabel(range)), style = MaterialTheme.typography.titleMedium)
    }
}

private fun rhythmsTag(range: InsightRange): Int = when (range) {
    InsightRange.WEEK -> R.string.ins_rhythms_week
    InsightRange.MONTH -> R.string.ins_rhythms_month
    InsightRange.YEAR -> R.string.ins_rhythms_year
}

private fun shareLabel(range: InsightRange): Int = when (range) {
    InsightRange.WEEK -> R.string.ins_share_week
    InsightRange.MONTH -> R.string.ins_share_month
    InsightRange.YEAR -> R.string.ins_share_year
}

// ---- Previews ----

private val previewChart = ChartData(
    dates = (0..6).map { LocalDate.of(2026, 9, 23).plusDays(it.toLong()) },
    sunMinutes = listOf(45, 62, 35, 50, 70, 40, 42),
    rainEncounters = listOf(0, 1, 0, 3, 1, 0, 2),
    avgFeelsLike = listOf(28.0, 36.0, 29.0, 31.0, 32.0, 27.0, 30.0),
    outdoorMinutes = listOf(60, 70, 40, 50, 80, 30, 40),
    heatMinutes = listOf(0, 30, 0, 10, 20, 0, 0),
)

@Preview(showBackground = true)
@Composable
private fun InsightsLoadingPreview() {
    AiraTheme { InsightsScreen(state = InsightsUiState(isLoading = true), onRangeChange = {}) }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun InsightsPreview() {
    AiraTheme {
        InsightsScreen(
            state = InsightsUiState(
                isLoading = false,
                chart = previewChart,
                reflection = PeriodReflections.of(previewChart, previewChart.copy(sunMinutes = listOf(30, 40, 30, 40, 50, 40, 30))),
                sentences = listOf("It rained on 5 of your 18 commutes this month."),
            ),
            onRangeChange = {},
        )
    }
}
