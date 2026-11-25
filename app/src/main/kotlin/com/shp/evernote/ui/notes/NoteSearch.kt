package com.shp.evernote.ui.notes

import com.shp.evernote.domain.model.Note

/** Case-insensitive match on title and text. Blank query matches everything. */
fun Note.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return title.contains(q, ignoreCase = true) || text.contains(q, ignoreCase = true)
}

fun List<Note>.filterBy(query: String): List<Note> = if (query.isBlank()) this else filter { it.matches(query) }
