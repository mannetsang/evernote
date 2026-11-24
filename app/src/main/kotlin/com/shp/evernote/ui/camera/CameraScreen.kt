package com.shp.evernote.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.shp.evernote.R
import java.io.File
import java.util.UUID

/**
 * In-app camera. Captures a full-resolution JPEG into the cache directory and
 * hands its URI back; the editor imports (and compresses) it from there.
 */
@Composable
fun CameraRoute(onCaptured: (Uri) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(Unit) { if (!granted) request.launch(Manifest.permission.CAMERA) }

    when {
        granted -> CameraPreview(onCaptured = onCaptured, onClose = onClose)
        denied -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(stringResource(R.string.camera_permission_needed), textAlign = TextAlign.Center)
            Button(onClick = { request.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.action_try_again))
            }
            Button(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.action_back)) }
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun CameraPreview(onCaptured: (Uri) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            provider = future.get().also { p ->
                val preview = Preview.Builder().build().apply { surfaceProvider = previewView.surfaceProvider }
                try {
                    p.unbindAll()
                    p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                } catch (e: Exception) {
                    error = e.message ?: "Camera unavailable"
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_back), tint = Color.White)
        }

        error?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                textAlign = TextAlign.Center,
            )
        }

        FilledIconButton(
            onClick = {
                if (capturing) return@FilledIconButton
                capturing = true
                val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    output,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            capturing = false
                            onCaptured(Uri.fromFile(file))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            capturing = false
                            error = exception.message ?: "Capture failed"
                        }
                    },
                )
            },
            enabled = !capturing,
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).size(72.dp),
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.action_take_photo),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
