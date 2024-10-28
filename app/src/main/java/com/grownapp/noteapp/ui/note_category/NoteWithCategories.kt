package com.grownapp.noteapp.ui.note_category

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

data class NoteWithCategories(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "noteId",
        entityColumn = "categoryId",
        associateBy = Junction(NoteCategoryCrossRef::class)
    ) val categories: List<Category> = emptyList()
)