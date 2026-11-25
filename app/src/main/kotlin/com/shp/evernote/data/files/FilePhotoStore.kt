package com.shp.evernote.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.shp.evernote.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePhotoStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PhotoStore {

    private val dir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    override fun fileFor(fileName: String): File = File(dir, fileName)

    override suspend fun import(source: Uri): StoredPhoto = withContext(io) {
        val resolver = context.contentResolver

        // Pass 1: dimensions only. decodeStream returns null here by design.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image: $source")

        // Pass 2: decode at a power-of-two sample so memory stays bounded.
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, PhotoStore.MAX_EDGE_PX)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = open(source).use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IOException("Cannot decode $source")

        val rotation = open(source).use { exifRotation(ExifInterface(it)) }
        val scale = scaleFor(sampled.width, sampled.height, PhotoStore.MAX_EDGE_PX)

        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val finalBitmap = if (matrix.isIdentity) {
            sampled
        } else {
            Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
                .also { if (it !== sampled) sampled.recycle() }
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val target = fileFor(fileName)
        val tmp = File(dir, "$fileName.tmp")
        try {
            tmp.outputStream().buffered().use { out ->
                if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, PhotoStore.JPEG_QUALITY, out)) {
                    throw IOException("JPEG encode failed")
                }
            }
            if (!tmp.renameTo(target)) throw IOException("Cannot move photo into place")
        } finally {
            tmp.delete()
        }
        val result = StoredPhoto(fileName = fileName, width = finalBitmap.width, height = finalBitmap.height)
        finalBitmap.recycle()
        result
    }

    private fun open(source: Uri): InputStream =
        context.contentResolver.openInputStream(source) ?: throw IOException("Cannot open $source")

    override suspend fun discardIfTemporary(source: Uri) {
        if (source.scheme != "file") return
        val path = source.path ?: return
        withContext(io) {
            val file = File(path)
            if (file.canonicalPath.startsWith(context.cacheDir.canonicalPath)) file.delete()
        }
    }

    override suspend fun delete(fileName: String) {
        withContext(io) { fileFor(fileName).delete() }
    }

    private fun exifRotation(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}
