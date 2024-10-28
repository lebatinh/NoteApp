package com.grownapp.noteapp.ui.note_category

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.dao.Note_Category

data class CategoryWithNotes(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "name",
        entityColumn = "id",
        associateBy = Junction(Note_Category::class)
    )
    val notes: List<Note>
)
