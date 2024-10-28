package com.grownapp.dao

import androidx.lifecycle.LiveData
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.dao.NoteDao

class NoteRepository(private val noteDao: NoteDao) {
    val allNote: LiveData<List<Note>> = noteDao.getAllNote()

    suspend fun upsert(note: Note): Int {
        return if (note.id == 0 ){
            noteDao.insert(note).toInt()
        }else{
            noteDao.update(note)
            note.id
        }
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    fun getNoteById(id: Int): LiveData<Note> {
        return noteDao.getNoteById(id)
    }

    fun searchNote(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchNote(searchQuery)
    }
}