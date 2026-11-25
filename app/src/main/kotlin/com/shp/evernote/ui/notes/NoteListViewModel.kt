package com.shp.evernote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.repository.NoteRepository
import com.shp.evernote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    repository: NoteRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    val uiState: StateFlow<NoteListUiState> = repository.observeNotes()
        .map { NoteListUiState(notes = it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteListUiState())

    fun fileFor(fileName: String): File = photoStore.fileFor(fileName)
}
