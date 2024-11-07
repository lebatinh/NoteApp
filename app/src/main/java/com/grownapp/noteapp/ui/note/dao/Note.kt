package com.grownapp.noteapp.ui.note.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val title: String? = null,
    val note: String? = null,
    var timeCreate: String? = null,
    var timeLastEdit: String? = getCurrentTime(),
    var onTrash: Boolean? = false,
    var backgroundColor: Int? = null
) {
    companion object {
        fun getCurrentTime(): String {
            val currentDateTime = Date()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
            val time = dateFormat.format(currentDateTime)
            return time
        }
    }
}
