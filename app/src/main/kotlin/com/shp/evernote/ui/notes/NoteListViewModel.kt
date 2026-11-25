package com.shp.evernote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.repository.NoteRepository
import com.shp.evernote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val loaded: Boolean = false,
) {
    /** True when a search is active and nothing matched. */
    val noMatches: Boolean get() = loaded && query.isNotBlank() && notes.isEmpty()
}

@HiltViewModel
class NoteListViewModel @Inject constructor(
    repository: NoteRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<NoteListUiState> =
        combine(repository.observeNotes(), query) { notes, q ->
            NoteListUiState(notes = notes.filterBy(q), query = q, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteListUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun fileFor(fileName: String): File = photoStore.fileFor(fileName)
}
