package com.shp.evernote.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.repository.NoteRepository
import com.shp.evernote.data.repository.PhotoInput
import com.shp.evernote.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** A photo in the editor: already stored, or picked and not yet imported. */
sealed interface DraftPhoto {
    val key: String

    data class Existing(val photo: Photo) : DraftPhoto {
        override val key: String get() = photo.id
    }

    data class New(val source: Uri, override val key: String = UUID.randomUUID().toString()) : DraftPhoto
}

data class NoteEditorUiState(
    val isNew: Boolean = true,
    val text: String = "",
    val photos: List<DraftPhoto> = emptyList(),
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = loaded && !saving && (text.isNotBlank() || photos.isNotEmpty())
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get<String>("noteId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(NoteEditorUiState(isNew = noteId == null))
    val uiState: StateFlow<NoteEditorUiState> = _uiState

    init {
        viewModelScope.launch {
            val note = noteId?.let { repository.getNote(it) }
            _uiState.update {
                it.copy(
                    text = note?.text ?: "",
                    photos = note?.photos.orEmpty().map { p -> DraftPhoto.Existing(p) },
                    loaded = true,
                )
            }
        }
    }

    fun fileFor(fileName: String): File = photoStore.fileFor(fileName)

    fun onTextChange(text: String) = _uiState.update { it.copy(text = text) }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(photos = it.photos + uris.map { u -> DraftPhoto.New(u) }) }
    }

    fun removePhoto(key: String) = _uiState.update { s -> s.copy(photos = s.photos.filterNot { it.key == key }) }

    /** Moves the photo at [from] to [to], clamping to the list. */
    fun movePhoto(from: Int, to: Int) = _uiState.update { s ->
        val list = s.photos.toMutableList()
        if (from !in list.indices) return@update s
        val target = to.coerceIn(list.indices)
        if (target == from) return@update s
        val item = list.removeAt(from)
        list.add(target, item)
        s.copy(photos = list)
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun save(onSaved: (String) -> Unit) {
        val s = _uiState.value
        if (!s.canSave) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val inputs = s.photos.map {
                    when (it) {
                        is DraftPhoto.Existing -> PhotoInput.Existing(it.photo.id)
                        is DraftPhoto.New -> PhotoInput.New(it.source)
                    }
                }
                val saved = repository.saveNote(noteId, s.text.trim(), inputs)
                onSaved(saved.id)
            } catch (t: Exception) {
                _uiState.update { it.copy(saving = false, error = t.message ?: t.javaClass.simpleName) }
            }
        }
    }
}
