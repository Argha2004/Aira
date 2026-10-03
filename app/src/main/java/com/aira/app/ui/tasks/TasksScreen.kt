@file:OptIn(ExperimentalMaterial3Api::class)

package com.aira.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.ui.components.LocalOnSky
import com.aira.app.ui.components.PlainTheme
import com.aira.app.ui.components.pageTextColor
import com.aira.app.ui.components.pageMutedColor
import com.aira.app.domain.engine.ChangeKind
import com.aira.app.domain.engine.TaskGroups
import com.aira.app.domain.engine.Thresholds
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherTask
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.ErrorMessage
import com.aira.app.ui.components.LoadingIndicator
import com.aira.app.ui.components.SlideContent
import com.aira.app.ui.components.WideButton
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.components.localizeTemperatures
import com.aira.app.ui.theme.AiraTheme
import com.aira.app.ui.theme.HeatCoral
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/** Everything the user can do on the Tasks tab. */
data class TasksActions(
    val onMarkDone: (taskId: Long) -> Unit,
    val onReopen: (taskId: Long) -> Unit,
    val onSave: (WeatherTask) -> Unit,
    val onDelete: (taskId: Long) -> Unit,
)

/** The filter chips above the lists. */
private enum class TaskFilter { ALL, WEATHER, TODAY, UPCOMING, DONE }

/** Connects the screen to its ViewModel. */
@Composable
fun TasksRoute(modifier: Modifier = Modifier, viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TasksScreen(
        state = state,
        actions = TasksActions(
            onMarkDone = viewModel::markDone,
            onReopen = viewModel::reopen,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        ),
        modifier = modifier,
    )
}

/**
 * Stateless screen: "Active triggers" and "Next due" cards, filter chips, the weather-linked routines,
 * today's and upcoming tasks, a collapsible "Completed" list, and the "Create weather task" button that
 * opens the add/edit sheet. Tap a task to edit it.
 */
@Composable
fun TasksScreen(state: TasksUiState, actions: TasksActions, modifier: Modifier = Modifier) {
    // The task being edited, or null for a new task. Only used while the sheet is open.
    var sheetTask by remember { mutableStateOf<WeatherTask?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var doneExpanded by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }
    val edit: (WeatherTask?) -> Unit = { sheetTask = it; showSheet = true }

    if (state.isLoading) {
        LoadingIndicator(modifier)
        return
    }
    if (state.hasError) {
        ErrorMessage(modifier)
        return
    }

    val groups = state.groups
    Column(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopCards(state)
        FilterRow(state, filter) { filter = it }
    }
    // Another chip slides the list: a chip further right comes in from the right.
    SlideContent(target = filter, forward = { from, to -> to.ordinal > from.ordinal }) { filter ->
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (filter) {
            TaskFilter.ALL -> {
                if (groups.isEmpty) item(key = "empty") { EmptyText(R.string.tasks_empty) }
                taskSection("linked", R.string.tk_linked, R.string.tk_automated, groups.linkedToAlerts, actions, edit)
                taskSection(
                    "today", R.string.tk_today, null, groups.today, actions, edit,
                    trailingCount = groups.today.size,
                )
                taskSection("upcoming", R.string.tk_upcoming, null, groups.upcoming, actions, edit)
                if (state.done.isNotEmpty()) {
                    item(key = "done-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                                .clickable { doneExpanded = !doneExpanded }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.tk_completed, state.done.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = pageTextColor(),
                                modifier = Modifier.weight(1f),
                            )
                            Icon(if (doneExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = pageTextColor())
                        }
                    }
                    if (doneExpanded) taskItems("done", state.done, actions, edit)
                }
            }
            TaskFilter.WEATHER -> filtered(groups.linkedToAlerts, actions, edit)
            TaskFilter.TODAY -> filtered(groups.today, actions, edit)
            TaskFilter.UPCOMING -> filtered(groups.upcoming, actions, edit)
            TaskFilter.DONE -> filtered(state.done, actions, edit, prefix = "done")
        }

        item(key = "create") {
            WideButton(onClick = { edit(null) }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tk_new), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    }
    }

    if (showSheet) {
        PlainTheme {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            TaskForm(
                task = sheetTask,
                hasBarometer = state.hasBarometer,
                onSave = { actions.onSave(it); showSheet = false },
                onDelete = { id -> actions.onDelete(id); showSheet = false },
            )
        }
        }
    }
}

