package com.grownapp.noteapp.ui.note_category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.dao.Note_CategoryDatabase
import com.grownapp.noteapp.ui.note_category.dao.Note_CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteCategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val noteNoteCategoryRepository: Note_CategoryRepository

    init {
        val noteCategoryDao = Note_CategoryDatabase.getDatabase(application).noteCategoryDao()
        noteNoteCategoryRepository = Note_CategoryRepository(noteCategoryDao)
    }

    fun upsertNoteCategories(noteId: Int, newCategories: String) {
        viewModelScope.launch(Dispatchers.IO) {
            noteNoteCategoryRepository.removeNoteCategory(noteId)
            noteNoteCategoryRepository.upsertNoteCategories(noteId, newCategories)
        }
    }

    fun removeNoteCategory(noteId: Int){
        viewModelScope.launch(Dispatchers.IO) {
            noteNoteCategoryRepository.removeNoteCategory(noteId)
        }
    }

    fun getAllCategoryOfNote(noteId: Int): LiveData<List<Category>> {
        return noteNoteCategoryRepository.getAllCategoryOfNote(noteId)
    }

    fun getAllNoteOnCategory(categoryName: String): LiveData<List<Note>> {
        return noteNoteCategoryRepository.getAllNoteOnCategory(categoryName)
    }
}