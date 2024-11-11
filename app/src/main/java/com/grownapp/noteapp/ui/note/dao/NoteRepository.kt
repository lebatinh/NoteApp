package com.grownapp.noteapp.ui.note.dao

import androidx.lifecycle.LiveData
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import com.grownapp.noteapp.ui.note_category.NoteWithCategories

class NoteRepository(
    private val noteDao: NoteDao
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

    suspend fun deleteCategoriesForNote(noteId: Int) {
        noteDao.deleteCategoriesForNote(noteId)
    }

    val notesWithoutCategory: LiveData<List<Note>> = noteDao.getNotesWithoutCategory()

    suspend fun delete(noteId: Int) = noteDao.delete(noteId)

    fun getNotesByCategory(categoryId: Int): LiveData<List<NoteWithCategories>> {
        return noteDao.getNotesByCategory(categoryId)
    }

    fun getNoteById(id: Int): LiveData<Note> {
        return noteDao.getNoteById(id)
    }

    fun getCategoryOfNote(noteId: Int): LiveData<List<Category>> {
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

    //all
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

    fun sortedByColorAsc(): LiveData<List<Note>> {
        return noteDao.sortedByColor()
    }

    // ko category
    fun sortedByUpdatedTimeDescWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByUpdatedTimeDescWithoutCategory()
    }

    fun sortedByUpdatedTimeAscWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByUpdatedTimeAscWithoutCategory()
    }

    fun sortedByTitleDescWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByTitleDescWithoutCategory()
    }

    fun sortedByTitleAscWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByTitleAscWithoutCategory()
    }

    fun sortedByCreatedTimeDescWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByCreatedTimeDescWithoutCategory()
    }

    fun sortedByCreatedTimeAscWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByCreatedTimeAscWithoutCategory()
    }

    fun sortedByColorWithoutCategory(): LiveData<List<Note>> {
        return noteDao.sortedByColorWithoutCategory()
    }

    //theo category
    fun sortedByUpdatedTimeDescByCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByUpdatedTimeDescByCategory(categoryId)
    }

    fun sortedByUpdatedTimeAscByCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByUpdatedTimeAscByCategory(categoryId)
    }

    fun sortedByTitleDescByCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByTitleDescByCategory(categoryId)
    }

    fun sortedByTitleAscByCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByTitleAscByCategory(categoryId)
    }

    fun sortedByCreatedTimeDescByCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByCreatedTimeDescByCategory(categoryId)
    }

    fun sortedByCreatedTimeAscByCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByCreatedTimeAscByCategory(categoryId)
    }

    fun sortedByColorWithCategory(categoryId: Int): LiveData<List<Note>> {
        return noteDao.sortedByColorWithCategory(categoryId)
    }

    val allTrashNote: LiveData<List<Note>> = noteDao.getNoteOnTrash()

    fun pushInTrash(onTrash: Boolean, noteId: Int) {
        noteDao.pushInTrash(onTrash, noteId)
    }

    suspend fun restoreAllNote() {
        noteDao.restoreAllNote()
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun updateBackgroundColor(noteIds: List<Int>, backgroundColor: Int) {
        noteDao.updateBackgroundColor(noteIds, backgroundColor)
    }
}