package com.shp.evernote.ui.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Hands a note to the Android share sheet: the title and text as the
 * message, the photos as attachments. Photo files stay app-private; the
 * receiving app gets a one-off, read-only content URI through FileProvider.
 *
 * Sharing a link to a note needs the note to live on the server, so that
 * arrives with the sync milestone, and then as an expiring signed link.
 */
class NoteSharer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val photoStore: PhotoStore,
) {
    fun shareIntent(note: Note): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>(
            note.photos.map { FileProvider.getUriForFile(context, authority, photoStore.fileFor(it.fileName)) },
        )
        val body = buildString {
            if (note.title.isNotBlank()) appendLine(note.title)
            if (note.text.isNotBlank()) {
                if (isNotEmpty()) appendLine()
                append(note.text)
            }
        }.trim()

        val send = when (uris.size) {
            0 -> Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
            1 -> Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }.apply {
            if (note.title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, note.title)
            if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, note.title.ifBlank { null })
    }

    fun share(context: Context, note: Note) {
        context.startActivity(shareIntent(note))
    }
}
