package com.shp.evernote.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    fun observeNotes(): Flow<List<NoteWithPhotos>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND deleted_at IS NULL")
    fun observeNote(id: String): Flow<NoteWithPhotos?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND deleted_at IS NULL")
    suspend fun getNote(id: String): NoteWithPhotos?

    @Query("SELECT * FROM photos WHERE note_id = :noteId ORDER BY position")
    suspend fun getPhotos(noteId: String): List<PhotoEntity>

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Upsert
    suspend fun upsertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE note_id = :noteId AND id NOT IN (:keepIds)")
    suspend fun deletePhotosNotIn(noteId: String, keepIds: List<String>)

    @Query("DELETE FROM photos WHERE note_id = :noteId")
    suspend fun deletePhotosOf(noteId: String)

    @Query("UPDATE notes SET deleted_at = :at, updated_at = :at WHERE id = :id")
    suspend fun markDeleted(id: String, at: Long)

    /**
     * Replaces a note and its photo list in one transaction. Photos not in
     * [photos] are removed from the database; their files are the caller's
     * to delete.
     */
    @Transaction
    suspend fun saveNoteWithPhotos(note: NoteEntity, photos: List<PhotoEntity>) {
        upsertNote(note)
        if (photos.isEmpty()) {
            deletePhotosOf(note.id)
        } else {
            deletePhotosNotIn(note.id, photos.map { it.id })
            upsertPhotos(photos)
        }
    }
}
