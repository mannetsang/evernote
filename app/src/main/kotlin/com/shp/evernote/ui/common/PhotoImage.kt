package com.shp.evernote.ui.common

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File

/** Renders a photo from app-private storage. */
@Composable
fun PhotoImage(
    file: File,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = file,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

fun Modifier.photoAspect(width: Int, height: Int): Modifier =
    if (width > 0 && height > 0) aspectRatio(width.toFloat() / height) else this
