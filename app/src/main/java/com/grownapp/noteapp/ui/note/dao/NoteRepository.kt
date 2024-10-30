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

    suspend fun insert(note: Note): Long {
        return noteDao.insert(note)
    }

    suspend fun update(noteId: Int, title: String, content: String, updatedTime: String) {
        noteDao.update(noteId, title, content, updatedTime)
    }

    suspend fun insertNoteCategoryCrossRef(noteCategoryCrossRef: NoteCategoryCrossRef) {
        noteDao.insertNoteCategoryCrossRef(noteCategoryCrossRef)
    }
    suspend fun deleteCategoriesForNote(noteId: Int){
        noteDao.deleteCategoriesForNote(noteId)
    }

    val notesWithoutCategory: LiveData<List<Note>> = noteDao.getNotesWithoutCategory()

    suspend fun delete(note: Note) = noteDao.delete(note)

    fun getNotesByCategory(categoryId: Int): LiveData<List<NoteWithCategories>> {
        return noteDao.getNotesByCategory(categoryId)
    }

    fun getNoteById(id: Int): LiveData<Note> {
        return noteDao.getNoteById(id)
    }

    fun getCategoryOfNote(noteId: Int): LiveData<List<Category>>{
        return noteDao.getCategoryOfNote(noteId)
    }
    fun searchNoteWithCategory(
        searchQuery: String,
        categoryId: Int
    ): LiveData<List<NoteWithCategories>> {
        return noteDao.searchNoteWithCategory(searchQuery, categoryId)
    }

    fun searchNoteWithoutCategory(searchQuery: String): LiveData<List<NoteWithCategories>> {
        return noteDao.searchNoteWithoutCategory(searchQuery)
    }

    fun searchNote(searchQuery: String): LiveData<List<Note>> {
        return noteDao.searchNote(searchQuery)
    }

    fun sortedByUpdatedTimeDesc(): LiveData<List<Note>> {
        return noteDao.sortedByUpdatedTimeDesc()
    }
    fun sortedByUpdatedTimeAsc(): LiveData<List<Note>> {
        return noteDao.sortedByUpdatedTimeAsc()
    }

    fun sortedByTitleDesc(): LiveData<List<Note>> {
        return noteDao.sortedByTitleDesc()
    }
    fun sortedByTitleAsc(): LiveData<List<Note>> {
        return noteDao.sortedByTitleAsc()
    }

    fun sortedByCreatedTimeDesc(): LiveData<List<Note>> {
        return noteDao.sortedByCreatedTimeDesc()
    }
    fun sortedByCreatedTimeAsc(): LiveData<List<Note>> {
        return noteDao.sortedByCreatedTimeAsc()
    }
}