/**
 * A section title (with an optional note or "N remaining" on the right) and its tasks. The note is passed as a
 * string resource, because text can only be looked up inside an item, not while the list is being built.
 */
private fun LazyListScope.taskSection(
    key: String,
    titleRes: Int,
    noteRes: Int?,
    tasks: List<WeatherTask>,
    actions: TasksActions,
    onEdit: (WeatherTask?) -> Unit,
    trailingCount: Int? = null,
) {
    if (tasks.isEmpty()) return
    item(key = "header-$key") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                stringResource(titleRes).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = pageMutedColor(),
                modifier = Modifier.weight(1f),
            )
            val right = noteRes?.let { stringResource(it) } ?: trailingCount?.let { pluralStringResource(R.plurals.tk_remaining, it, it) }
            if (right != null) {
                Text(
                    right,
                    style = MaterialTheme.typography.labelMedium,
                    color = pageMutedColor(),
                )
            }
        }
    }
    taskItems(key, tasks, actions, onEdit)
}

private fun LazyListScope.taskItems(key: String, tasks: List<WeatherTask>, actions: TasksActions, onEdit: (WeatherTask?) -> Unit) {
    items(tasks, key = { "$key-${it.id}" }) { task ->
        val done = task.status == TaskStatus.DONE
        TaskCard(
            task,
            onToggle = { if (done) actions.onReopen(task.id) else actions.onMarkDone(task.id) },
            onEdit = { onEdit(task) },
            modifier = Modifier.animateItem(),
        )
    }
}

private fun LazyListScope.filtered(tasks: List<WeatherTask>, actions: TasksActions, onEdit: (WeatherTask?) -> Unit, prefix: String = "task") {
    if (tasks.isEmpty()) item(key = "empty") { EmptyText(R.string.tk_filter_empty) }
    taskItems(prefix, tasks, actions, onEdit)
}

@Composable
private fun EmptyText(textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = pageMutedColor())
}

