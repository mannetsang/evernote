package com.shp.evernote.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Runs the real decode/compress pipeline through Robolectric's native
 * graphics so a regression in image handling fails here, not on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FilePhotoStoreTest {

    private lateinit var context: Context
    private lateinit var store: FilePhotoStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = FilePhotoStore(context, Dispatchers.Unconfined)
    }

    private fun jpeg(width: Int, height: Int, dir: File = context.cacheDir): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val file = File(dir, "src-${width}x$height.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    @Test
    fun `a large photo is downsized to the limit and written as JPEG`() = runTest {
        val src = jpeg(4000, 3000)

        val stored = store.import(Uri.fromFile(src))

        assertEquals(1600, stored.width)
        assertEquals(1200, stored.height)
        val out = store.fileFor(stored.fileName)
        assertTrue(out.exists())
        assertTrue(out.parentFile!!.canonicalPath.startsWith(context.filesDir.canonicalPath))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.path, bounds)
        assertEquals(1600, bounds.outWidth)
        assertEquals(1200, bounds.outHeight)
        // Import alone must not touch the source.
        assertTrue(src.exists())
    }

    @Test
    fun `a small photo keeps its size`() = runTest {
        val stored = store.import(Uri.fromFile(jpeg(640, 480)))
        assertEquals(640, stored.width)
        assertEquals(480, stored.height)
    }

    @Test
    fun `a file that is not an image is rejected and nothing is written`() = runTest {
        val junk = File(context.cacheDir, "junk.jpg").apply { writeText("not a jpeg") }
        val before = File(context.filesDir, "photos").list().orEmpty().size
        try {
            store.import(Uri.fromFile(junk))
            throw AssertionError("expected failure")
        } catch (e: java.io.IOException) {
            // expected
        }
        assertEquals(before, File(context.filesDir, "photos").list().orEmpty().size)
    }

    @Test
    fun `discard only removes temporary files under the cache directory`() = runTest {
        val capture = jpeg(100, 100, File(context.cacheDir, "captures").apply { mkdirs() })
        val elsewhere = jpeg(100, 100, context.filesDir)

        store.discardIfTemporary(Uri.fromFile(capture))
        store.discardIfTemporary(Uri.fromFile(elsewhere))
        store.discardIfTemporary(Uri.parse("content://media/external/images/1"))

        assertFalse(capture.exists())
        assertTrue(elsewhere.exists())
    }

    @Test
    fun `delete removes a stored photo`() = runTest {
        val stored = store.import(Uri.fromFile(jpeg(200, 100)))
        assertTrue(store.fileFor(stored.fileName).exists())
        store.delete(stored.fileName)
        assertFalse(store.fileFor(stored.fileName).exists())
    }
}
