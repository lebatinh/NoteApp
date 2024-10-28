package com.grownapp.noteapp.ui.note.dao

import androidx.lifecycle.LiveData
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.categories.dao.CategoryDao
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import com.grownapp.noteapp.ui.note_category.NoteWithCategories

class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) {
    val allNote: LiveData<List<Note>> = noteDao.getAllNote()

    suspend fun insert(note: Note):Long {
        return noteDao.insert(note)
    }

    suspend fun insertNoteCategoryCrossRef(noteCategoryCrossRef: NoteCategoryCrossRef){
        noteDao.insertNoteCategoryCrossRef(noteCategoryCrossRef)
    }

    val notesWithoutCategory: LiveData<List<Note>> = noteDao.getNotesWithoutCategory()

    suspend fun delete(note: Note) = noteDao.delete(note)

    fun getNotesByCategory(categoryId: Int): LiveData<List<NoteWithCategories>> {
        return noteDao.getNotesByCategory(categoryId)
    }
    suspend fun insertCategory(category: Category) = categoryDao.insert(category)

    fun getAllCategories(): LiveData<List<Category>> = categoryDao.getAllCategory()

    fun getNoteById(id: Int): LiveData<Note> {
        return noteDao.getNoteById(id)
    }

    fun searchNote(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchNote(searchQuery)
    }
}