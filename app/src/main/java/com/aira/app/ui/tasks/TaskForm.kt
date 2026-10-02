@file:OptIn(ExperimentalMaterial3Api::class)

package com.aira.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aira.app.R
import com.aira.app.domain.engine.DueTimeBuilder
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.WeatherTask
import com.aira.app.ui.components.labelRes
import com.aira.app.ui.theme.AiraTheme
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The add/edit task form (shown in a bottom sheet): title, note, "Remind me when", optional date and time,
 * an outdoor switch and the repeat choice. [task] is null for a new task. The "falling pressure" choice
 * is hidden on phones without a barometer (unless the task being edited already uses it).
 */
@Composable
fun TaskForm(
    task: WeatherTask?,
    hasBarometer: Boolean,
    onSave: (WeatherTask) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val (initialDate, initialTime) = remember(task?.id) { DueTimeBuilder.split(task?.dueTime, zone) }

    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var note by remember(task?.id) { mutableStateOf(task?.note.orEmpty()) }
    var trigger by remember(task?.id) { mutableStateOf(task?.trigger) }
    var date by remember(task?.id) { mutableStateOf(initialDate) }
    var time by remember(task?.id) { mutableStateOf(initialTime) }
    var outdoor by remember(task?.id) { mutableStateOf(task?.isOutdoor ?: false) }
    var repeat by remember(task?.id) { mutableStateOf(task?.repeat ?: TaskRepeat.ONCE) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val triggerChoices = AlertType.entries.filter { hasBarometer || it != AlertType.PRESSURE_DROP || it == task?.trigger }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (task == null) R.string.task_add else R.string.task_edit),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.task_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.task_note)) },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledRow(R.string.task_remind_when) { TriggerMenu(trigger, triggerChoices) { trigger = it } }

        LabeledRow(R.string.task_date) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: stringResource(R.string.task_not_set))
            }
            if (date != null) TextButton(onClick = { date = null }) { Text(stringResource(R.string.task_clear)) }
        }
        LabeledRow(R.string.task_time) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Text(time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: stringResource(R.string.task_not_set))
            }
            if (time != null) TextButton(onClick = { time = null }) { Text(stringResource(R.string.task_clear)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.task_outdoor), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.task_outdoor_hint), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = outdoor, onCheckedChange = { outdoor = it })
        }

        if (trigger != null) {
            Text(stringResource(R.string.task_repeat), style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TaskRepeat.entries.forEachIndexed { index, choice ->
                    SegmentedButton(
                        selected = choice == repeat,
                        onClick = { repeat = choice },
                        shape = SegmentedButtonDefaults.itemShape(index, TaskRepeat.entries.size),
                    ) {
                        Text(stringResource(if (choice == TaskRepeat.ONCE) R.string.task_repeat_once else R.string.task_repeat_every_time))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val dueTime = DueTimeBuilder.build(date, time, System.currentTimeMillis(), zone)
                    onSave(
                        (task ?: WeatherTask(title = "")).copy(
                            title = title.trim(),
                            note = note.trim(),
                            trigger = trigger,
                            dueTime = dueTime,
                            isOutdoor = outdoor && dueTime != null,
                            repeat = if (trigger == null) TaskRepeat.ONCE else repeat,
                        ),
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
            // A task with id 0 is a new one (for example pre-filled from an alert): nothing to delete yet.
            if (task != null && task.id != 0L) {
                OutlinedButton(onClick = { onDelete(task.id) }) { Text(stringResource(R.string.task_delete)) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showDatePicker) {
        // The date picker works in UTC midnights, so convert to and from a plain date.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time?.hour ?: DueTimeBuilder.DEFAULT_HOUR,
            initialMinute = time?.minute ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun LabeledRow(labelRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** A button that opens a menu: None, then one entry per alert type. */
@Composable
private fun TriggerMenu(selected: AlertType?, choices: List<AlertType>, onSelect: (AlertType?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) {
        Text(stringResource(selected?.labelRes() ?: R.string.task_trigger_none))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.task_trigger_none)) },
            onClick = { onSelect(null); open = false },
        )
        choices.forEach { type ->
            DropdownMenuItem(
                text = { Text(stringResource(type.labelRes())) },
                onClick = { onSelect(type); open = false },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskFormNewPreview() {
    AiraTheme { TaskForm(task = null, hasBarometer = false, onSave = {}, onDelete = {}) }
}

@Preview(showBackground = true)
@Composable
private fun TaskFormEditPreview() {
    AiraTheme {
        TaskForm(
            task = WeatherTask(
                id = 5, title = "Bring clothes inside", trigger = AlertType.RAIN_SOON,
                dueTime = 1_790_660_000_000, isOutdoor = false, repeat = TaskRepeat.EVERY_TIME,
            ),
            hasBarometer = true,
            onSave = {},
            onDelete = {},
        )
    }
}
