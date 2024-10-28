package com.grownapp.noteapp.ui.note_category.dao

import androidx.room.Entity
import androidx.room.ForeignKey
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

@Entity(
    primaryKeys = ["noteId", "categoryName"],
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Category::class, parentColumns = ["name"], childColumns = ["categoryName"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Note_Category(
    val noteId: Int,
    val categoryName: String
)
