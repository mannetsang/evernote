package com.shp.evernote.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.shp.evernote.domain.model.Note
import com.shp.evernote.domain.model.Photo

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    /** Added in schema v2; older rows get an empty title. */
    @ColumnInfo(defaultValue = "") val title: String = "",
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** Soft delete so the deletion can be synced later; null when live. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "owner_id") val ownerId: String? = null,
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id")],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    val position: Int,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class NoteWithPhotos(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val photos: List<PhotoEntity>,
)

fun NoteWithPhotos.toDomain(): Note = Note(
    id = note.id,
    title = note.title,
    text = note.text,
    photos = photos.sortedBy { it.position }.map { it.toDomain() },
    createdAt = note.createdAt,
    updatedAt = note.updatedAt,
    ownerId = note.ownerId,
)

fun PhotoEntity.toDomain(): Photo = Photo(
    id = id,
    noteId = noteId,
    fileName = fileName,
    position = position,
    width = width,
    height = height,
    createdAt = createdAt,
)
