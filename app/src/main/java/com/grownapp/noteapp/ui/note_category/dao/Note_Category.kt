package com.grownapp.noteapp.ui.note_category.dao

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

@Entity(
    tableName = "note_category",
    primaryKeys = ["noteId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Note_Category(
    val noteId: Int,
    val categoryId: Int
)

