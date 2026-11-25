package com.shp.evernote.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shp.evernote.ui.camera.CameraRoute
import com.shp.evernote.ui.detail.NoteDetailRoute
import com.shp.evernote.ui.editor.NoteEditorRoute
import com.shp.evernote.ui.notes.NoteListRoute

/** Navigation graph. */
object Routes {
    const val NOTES = "notes"
    const val DETAIL = "note/{noteId}"
    const val EDITOR = "edit?noteId={noteId}"
    const val CAMERA = "camera"

    const val CAPTURED_PHOTO = "capturedPhoto"

    fun detail(noteId: String) = "note/$noteId"
    fun editor(noteId: String? = null) = if (noteId == null) "edit" else "edit?noteId=$noteId"
}

@Composable
fun EvernoteApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.NOTES) {
        composable(Routes.NOTES) {
            NoteListRoute(
                onCreateNote = { navController.navigate(Routes.editor()) },
                onOpenNote = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("noteId") { type = NavType.StringType })) {
            NoteDetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editor(it)) },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            // The camera screen posts its result here; the editor consumes it once.
            val captured by entry.savedStateHandle.getLiveData<String?>(Routes.CAPTURED_PHOTO).observeAsState()
            NoteEditorRoute(
                capturedPhoto = captured?.let(Uri::parse),
                onCapturedPhotoConsumed = { entry.savedStateHandle[Routes.CAPTURED_PHOTO] = null },
                onOpenCamera = { navController.navigate(Routes.CAMERA) },
                onSaved = { savedId ->
                    // Back to wherever the editor was opened from, then show the note.
                    navController.popBackStack()
                    if (navController.currentBackStackEntry?.destination?.route == Routes.NOTES) {
                        navController.navigate(Routes.detail(savedId))
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CAMERA) {
            CameraRoute(
                onCaptured = { uri ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(Routes.CAPTURED_PHOTO, uri.toString())
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() },
            )
        }
    }
}