private fun timeText(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

// ---- Top cards and filters ----

/** One plain card: how many alerts are live, and the next weather change (or the next task that is due). */
@Composable
private fun TopCards(state: TasksUiState) {
    val change = state.nextChange
    val next = state.nextDue
    AiraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SummaryValue(
                label = stringResource(R.string.tk_active_triggers),
                value = pluralStringResource(R.plurals.tk_live, state.activeAlerts, state.activeAlerts),
                caption = stringResource(if (state.activeAlerts > 0) R.string.tk_alerts_on else R.string.tk_all_quiet),
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(1.dp).height(44.dp).background(MaterialTheme.colorScheme.outlineVariant))
            SummaryValue(
                label = stringResource(R.string.tk_next_change),
                value = change?.let { timeText(it.time) } ?: next?.dueTime?.let { timeText(it) } ?: stringResource(R.string.ins_none),
                caption = when {
                    change != null -> stringResource(if (change.kind == ChangeKind.RAIN) R.string.tk_change_rain else R.string.tk_change_heat)
                    next != null -> next.title
                    else -> stringResource(R.string.tk_all_clear)
                },
                modifier = Modifier.weight(1f).padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, style = MaterialTheme.typography.labelMedium, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilterRow(state: TasksUiState, selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    val groups = state.groups
    val counts = mapOf(
        TaskFilter.ALL to groups.linkedToAlerts.size + groups.today.size + groups.upcoming.size,
        TaskFilter.WEATHER to groups.linkedToAlerts.size,
        TaskFilter.TODAY to groups.today.size,
        TaskFilter.UPCOMING to groups.upcoming.size,
        TaskFilter.DONE to state.done.size,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        TaskFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filterLabel(filter), counts.getValue(filter))) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    // On the sky the chosen chip is white, so its text is dark.
                    selectedLabelColor = if (LocalOnSky.current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

private fun filterLabel(filter: TaskFilter): Int = when (filter) {
    TaskFilter.ALL -> R.string.tk_filter_all
    TaskFilter.WEATHER -> R.string.tk_filter_weather
    TaskFilter.TODAY -> R.string.tk_filter_today
    TaskFilter.UPCOMING -> R.string.tk_filter_upcoming
    TaskFilter.DONE -> R.string.tk_filter_done
}

// ---- Task card ----

/**
 * One task: checkbox, title, the due time (red when overdue), and one grey line with its trigger, note, outdoor
 * and repeat.
 */
@Composable
private fun TaskCard(task: WeatherTask, onToggle: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val done = task.status == TaskStatus.DONE
    AiraCard(modifier = modifier.clickable(onClick = onEdit)) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked = done, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    task.dueTime?.takeIf { !done }?.let { due -> TimeBadge(due, before = task.trigger != null) }
                }
                if (done) {
                    task.completedAt?.let {
                        Text(
                            stringResource(R.string.tk_completed_at, timeText(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val details = taskDetails(task)
                    if (details.isNotEmpty()) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (task.status == TaskStatus.SNOOZED && task.snoozedUntil != null) {
                        Text(
                            stringResource(R.string.task_snoozed_until, timeText(task.snoozedUntil)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The due time: today it shows the time, another day the date and time; red when it has passed. A task linked
 * to an alert reads "Before 3:45 PM".
 */
@Composable
private fun TimeBadge(due: Long, before: Boolean) {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val isToday = Instant.ofEpochMilli(due).atZone(zone).toLocalDate() == LocalDate.now(zone)
    val color = when {
        due < now -> HeatCoral
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val time = if (isToday) timeText(due) else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(due))
    Text(
        if (before) stringResource(R.string.tk_before, time) else time,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
    )
}

/** The rule behind an alert type, as on the task chip: "Rain > 60%", "Heat > 38°C". */
@Composable
private fun triggerRule(type: AlertType): String = when (type) {
    AlertType.RAIN_SOON -> stringResource(R.string.rule_rain_soon, Thresholds.RAIN_SOON_PROBABILITY)
    AlertType.RAIN_NOW -> stringResource(R.string.rule_rain_now)
    AlertType.HEAT -> stringResource(R.string.rule_heat, formatTemperature(Thresholds.HEAT_ALERT_C))
    AlertType.STRONG_SUN -> stringResource(R.string.rule_sun, Thresholds.STRONG_SUN_ALERT_MINUTES.toInt())
    AlertType.PRESSURE_DROP -> stringResource(R.string.rule_pressure, Thresholds.PRESSURE_DROP_HPA.toInt())
}

/** The task's details in one line: "Rain > 60% · Balcony · Outdoor · Every time". */
@Composable
private fun taskDetails(task: WeatherTask): String = listOfNotNull(
    task.trigger?.let { stringResource(R.string.tk_trigger, triggerRule(it)) },
    task.note.takeIf { it.isNotBlank() }?.let { localizeTemperatures(it) },
    stringResource(R.string.tk_outdoor).takeIf { task.isOutdoor },
    stringResource(R.string.tk_every_time).takeIf { task.repeat == TaskRepeat.EVERY_TIME },
).joinToString(" · ")

// ---- Previews ----

private val previewTasks = TaskGroups(
    linkedToAlerts = listOf(
        WeatherTask(1, "Bring clothes inside", note = "Balcony", trigger = AlertType.RAIN_SOON, isOutdoor = true, createdAt = 1),
    ),
    today = listOf(
        WeatherTask(2, "Go to market", dueTime = System.currentTimeMillis() + 3_600_000, isOutdoor = true, createdAt = 2),
    ),
    upcoming = emptyList(),
)

private val noActions = TasksActions({}, {}, {}, {})

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun TasksPreview() {
    AiraTheme {
        TasksScreen(
            state = TasksUiState(
                activeAlerts = 2,
                groups = previewTasks,
                done = listOf(WeatherTask(3, "Apply sunscreen", status = TaskStatus.DONE, completedAt = 1)),
                nextDue = previewTasks.today.first(),
            ),
            actions = noActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksEmptyPreview() {
    AiraTheme { TasksScreen(state = TasksUiState(), actions = noActions) }
}

@Preview(showBackground = true)
@Composable
private fun TasksLoadingPreview() {
    AiraTheme { Box { TasksScreen(state = TasksUiState(isLoading = true), actions = noActions) } }
}
