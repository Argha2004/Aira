package com.aira.app.ui.lognote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.repository.DiaryRepository
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.model.Note
import com.aira.app.domain.usecase.TakeSnapshotUseCase
import com.aira.app.domain.usecase.runCatchingCancellable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** How the "Log Sky Note" button ended. */
sealed interface LogResult {
    /** The snapshot is saved. [note] says what happened to the note the user typed (if any). */
    data class Saved(val note: NoteOutcome) : LogResult
    data class Failed(val message: String?) : LogResult
}

enum class NoteOutcome { NONE, ATTACHED, NO_EVENT_YET }

/**
 * Behind the "Log Sky Note" button on Home, Timeline and Calendar: takes a snapshot right now, updates
 * today's diary, and attaches the user's note (if any) to the newest event of today.
 */
@HiltViewModel
class LogNoteViewModel @Inject constructor(
    private val takeSnapshot: TakeSnapshotUseCase,
    private val diary: DiaryRepository,
) : ViewModel() {

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _result = MutableStateFlow<LogResult?>(null)
    val result: StateFlow<LogResult?> = _result.asStateFlow()

    fun logNow(note: String) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            takeSnapshot().fold(
                onSuccess = {
                    runCatchingCancellable { diary.rebuildDay(LocalDate.now()) }
                    _result.value = LogResult.Saved(attachNote(note.trim()))
                },
                onFailure = { _result.value = LogResult.Failed(it.message) },
            )
            _saving.value = false
        }
    }

    private suspend fun attachNote(text: String): NoteOutcome {
        if (text.isEmpty()) return NoteOutcome.NONE
        val range = DayRange.of(LocalDate.now(), ZoneId.systemDefault())
        val newest = diary.observeEvents(range.first, range.last).first().maxByOrNull { it.startTime }
            ?: return NoteOutcome.NO_EVENT_YET
        diary.addNote(Note(eventId = newest.id, text = text, createdAt = System.currentTimeMillis()))
        return NoteOutcome.ATTACHED
    }

    fun clearResult() {
        _result.value = null
    }
}
