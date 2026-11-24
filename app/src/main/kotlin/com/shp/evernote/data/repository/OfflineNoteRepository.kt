package com.shp.evernote.data.repository

import com.shp.evernote.auth.AuthProvider
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.local.NoteDao
import com.shp.evernote.data.local.NoteEntity
import com.shp.evernote.data.local.PhotoEntity
import com.shp.evernote.data.local.toDomain
import com.shp.evernote.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room plus app-private files. Works with no network at all. */
@Singleton
class OfflineNoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val photoStore: PhotoStore,
    private val auth: AuthProvider,
    private val clock: Clock = Clock.System,
    private val ids: IdGenerator = IdGenerator.Random,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeNotes().map { list -> list.map { it.toDomain() } }

    override fun observeNote(id: String): Flow<Note?> =
        dao.observeNote(id).map { it?.toDomain() }

    override suspend fun getNote(id: String): Note? = dao.getNote(id)?.toDomain()

    override suspend fun saveNote(id: String?, text: String, photos: List<PhotoInput>): Note {
        val now = clock.now()
        val existing = id?.let { dao.getNote(it) }
        val noteId = existing?.note?.id ?: id ?: ids.next()
        val existingPhotos = existing?.photos.orEmpty().associateBy { it.id }

        // Import new photos first so a failure leaves the database untouched.
        val imported = ArrayList<String>()
        val entities = try {
            photos.mapIndexed { index, input ->
                when (input) {
                    is PhotoInput.Existing -> {
                        val p = existingPhotos[input.photoId]
                            ?: throw IllegalArgumentException("Photo ${input.photoId} is not on note $noteId")
                        p.copy(position = index)
                    }
                    is PhotoInput.New -> {
                        val stored = photoStore.import(input.source)
                        imported += stored.fileName
                        PhotoEntity(
                            id = ids.next(),
                            noteId = noteId,
                            fileName = stored.fileName,
                            position = index,
                            width = stored.width,
                            height = stored.height,
                            createdAt = now,
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            imported.forEach { runCatching { photoStore.delete(it) } }
            throw t
        }

        val entity = NoteEntity(
            id = noteId,
            text = text,
            createdAt = existing?.note?.createdAt ?: now,
            updatedAt = now,
            deletedAt = null,
            ownerId = existing?.note?.ownerId ?: auth.currentUserId(),
        )
        dao.saveNoteWithPhotos(entity, entities)

        // Files of photos that were dropped from the note.
        val kept = entities.map { it.id }.toSet()
        existingPhotos.values.filter { it.id !in kept }.forEach { photoStore.delete(it.fileName) }

        return checkNotNull(dao.getNote(noteId)).toDomain()
    }

    override suspend fun deleteNote(id: String) {
        val photos = dao.getPhotos(id)
        dao.deletePhotosOf(id)
        dao.markDeleted(id, clock.now())
        photos.forEach { photoStore.delete(it.fileName) }
    }
}

/** Injectable time source so repository tests are deterministic. */
fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

/** Client-side IDs so offline-created rows never clash when synced. */
fun interface IdGenerator {
    fun next(): String

    companion object {
        val Random = IdGenerator { java.util.UUID.randomUUID().toString() }
    }
}
