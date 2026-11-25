package com.shp.evernote.ui.notes

import com.shp.evernote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchTest {
    private fun note(id: String, title: String, text: String) =
        Note(id = id, title = title, text = text, photos = emptyList(), createdAt = 0, updatedAt = 0, ownerId = null)

    private val notes = listOf(
        note("1", "Shelf A", "Boxes of lace fronts"),
        note("2", "Delivery", "Courier left parcel at reception"),
        note("3", "", "shelf B needs restocking"),
    )

    @Test
    fun `blank query matches everything`() {
        assertEquals(notes, notes.filterBy(""))
        assertEquals(notes, notes.filterBy("   "))
    }

    @Test
    fun `matches title or text, ignoring case and surrounding spaces`() {
        assertEquals(listOf("1", "3"), notes.filterBy(" SHELF ").map { it.id })
        assertEquals(listOf("2"), notes.filterBy("parcel").map { it.id })
        assertTrue(notes.filterBy("zzz").isEmpty())
    }

    @Test
    fun `single note match`() {
        assertTrue(note("x", "Hello", "").matches("hel"))
        assertFalse(note("x", "Hello", "").matches("world"))
    }
}
