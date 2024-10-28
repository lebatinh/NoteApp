package com.grownapp.noteapp.ui.note

import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

sealed class NoteItem {
    data class NoteWithCategories(val note: Note, val categories: List<Category>) : NoteItem()
    data class NoteWithoutCategories(val note: Note) : NoteItem()
}