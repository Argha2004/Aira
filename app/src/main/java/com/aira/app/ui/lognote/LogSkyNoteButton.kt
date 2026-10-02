package com.aira.app.ui.lognote

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.ui.components.WideButton

/**
 * The full-width "Log Sky Note" button. It opens a small dialog for an optional note, then takes a sensor
 * snapshot now; the outcome is shown as a short message.
 */
@Composable
fun LogSkyNoteButton(modifier: Modifier = Modifier, viewModel: LogNoteViewModel = hiltViewModel()) {
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(result) {
        val text = when (val r = result) {
            null -> return@LaunchedEffect
            is LogResult.Saved -> resources.getString(
                when (r.note) {
                    NoteOutcome.NONE -> R.string.log_saved
                    NoteOutcome.ATTACHED -> R.string.log_saved_note
                    NoteOutcome.NO_EVENT_YET -> R.string.log_note_no_event
                },
            )
            is LogResult.Failed -> resources.getString(R.string.log_failed, r.message ?: resources.getString(R.string.error_unknown))
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        viewModel.clearResult()
    }

    WideButton(onClick = { showDialog = true }, enabled = !saving, modifier = modifier) {
        if (saving) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.log_button), style = MaterialTheme.typography.titleMedium)
    }

    if (showDialog) {
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.log_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.log_dialog_body), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.log_note_hint)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    viewModel.logNow(note)
                }) { Text(stringResource(R.string.log_save)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
