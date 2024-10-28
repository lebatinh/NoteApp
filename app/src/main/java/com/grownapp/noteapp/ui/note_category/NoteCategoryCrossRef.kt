package com.grownapp.noteapp.ui.note_category

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "note_category",
    primaryKeys = ["noteId", "categoryId"],
    indices = [Index(value = ["noteId"]), Index(value = ["categoryId"])]
)
data class NoteCategoryCrossRef(
    val noteId: Int,
    val categoryId: Int
)