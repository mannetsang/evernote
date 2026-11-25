package com.shp.evernote.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.shp.evernote.data.repository.FakeAuth
import com.shp.evernote.data.repository.FakeClock
import com.shp.evernote.data.repository.FakeNoteDao
import com.shp.evernote.data.repository.FakePhotoStore
import com.shp.evernote.data.repository.OfflineNoteRepository
import com.shp.evernote.data.repository.PhotoInput
import com.shp.evernote.data.repository.SequentialIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeNoteDao
    private lateinit var store: FakePhotoStore
    private lateinit var repo: OfflineNoteRepository

    private fun uri(n: Int): Uri = Uri.parse("content://test/$n")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeNoteDao()
        store = FakePhotoStore()
        repo = OfflineNoteRepository(dao, store, FakeAuth(), FakeClock(), SequentialIds())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(noteId: String? = null) =
        NoteEditorViewModel(SavedStateHandle(mapOf("noteId" to noteId)), repo, store)

    @Test
    fun `new note cannot be saved until it has text or a photo`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isNew)
        assertFalse(vm.uiState.value.canSave)

        vm.onTextChange("  ")
        assertFalse(vm.uiState.value.canSave)

        vm.onTitleChange("Shelf A")
        assertTrue(vm.uiState.value.canSave)

        vm.onTitleChange("")
        vm.addPhotos(listOf(uri(1)))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `photos can be reordered and removed`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.addPhotos(listOf(uri(1), uri(2), uri(3)))
        val keys = vm.uiState.value.photos.map { it.key }

        vm.movePhoto(0, 2)
        assertEquals(listOf(keys[1], keys[2], keys[0]), vm.uiState.value.photos.map { it.key })

        vm.movePhoto(2, -5) // clamps to the front
        assertEquals(listOf(keys[0], keys[1], keys[2]), vm.uiState.value.photos.map { it.key })

        vm.movePhoto(9, 0) // out of range: no change
        assertEquals(listOf(keys[0], keys[1], keys[2]), vm.uiState.value.photos.map { it.key })

        vm.removePhoto(keys[1])
        assertEquals(listOf(keys[0], keys[2]), vm.uiState.value.photos.map { it.key })
    }

    @Test
    fun `editing loads the note and saving writes the new order`() = runTest(dispatcher) {
        val note = repo.saveNote(null, "Orig title", "orig", listOf(PhotoInput.New(uri(1)), PhotoInput.New(uri(2))))
        val vm = viewModel(note.id)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isNew)
        assertEquals("Orig title", vm.uiState.value.title)
        assertEquals("orig", vm.uiState.value.text)
        assertEquals(2, vm.uiState.value.photos.size)

        vm.onTitleChange("New title")
        vm.onTextChange("changed")
        vm.movePhoto(1, 0)
        vm.addPhotos(listOf(uri(3)))
        var savedId: String? = null
        vm.save { savedId = it }
        advanceUntilIdle()

        assertEquals(note.id, savedId)
        val saved = repo.getNote(note.id)
        assertNotNull(saved)
        assertEquals("New title", saved!!.title)
        assertEquals("changed", saved.text)
        assertEquals(listOf(note.photos[1].id, note.photos[0].id), saved.photos.take(2).map { it.id })
        assertEquals(3, saved.photos.size)
    }

    @Test
    fun `a failed save surfaces an error and keeps the draft`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onTextChange("hello")
        vm.addPhotos(listOf(uri(1)))
        store.failOnImport = 1

        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.saving)
        assertEquals("hello", vm.uiState.value.text)
        assertEquals(1, vm.uiState.value.photos.size)
        vm.dismissError()
        assertNull(vm.uiState.value.error)
    }
}
