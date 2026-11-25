package com.shp.evernote.ui.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import com.shp.evernote.data.files.FilePhotoStore
import com.shp.evernote.domain.model.Note
import com.shp.evernote.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class NoteSharerTest {

    private lateinit var context: Context
    private lateinit var sharer: NoteSharer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharer = NoteSharer(context, FilePhotoStore(context, Dispatchers.Unconfined))
    }

    /** A photo whose file really exists in app-private storage, as it would after import. */
    private fun photo(name: String): Photo {
        File(context.filesDir, "photos").apply { mkdirs() }.resolve(name).writeBytes(byteArrayOf(1, 2, 3))
        return Photo(id = name, noteId = "n", fileName = name, position = 0, width = 1, height = 1, createdAt = 0)
    }

    private fun inner(chooser: Intent): Intent = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)!!

    @Test
    fun `text-only note shares as plain text with the title as subject`() {
        val send = inner(sharer.shareIntent(Note("n", "Shelf A", "Two boxes", emptyList(), 0, 0, null)))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals("Shelf A", send.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Shelf A\n\nTwo boxes", send.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
    }

    /**
     * One test for both photo cases: FileProvider caches its roots per
     * authority in a static map, and each Robolectric test gets a fresh data
     * directory, so a second test would match against stale roots.
     */
    @Test
    fun `photos go out as FileProvider content URIs with a read grant`() {
        val single = inner(sharer.shareIntent(Note("n", "T", "", listOf(photo("a.jpg")), 0, 0, null)))
        assertEquals(Intent.ACTION_SEND, single.action)
        assertEquals("content", IntentCompat.getParcelableExtra(single, Intent.EXTRA_STREAM, Uri::class.java)!!.scheme)

        val send = inner(sharer.shareIntent(Note("n", "", "", listOf(photo("a.jpg"), photo("b.jpg")), 0, 0, null)))
        assertEquals(Intent.ACTION_SEND_MULTIPLE, send.action)
        assertEquals("image/jpeg", send.type)
        val uris = IntentCompat.getParcelableArrayListExtra(send, Intent.EXTRA_STREAM, Uri::class.java)!!
        assertEquals(2, uris.size)
        uris.forEach {
            assertEquals("content", it.scheme)
            assertEquals("${context.packageName}.fileprovider", it.authority)
            assertTrue(it.path!!.startsWith("/photos/"))
        }
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
