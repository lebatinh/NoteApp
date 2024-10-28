package com.grownapp.noteapp.ui.note_category.dao

import androidx.lifecycle.LiveData
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

class Note_CategoryRepository(
    private val noteCategoryDao: Note_CategoryDao
) {
    suspend fun upsertNoteCategories(noteId: Int, newCategories: String) {
        noteCategoryDao.upsertNoteToCategory(Note_Category(noteId, newCategories))
    }

    suspend fun removeNoteCategory(noteId: Int){
        noteCategoryDao.removeNoteCategory(noteId)
    }
    // Lấy all category của note nào đó
    fun getAllCategoryOfNote(noteId: Int): LiveData<List<Category>> {
        return noteCategoryDao.getAllCategoryOfNote(noteId)
    }

    // Lấy all note trong category nào đó
    fun getAllNoteOnCategory(categoryName: String): LiveData<List<Note>> {
        return noteCategoryDao.getAllNoteOnCategory(categoryName)
    }
}