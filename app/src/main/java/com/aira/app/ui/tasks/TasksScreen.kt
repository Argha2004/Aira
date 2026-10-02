@file:OptIn(ExperimentalMaterial3Api::class)

package com.aira.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.domain.engine.ChangeKind
import com.aira.app.domain.engine.TaskGroups
import com.aira.app.domain.engine.Thresholds
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherTask
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.AlertIcon
import com.aira.app.ui.components.ErrorMessage
import com.aira.app.ui.components.LoadingIndicator
import com.aira.app.ui.components.SlideContent
import com.aira.app.ui.components.StatCard
import com.aira.app.ui.components.WideButton
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.components.localizeTemperatures
import com.aira.app.ui.theme.AiraTheme
import com.aira.app.ui.theme.HeatCoral
import com.aira.app.ui.theme.LiveGreen
import com.aira.app.ui.theme.SunAmber
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
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                                modifier = Modifier.weight(1f),
                            )
                            Icon(if (doneExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
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
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val right = noteRes?.let { stringResource(it) } ?: trailingCount?.let { pluralStringResource(R.plurals.tk_remaining, it, it) }
            if (right != null) {
                Text(
                    right,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (noteRes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun timeText(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

// ---- Top cards and filters ----

@Composable
private fun TopCards(state: TasksUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.tk_active_triggers),
            icon = Icons.Filled.Bolt,
            tint = MaterialTheme.colorScheme.primary,
            value = pluralStringResource(R.plurals.tk_live, state.activeAlerts, state.activeAlerts),
            caption = "",
            note = stringResource(if (state.activeAlerts > 0) R.string.tk_alerts_on else R.string.tk_all_quiet),
            noteColor = if (state.activeAlerts > 0) LiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        // The next weather change from the forecast; otherwise the next task that is due.
        val change = state.nextChange
        val next = state.nextDue
        StatCard(
            label = stringResource(R.string.tk_next_change),
            icon = Icons.Filled.Schedule,
            tint = SunAmber,
            value = change?.let { timeText(it.time) } ?: next?.dueTime?.let { timeText(it) } ?: stringResource(R.string.ins_none),
            caption = "",
            note = when {
                change != null -> stringResource(if (change.kind == ChangeKind.RAIN) R.string.tk_change_rain else R.string.tk_change_heat)
                next != null -> next.title
                else -> stringResource(R.string.tk_all_clear)
            },
            noteColor = SunAmber,
            modifier = Modifier.weight(1f),
        )
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

private fun filterLabel(filter: TaskFilter): Int = when (filter) {
    TaskFilter.ALL -> R.string.tk_filter_all
    TaskFilter.WEATHER -> R.string.tk_filter_weather
    TaskFilter.TODAY -> R.string.tk_filter_today
    TaskFilter.UPCOMING -> R.string.tk_filter_upcoming
    TaskFilter.DONE -> R.string.tk_filter_done
}

// ---- Task card ----

/**
 * One task: checkbox, title, a time badge (red when overdue, amber when due today, blue later), chips for
 * its trigger, outdoor and repeat, and the note.
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        task.trigger?.let { type ->
                            TaskChip(stringResource(R.string.tk_trigger, triggerRule(type)), MaterialTheme.colorScheme.primaryContainer) {
                                AlertIcon(type, Modifier.size(14.dp))
                            }
                        }
                        if (task.note.isNotBlank()) {
                            TaskChip(localizeTemperatures(task.note), MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (task.isOutdoor) {
                            TaskChip(stringResource(R.string.tk_outdoor), LiveGreen.copy(alpha = 0.14f)) {
                                Icon(Icons.Filled.Park, null, tint = LiveGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (task.repeat == TaskRepeat.EVERY_TIME) {
                            TaskChip(stringResource(R.string.tk_every_time), MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Filled.Repeat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
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
        isToday -> SunAmber
        else -> MaterialTheme.colorScheme.primary
    }
    val time = if (isToday) timeText(due) else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(due))
    Text(
        if (before) stringResource(R.string.tk_before, time) else time,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
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

@Composable
private fun TaskChip(text: String, background: Color, icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(background).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

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
