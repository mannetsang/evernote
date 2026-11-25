package com.shp.evernote.data.repository

import android.net.Uri
import com.shp.evernote.auth.AuthProvider
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.files.StoredPhoto
import com.shp.evernote.data.local.NoteDao
import com.shp.evernote.data.local.NoteEntity
import com.shp.evernote.data.local.NoteWithPhotos
import com.shp.evernote.data.local.PhotoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

/** In-memory stand-in for the Room DAO with the same contract. */
class FakeNoteDao : NoteDao {
    val notes = LinkedHashMap<String, NoteEntity>()
    val photos = LinkedHashMap<String, PhotoEntity>()
    private val version = MutableStateFlow(0)

    private fun snapshot(): List<NoteWithPhotos> =
        notes.values
            .filter { it.deletedAt == null }
            .sortedByDescending { it.updatedAt }
            .map { n -> NoteWithPhotos(n, photos.values.filter { it.noteId == n.id }) }

    private fun bump() { version.value++ }

    override fun observeNotes(): Flow<List<NoteWithPhotos>> = version.map { snapshot() }
    override fun observeNote(id: String): Flow<NoteWithPhotos?> = version.map { snapshot().find { it.note.id == id } }
    override suspend fun getNote(id: String): NoteWithPhotos? = snapshot().find { it.note.id == id }
    override suspend fun getPhotos(noteId: String): List<PhotoEntity> =
        photos.values.filter { it.noteId == noteId }.sortedBy { it.position }

    override suspend fun upsertNote(note: NoteEntity) { notes[note.id] = note; bump() }
    override suspend fun upsertPhotos(photos: List<PhotoEntity>) { photos.forEach { this.photos[it.id] = it }; bump() }
    override suspend fun deletePhotosNotIn(noteId: String, keepIds: List<String>) {
        photos.values.removeIf { it.noteId == noteId && it.id !in keepIds }; bump()
    }
    override suspend fun deletePhotosOf(noteId: String) { photos.values.removeIf { it.noteId == noteId }; bump() }
    override suspend fun markDeleted(id: String, at: Long) {
        notes[id]?.let { notes[id] = it.copy(deletedAt = at, updatedAt = at) }; bump()
    }
}

class FakePhotoStore : PhotoStore {
    val files = LinkedHashSet<String>()
    /** When set, the import with this 1-based sequence number (counting every call) throws. */
    var failOnImport: Int? = null
    private var calls = 0
    private var n = 0

    override suspend fun import(source: Uri): StoredPhoto {
        calls++
        if (calls == failOnImport) throw java.io.IOException("boom")
        val name = "p${n++}.jpg"
        files += name
        return StoredPhoto(name, 1600, 1200)
    }

    val discarded = ArrayList<Uri>()

    override fun fileFor(fileName: String): File = File("/fake/$fileName")
    override suspend fun delete(fileName: String) { files -= fileName }
    override suspend fun discardIfTemporary(source: Uri) { discarded += source }
}

class FakeAuth(private val id: String? = "user-1") : AuthProvider {
    override fun currentUserId(): String? = id
}

class FakeClock(var now: Long = 1_000L) : Clock {
    override fun now(): Long = now
}

class SequentialIds : IdGenerator {
    private var n = 0
    override fun next(): String = "id-${n++}"
}
