package com.grownapp.noteapp

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.grownapp.noteapp.ui.NoteContent

class Converters {
    @TypeConverter
    fun fromNoteContent(noteContent: NoteContent): String {
        return Gson().toJson(noteContent) // Chuyển NoteContent thành JSON
    }

    @TypeConverter
    fun toNoteContent(noteContentJson: String): NoteContent {
        return Gson().fromJson(noteContentJson, NoteContent::class.java)
    }
}