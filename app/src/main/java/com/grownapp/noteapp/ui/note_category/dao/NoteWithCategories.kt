package com.grownapp.noteapp.ui.note_category.dao

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

data class NoteWithCategories(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(Note_Category::class)
    )
    val categories: List<Category>
){
    constructor() : this(Note(time = ""), emptyList())
}
