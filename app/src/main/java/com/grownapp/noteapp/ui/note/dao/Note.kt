package com.grownapp.noteapp.ui.note.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String? = null,
    val note: String? = null,
    val time: String
)
