package com.shp.evernote.data.repository

import android.net.Uri
import com.shp.evernote.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * One photo in a note being saved, in display order. Either a photo the note
 * already has, or a new image to import from a content URI.
 */
sealed interface PhotoInput {
    data class Existing(val photoId: String) : PhotoInput
    data class New(val source: Uri) : PhotoInput
}

/**
 * The only door between the UI and storage. The offline implementation is
 * the source of truth; a syncing implementation can wrap it later without
 * the UI noticing.
 */
interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun getNote(id: String): Note?

    /**
     * Creates (when [id] is null) or replaces a note. [photos] is the full
     * list in display order: photos of the note that are absent from it are
     * removed, and new ones are imported and compressed.
     */
    suspend fun saveNote(id: String?, text: String, photos: List<PhotoInput>): Note

    suspend fun deleteNote(id: String)
}
