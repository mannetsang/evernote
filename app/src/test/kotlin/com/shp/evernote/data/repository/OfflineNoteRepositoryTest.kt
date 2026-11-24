package com.shp.evernote.data.repository

import android.net.Uri
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Repository behaviour against in-memory fakes. Robolectric only supplies
 * android.net.Uri; no database or file system is involved.
 */
@RunWith(RobolectricTestRunner::class)
class OfflineNoteRepositoryTest {

    private lateinit var dao: FakeNoteDao
    private lateinit var store: FakePhotoStore
    private lateinit var clock: FakeClock
    private lateinit var repo: OfflineNoteRepository

    private fun uri(n: Int): Uri = Uri.parse("content://test/$n")

    @Before
    fun setUp() {
        dao = FakeNoteDao()
        store = FakePhotoStore()
        clock = FakeClock()
        repo = OfflineNoteRepository(dao, store, FakeAuth(), clock, SequentialIds())
    }

    @Test
    fun `creating a note imports photos in order and stamps owner and times`() = runTest {
        val note = repo.saveNote(null, "hello", listOf(PhotoInput.New(uri(1)), PhotoInput.New(uri(2))))

        assertEquals("hello", note.text)
        assertEquals("user-1", note.ownerId)
        assertEquals(1_000L, note.createdAt)
        assertEquals(1_000L, note.updatedAt)
        assertEquals(listOf(0, 1), note.photos.map { it.position })
        assertEquals(listOf("p0.jpg", "p1.jpg"), note.photos.map { it.fileName })
        assertEquals(setOf("p0.jpg", "p1.jpg"), store.files)
    }

    @Test
    fun `editing keeps createdAt, bumps updatedAt, reorders and drops photos`() = runTest {
        val created = repo.saveNote(null, "a", listOf(PhotoInput.New(uri(1)), PhotoInput.New(uri(2)), PhotoInput.New(uri(3))))
        val (p0, p1, p2) = created.photos

        clock.now = 2_000L
        val edited = repo.saveNote(
            created.id,
            "b",
            listOf(PhotoInput.Existing(p2.id), PhotoInput.New(uri(4)), PhotoInput.Existing(p0.id)),
        )

        assertEquals(created.id, edited.id)
        assertEquals("b", edited.text)
        assertEquals(1_000L, edited.createdAt)
        assertEquals(2_000L, edited.updatedAt)
        assertEquals(listOf(p2.id, "id-4", p0.id), edited.photos.map { it.id })
        assertEquals(listOf(0, 1, 2), edited.photos.map { it.position })
        // p1 was dropped: row and file gone.
        assertTrue(p1.fileName !in store.files)
        assertEquals(setOf(p0.fileName, p2.fileName, "p3.jpg"), store.files)
    }

    @Test
    fun `a failed import leaves the database untouched and cleans up imported files`() = runTest {
        val created = repo.saveNote(null, "a", listOf(PhotoInput.New(uri(1))))

        // Imports so far: 1. The next save imports two new photos; the second fails.
        store.failOnImport = 3
        try {
            repo.saveNote(
                created.id,
                "changed",
                listOf(PhotoInput.Existing(created.photos[0].id), PhotoInput.New(uri(2)), PhotoInput.New(uri(3))),
            )
            fail("expected failure")
        } catch (e: java.io.IOException) {
            // expected
        }

        val after = repo.getNote(created.id)
        assertNotNull(after)
        assertEquals("a", after!!.text)
        assertEquals(1, after.photos.size)
        assertEquals(setOf("p0.jpg"), store.files)
    }

    @Test
    fun `deleting hides the note, removes photos and files, and the list updates`() = runTest {
        val note = repo.saveNote(null, "x", listOf(PhotoInput.New(uri(1))))

        repo.observeNotes().test {
            assertEquals(1, awaitItem().size)
            repo.deleteNote(note.id)
            // Deletion is two writes (photos, then the note); only the final state matters.
            assertEquals(0, expectMostRecentItem().size)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(repo.getNote(note.id))
        assertTrue(store.files.isEmpty())
        // Soft-deleted row is kept for sync.
        assertNotNull(dao.notes[note.id]?.deletedAt)
    }

    @Test
    fun `referencing a photo from another note is rejected`() = runTest {
        val a = repo.saveNote(null, "a", listOf(PhotoInput.New(uri(1))))
        val b = repo.saveNote(null, "b", emptyList())
        try {
            repo.saveNote(b.id, "b", listOf(PhotoInput.Existing(a.photos[0].id)))
            fail("expected rejection")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
