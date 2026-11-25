package com.shp.evernote.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.repository.NoteRepository
import com.shp.evernote.domain.model.Note
import com.shp.evernote.ui.share.NoteSharer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class NoteDetailUiState(
    val note: Note? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    private val photoStore: PhotoStore,
    private val sharer: NoteSharer,
) : ViewModel() {

    val noteId: String = checkNotNull(savedStateHandle["noteId"])

    val uiState: StateFlow<NoteDetailUiState> = repository.observeNote(noteId)
        .map { NoteDetailUiState(note = it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteDetailUiState())

    fun fileFor(fileName: String): File = photoStore.fileFor(fileName)

    fun share(context: android.content.Context) {
        val note = uiState.value.note ?: return
        sharer.share(context, note)
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            onDone()
        }
    }
}
