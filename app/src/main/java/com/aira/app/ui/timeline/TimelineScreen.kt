package com.aira.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.domain.engine.Comfort
import com.aira.app.domain.engine.DailyStats
import com.aira.app.domain.engine.HomeBands
import com.aira.app.domain.engine.Thresholds
import com.aira.app.domain.engine.UvBand
import com.aira.app.domain.engine.formatMinutesShort
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.Note
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.AlertIcon
import com.aira.app.ui.components.ErrorMessage
import com.aira.app.ui.components.EventIcon
import com.aira.app.ui.components.IconTile
import com.aira.app.ui.components.LoadingIndicator
import com.aira.app.ui.components.SlideContent
import com.aira.app.ui.components.eventTitle
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.components.imageVector
import com.aira.app.ui.components.labelRes
import com.aira.app.ui.components.localizeTemperatures
import com.aira.app.ui.components.tint
import com.aira.app.ui.lognote.LogSkyNoteButton
import com.aira.app.ui.theme.AiraTheme
import com.aira.app.ui.theme.HeatCoral
import com.aira.app.ui.theme.LiveGreen
import com.aira.app.ui.theme.RainBlue
import com.aira.app.ui.theme.SunAmber
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/** Connects the screen to its ViewModel. */
@Composable
fun TimelineRoute(modifier: Modifier = Modifier, viewModel: TimelineViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TimelineScreen(
        state = state,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onAddNote = viewModel::addNote,
        modifier = modifier,
    )
}

/**
 * Stateless screen: date bar, the day's summary card, filter chips, the entries on a vertical rail (newest
 * first), and the "Log Sky Note" button at the end.
 */
@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddNote: (eventId: Long, text: String) -> Unit,
    modifier: Modifier = Modifier,
    logButton: @Composable () -> Unit = { LogSkyNoteButton() },
) {
    var noteEventId by remember { mutableStateOf<Long?>(null) }
    var filter by rememberSaveable { mutableStateOf(TimelineFilter.ALL) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DateBar(state.date, state.isToday, onPreviousDay, onNextDay)
        when {
            state.isLoading -> LoadingIndicator()
            state.hasError -> ErrorMessage()
            else -> SlideContent(
                target = state to filter,
                key = { (day, chip) -> day.date to chip },
                // A later day, or a chip further right, is a step forward.
                forward = { (oldDay, oldChip), (newDay, newChip) ->
                    if (newDay.date != oldDay.date) newDay.date > oldDay.date else newChip.ordinal > oldChip.ordinal
                },
            ) { (state, chip) ->
                val shown = state.entries.filter { TimelineFilters.matches(it, chip) }
                // Newest first, like a diary you read from the top.
                val ordered = shown.sortedByDescending { it.time }
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                    item(key = "summary") { SummaryCard(state.isToday, TimelineFilters.summary(state.entries), state.stats) }
                    if (state.entries.isNotEmpty()) item(key = "filters") { FilterRow(state.entries, chip) { filter = it } }
                    if (state.entries.isEmpty()) {
                        item(key = "empty") { EmptyText(stringResource(R.string.timeline_empty)) }
                    } else if (shown.isEmpty()) {
                        item(key = "filter-empty") { EmptyText(stringResource(R.string.tl_filter_empty)) }
                    }
                    itemsIndexed(ordered, key = { _, entry -> entry.key }) { index, entry ->
                        // animateItem: entries slide into their new place when a filter chip changes the list.
                        RailRow(entry, isLast = index == ordered.lastIndex, modifier = Modifier.animateItem()) {
                            when (entry) {
                                is TimelineEntry.Event -> EventCard(entry.item, onAddNote = { noteEventId = entry.item.event.id })
                                is TimelineEntry.Alert -> AlertEntryCard(entry.alert)
                                is TimelineEntry.TaskDone -> TaskDoneCard(entry.task)
                            }
                        }
                    }
                    if (state.isToday) item(key = "log") { Box(Modifier.padding(top = 8.dp)) { logButton() } }
                }
            }
        }
    }

    noteEventId?.let { eventId ->
        AddNoteDialog(
            onDismiss = { noteEventId = null },
            onSave = { text ->
                onAddNote(eventId, text)
                noteEventId = null
            },
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

// ---- Header parts ----

@Composable
private fun DateBar(date: LocalDate, isToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_day)) }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isToday) {
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.today).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day))
        }
    }
}

