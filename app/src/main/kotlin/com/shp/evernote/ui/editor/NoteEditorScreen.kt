package com.shp.evernote.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.shp.evernote.R
import com.shp.evernote.ui.common.PhotoImage
import java.io.File

@Composable
fun NoteEditorRoute(
    capturedPhoto: Uri?,
    onCapturedPhotoConsumed: () -> Unit,
    onOpenCamera: () -> Unit,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(capturedPhoto) {
        if (capturedPhoto != null) {
            viewModel.addPhotos(listOf(capturedPhoto))
            onCapturedPhotoConsumed()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        viewModel.addPhotos(uris)
    }

    NoteEditorScreen(
        state = state,
        fileFor = viewModel::fileFor,
        onTextChange = viewModel::onTextChange,
        onPickFromGallery = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onOpenCamera = onOpenCamera,
        onRemovePhoto = viewModel::removePhoto,
        onMovePhoto = viewModel::movePhoto,
        onSave = { viewModel.save(onSaved) },
        onDismissError = viewModel::dismissError,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState,
    fileFor: (String) -> File,
    onTextChange: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onMovePhoto: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            PhotoStrip(
                photos = state.photos,
                fileFor = fileFor,
                enabled = !state.saving,
                onRemove = onRemovePhoto,
                onMove = onMovePhoto,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onOpenCamera, enabled = !state.saving) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Text(stringResource(R.string.action_camera), modifier = Modifier.padding(start = 8.dp))
                }
                FilledTonalButton(onClick = onPickFromGallery, enabled = !state.saving) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Text(stringResource(R.string.action_gallery), modifier = Modifier.padding(start = 8.dp))
                }
            }

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                enabled = state.loaded && !state.saving,
                label = { Text(stringResource(R.string.editor_text_label)) },
                minLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.editor_error_title)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDismissError) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Composable
private fun PhotoStrip(
    photos: List<DraftPhoto>,
    fileFor: (String) -> File,
    enabled: Boolean,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.editor_no_photos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(photos, key = { _, p -> p.key }) { index, photo ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.size(140.dp).clip(RoundedCornerShape(10.dp)),
                    ) {
                        when (photo) {
                            is DraftPhoto.Existing -> PhotoImage(
                                file = fileFor(photo.photo.fileName),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                            is DraftPhoto.New -> AsyncImage(
                                model = photo.source,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    IconButton(
                        onClick = { onRemove(photo.key) },
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_remove_photo),
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = enabled && index > 0) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.action_move_earlier))
                    }
                    IconButton(onClick = { onMove(index, index + 1) }, enabled = enabled && index < photos.lastIndex) {
                        Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.action_move_later))
                    }
                }
            }
        }
    }
}
