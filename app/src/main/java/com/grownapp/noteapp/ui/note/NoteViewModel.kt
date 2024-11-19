package com.grownapp.noteapp.ui.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grownapp.noteapp.ReturnResult
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.dao.NoteDatabase
import com.grownapp.noteapp.ui.note.dao.NoteRepository
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import com.grownapp.noteapp.ui.note_category.NoteWithCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNote: LiveData<List<Note>>
    val allNoteWithoutCategory: LiveData<List<Note>>
    val allTrashNote: LiveData<List<Note>>

    private val _noteId = MutableLiveData<Int?>()
    val noteId: LiveData<Int?> get() = _noteId

    private val _returnResult = MutableLiveData<ReturnResult>()

    private val searchQuery = MutableLiveData<String>()
    val searchResult = MediatorLiveData<List<Note>>()

    private val categoryId = MutableLiveData<Int?>()
    val searchResultCategory = MediatorLiveData<List<NoteWithCategories>>()

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNote = repository.allNote
        allNoteWithoutCategory = repository.notesWithoutCategory
        allTrashNote = repository.allTrashNote

        searchResult.addSource(searchQuery) { query ->
            if (!query.isNullOrEmpty()) {
                val source = repository.searchNote(query)
                searchResult.addSource(source) { notes ->
                    searchResult.value = notes
                    searchResult.removeSource(source)
                }
            }
        }

        searchResultCategory.addSource(searchQuery) { query ->
            performSearch(query, categoryId.value)
        }
        searchResultCategory.addSource(categoryId) { id ->
            performSearch(searchQuery.value, id)
        }

    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    fun setCategoryId(id: Int?) {
        categoryId.value = id
    }
    private fun performSearch(query: String?, id: Int?) {
        if (query.isNullOrEmpty()) {
            searchResultCategory.value = emptyList()
            return
        }
        val source = if (id == null) {
            repository.searchNoteWithoutCategory(query)
        } else {
            repository.searchNoteWithCategory(query, id)
        }
        searchResultCategory.addSource(source) { notes ->
            searchResultCategory.value = notes
            searchResultCategory.removeSource(source)
        }
    }

    fun insertFirst(note: Note) {
        viewModelScope.launch {
            repository.insert(note)
        }
    }

    fun insert(note: Note, callback: (Long) -> Unit) {
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        val time = dateFormat.format(currentDateTime)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (note.noteId == 0) {
                    val insertedNoteId = repository.insert(note.copy(timeCreate = time))
                    _noteId.postValue(insertedNoteId.toInt())
                    callback(insertedNoteId)
                } else {
                    repository.update(
                        note.noteId,
                        note.title.toString(),
                        note.note.toString(),
                        time
                    )
                    _noteId.postValue(note.noteId)
                    callback(note.noteId.toLong())
                }
                _returnResult.postValue(ReturnResult.Success)
            } catch (e: Exception) {
                _returnResult.postValue(ReturnResult.Error("Có lỗi xảy ra vui lòng thử lại sau!"))
            }
        }
    }

    fun insertNoteCategoryCrossRef(noteCategoryCrossRef: NoteCategoryCrossRef) =
        viewModelScope.launch {
            repository.insertNoteCategoryCrossRef(noteCategoryCrossRef)
        }

    fun deleteCategoriesForNote(noteId: Int) = viewModelScope.launch {
        repository.deleteCategoriesForNote(noteId)
    }

    fun delete(noteId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(noteId)
                _returnResult.postValue(ReturnResult.Success)
            } catch (e: Exception) {
                _returnResult.postValue(ReturnResult.Error("Xóa thất bại! Hãy thử lại"))
            }
        }
    }

    fun getNotesByCategory(categoryId: Int): LiveData<List<NoteWithCategories>> {
        return repository.getNotesByCategory(categoryId)
    }

    fun getNoteById(id: Int): LiveData<Note> {
        return repository.getNoteById(id)
    }

    fun getCategoryOfNote(noteId: Int): LiveData<List<Category>> {
        return repository.getCategoryOfNote(noteId)
    }

    fun clearNoteId() {
        _noteId.postValue(null)
    }

    //all
    fun sortedByUpdatedTimeDesc(): LiveData<List<Note>> {
        return repository.sortedByUpdatedTimeDesc()
    }

    fun sortedByUpdatedTimeAsc(): LiveData<List<Note>> {
        return repository.sortedByUpdatedTimeAsc()
    }

    fun sortedByTitleDesc(): LiveData<List<Note>> {
        return repository.sortedByTitleDesc()
    }

    fun sortedByTitleAsc(): LiveData<List<Note>> {
        return repository.sortedByTitleAsc()
    }

    fun sortedByCreatedTimeDesc(): LiveData<List<Note>> {
        return repository.sortedByCreatedTimeDesc()
    }

    fun sortedByCreatedTimeAsc(): LiveData<List<Note>> {
        return repository.sortedByCreatedTimeAsc()
    }

    fun sortedByColorAsc(): LiveData<List<Note>> {
        return repository.sortedByColorAsc()
    }

    // ko category
    fun sortedByUpdatedTimeDescWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByUpdatedTimeDescWithoutCategory()
    }

    fun sortedByUpdatedTimeAscWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByUpdatedTimeAscWithoutCategory()
    }

    fun sortedByTitleDescWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByTitleDescWithoutCategory()
    }

    fun sortedByTitleAscWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByTitleAscWithoutCategory()
    }

    fun sortedByCreatedTimeDescWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByCreatedTimeDescWithoutCategory()
    }

    fun sortedByCreatedTimeAscWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByCreatedTimeAscWithoutCategory()
    }

    fun sortedByColorWithoutCategory(): LiveData<List<Note>> {
        return repository.sortedByColorWithoutCategory()
    }

    //theo category
    fun sortedByUpdatedTimeDescByCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByUpdatedTimeDescByCategory(categoryId)
    }

    fun sortedByUpdatedTimeAscByCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByUpdatedTimeAscByCategory(categoryId)
    }

    fun sortedByTitleDescByCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByTitleDescByCategory(categoryId)
    }

    fun sortedByTitleAscByCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByTitleAscByCategory(categoryId)
    }

    fun sortedByCreatedTimeDescByCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByCreatedTimeDescByCategory(categoryId)
    }

    fun sortedByCreatedTimeAscByCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByCreatedTimeAscByCategory(categoryId)
    }

    fun sortedByColorWithCategory(categoryId: Int): LiveData<List<Note>> {
        return repository.sortedByColorWithCategory(categoryId)
    }

    fun pushInTrash(onTrash: Boolean, noteId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.pushInTrash(onTrash, noteId)
        }
    }

    fun restoreAllNote() = viewModelScope.launch(Dispatchers.IO) {
        repository.restoreAllNote()
    }

    fun emptyTrash() = viewModelScope.launch {
        repository.emptyTrash()
    }

    fun updateBackgroundColor(noteIds: List<Int>, backgroundColor: Int) {
        viewModelScope.launch {
            repository.updateBackgroundColor(noteIds, backgroundColor)
        }
    }

    fun updateChecklistMode(noteId: Int, isChecklistMode: Boolean){
        viewModelScope.launch {
            repository.updateChecklistMode(noteId, isChecklistMode)
        }
    }
}