/**
 * "Today's summary": what kind of day it was and the average temperature, then three tiles: time outdoors,
 * showers (with the time of the first), and how many entries were logged.
 */
@Composable
private fun SummaryCard(isToday: Boolean, summary: DaySummary, stats: DailyStats) {
    AiraCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Filled.Thermostat, MaterialTheme.colorScheme.primary, size = 48)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(if (isToday) R.string.tl_summary_today else R.string.tl_summary_day).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    summary.avgC?.let { stringResource(dayLabel(HomeBands.comfort(it))) } ?: stringResource(R.string.tl_no_temps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            summary.avgC?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatTemperature(it), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.tl_avg_temp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile(
                Icons.Filled.WbSunny, SunAmber, formatMinutesShort(stats.outdoorMinutes), stringResource(R.string.tl_outdoor_time), Modifier.weight(1f),
            )
            val first = summary.firstRainAt
            SummaryTile(
                Icons.Filled.Umbrella, RainBlue,
                pluralStringResource(R.plurals.tl_showers, summary.rainEvents, summary.rainEvents),
                if (first != null) {
                    stringResource(R.string.tl_first_rain, formatMinutesShort(summary.firstRainMinutes), timeText(first))
                } else {
                    stringResource(R.string.tl_no_rain)
                },
                Modifier.weight(1f),
            )
            SummaryTile(
                Icons.Filled.TaskAlt, LiveGreen,
                pluralStringResource(R.plurals.tl_entries, summary.chapters, summary.chapters),
                stringResource(R.string.tl_all_logged),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryTile(icon: ImageVector, tint: Color, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
    }
}

private fun dayLabel(comfort: Comfort): Int = when (comfort) {
    Comfort.COOL -> R.string.tl_day_cool
    Comfort.COMFORTABLE -> R.string.tl_day_mild
    Comfort.WARM -> R.string.tl_day_warm
    Comfort.HOT -> R.string.tl_day_hot
}

@Composable
private fun FilterRow(entries: List<TimelineEntry>, selected: TimelineFilter, onSelect: (TimelineFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimelineFilter.entries.forEach { filter ->
            val count = TimelineFilters.count(entries, filter)
            if (filter != TimelineFilter.ALL && count == 0) return@forEach
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filterLabel(filter), count)) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

private fun filterLabel(filter: TimelineFilter): Int = when (filter) {
    TimelineFilter.ALL -> R.string.tl_filter_all
    TimelineFilter.OUTDOORS -> R.string.tl_filter_outdoors
    TimelineFilter.INDOORS -> R.string.tl_filter_indoors
    TimelineFilter.COMMUTE -> R.string.tl_filter_commute
    TimelineFilter.ALERTS -> R.string.tl_filter_alerts
}

// ---- The rail ----

/** One row of the rail: a round white icon on a vertical line, and the entry's card on the right. */
@Composable
private fun RailRow(entry: TimelineEntry, isLast: Boolean, modifier: Modifier = Modifier, card: @Composable () -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            if (!isLast) Box(Modifier.width(2.dp).fillMaxHeight().background(lineColor))
            Box(
                modifier = Modifier.padding(top = 12.dp).size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, lineColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when (entry) {
                    is TimelineEntry.Event -> Icon(entry.item.event.type.imageVector(), null, tint = entry.item.event.type.tint(), modifier = Modifier.size(18.dp))
                    is TimelineEntry.Alert -> AlertIcon(entry.alert.type, Modifier.size(18.dp))
                    is TimelineEntry.TaskDone -> Icon(Icons.Filled.CheckCircle, null, tint = LiveGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 14.dp)) { card() }
    }
}

// ---- Cards ----

private fun timeText(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

@Composable
private fun TypePill(type: DiaryEventType) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(type.tint().copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EventIcon(type, Modifier.size(14.dp))
        Text(stringResource(type.labelRes()), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** A temperature badge: red when it felt hot, blue otherwise. */
@Composable
private fun TempBadge(celsius: Double) {
    val hot = celsius > Thresholds.HEAT_FEELS_LIKE_C
    Text(
        formatTemperature(celsius),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (hot) HeatCoral else MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(CircleShape)
            .background(if (hot) HeatCoral.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun SmallChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EventCard(item: TimelineItem, onAddNote: () -> Unit) {
    val event = item.event
    val minutes = ((event.endTime - event.startTime) / 60_000).toInt()
    AiraCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypePill(event.type)
            Text(
                "${timeText(event.startTime)} – ${timeText(event.endTime)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (event.avgTemperature != 0.0) TempBadge(event.avgTemperature)
        }
        Text(eventTitle(event.type, event.startTime), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(localizeTemperatures(event.summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        item.notes.forEach { NoteQuote(it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (minutes > 0) SmallChip(Icons.Filled.Schedule, formatMinutesShort(minutes))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAddNote) { Text(stringResource(R.string.tl_add_note)) }
        }
    }
}

/** A note as a quote block with a blue bar on the left. */
@Composable
private fun NoteQuote(note: Note) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.FormatQuote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column {
                Text("\"${note.text}\"", style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.tl_logged_at, timeText(note.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** An alert that fired on this day, on a warm tinted card. */
@Composable
private fun AlertEntryCard(alert: WeatherAlert) {
    AiraCard(containerColor = SunAmber.copy(alpha = 0.12f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tl_alert), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(timeText(alert.time), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(localizeTemperatures(alert.message), style = MaterialTheme.typography.titleSmall)
    }
}

/** A task that was ticked off on this day. */
@Composable
private fun TaskDoneCard(task: WeatherTask) {
    AiraCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tl_task_done), style = MaterialTheme.typography.labelMedium, color = LiveGreen, modifier = Modifier.weight(1f))
            task.completedAt?.let {
                Text(timeText(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(task.title, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun AddNoteDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_note)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.note_hint)) })
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---- Previews ----

private val previewEntries: List<TimelineEntry> = listOf(
    TimelineEntry.Event(
        TimelineItem(
            DiaryEvent(1, 1_790_650_000_000, 1_790_653_000_000, DiaryEventType.SUN, "Outdoors in strong sun, 50 min, 34 °C", 34.0),
            listOf(Note(1, 1, "Forgot my cap", null, 1_790_651_000_000)),
        ),
    ),
    TimelineEntry.Alert(WeatherAlert(1, AlertType.RAIN_SOON, 1_790_653_500_000, "Rain likely around 4 PM", 80.0)),
    TimelineEntry.Event(
        TimelineItem(DiaryEvent(2, 1_790_654_000_000, 1_790_655_500_000, DiaryEventType.RAIN, "Rain while outdoors, 25 min", 28.0), emptyList()),
    ),
    TimelineEntry.TaskDone(WeatherTask(1, "Bring clothes inside", status = TaskStatus.DONE, completedAt = 1_790_656_000_000)),
)

private val previewStats = DailyStats(
    sunMinutes = 50, heatMinutes = 0, rainEncounters = 1, uvDoseBand = UvBand.MODERATE, outdoorMinutes = 80, steps = 3000,
)

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun TimelinePreview() {
    AiraTheme {
        TimelineScreen(
            state = TimelineUiState(LocalDate.of(2026, 9, 29), isToday = true, entries = previewEntries, stats = previewStats),
            onPreviousDay = {}, onNextDay = {}, onAddNote = { _, _ -> }, logButton = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineEmptyPreview() {
    AiraTheme {
        TimelineScreen(
            state = TimelineUiState(LocalDate.of(2026, 9, 28), isToday = false, entries = emptyList()),
            onPreviousDay = {}, onNextDay = {}, onAddNote = { _, _ -> }, logButton = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineLoadingPreview() {
    AiraTheme {
        TimelineScreen(
            state = TimelineUiState(LocalDate.of(2026, 9, 29), isToday = true, entries = emptyList(), isLoading = true),
            onPreviousDay = {}, onNextDay = {}, onAddNote = { _, _ -> }, logButton = {},
        )
    }
}
