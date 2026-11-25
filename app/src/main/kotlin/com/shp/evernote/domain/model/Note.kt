package com.shp.evernote.domain.model

/**
 * A note: some text plus zero or more photos, in display order.
 * Times are epoch milliseconds (UTC).
 */
data class Note(
    val id: String,
    val title: String,
    val text: String,
    val photos: List<Photo>,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerId: String?,
) {
    val isEmpty: Boolean get() = title.isBlank() && text.isBlank() && photos.isEmpty()
}

data class Photo(
    val id: String,
    val noteId: String,
    /** File name inside the app-private photo directory. Never a public URL. */
    val fileName: String,
    val position: Int,
    val width: Int,
    val height: Int,
    val createdAt: Long,
)
