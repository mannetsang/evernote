package com.shp.evernote.data.files

import android.net.Uri
import java.io.File

/** A photo after import: compressed and written to app-private storage. */
data class StoredPhoto(val fileName: String, val width: Int, val height: Int)

/**
 * Owns the photo files. Everything is under the app's private files
 * directory, so nothing here is reachable by other apps or by URL.
 */
interface PhotoStore {
    /**
     * Reads the image at [source], downsizes it to at most [MAX_EDGE_PX] on the
     * long edge, corrects orientation, and writes a JPEG. The source is never
     * kept.
     */
    suspend fun import(source: Uri): StoredPhoto

    fun fileFor(fileName: String): File

    suspend fun delete(fileName: String)

    companion object {
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85
    }
}

/**
 * Power-of-two sample size that brings a [width] x [height] image to at most
 * [maxEdge] on its long edge, matching BitmapFactory.Options.inSampleSize.
 * Pure so it can be unit tested without Android.
 */
fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    require(maxEdge > 0)
    var sample = 1
    var edge = maxOf(width, height)
    while (edge / 2 >= maxEdge) {
        edge /= 2
        sample *= 2
    }
    return sample
}

/** Final scale to apply after sampling so the long edge is exactly <= [maxEdge]. */
fun scaleFor(width: Int, height: Int, maxEdge: Int): Float {
    val edge = maxOf(width, height)
    return if (edge <= maxEdge) 1f else maxEdge.toFloat() / edge
}
