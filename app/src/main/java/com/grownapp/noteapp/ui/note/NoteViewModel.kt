package com.grownapp.noteapp.ui.note

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grownapp.dao.NoteDatabase
import com.grownapp.noteapp.ReturnResult
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note
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

    private val _noteId = MutableLiveData<Int?>()
    val noteId: LiveData<Int?> get() = _noteId

    private val _returnResult = MutableLiveData<ReturnResult>()

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        val categoryDao = NoteDatabase.getDatabase(application).categoryDao()
        repository = NoteRepository(noteDao, categoryDao)
        allNote = repository.allNote
        allNoteWithoutCategory = repository.notesWithoutCategory
    }

    fun insertFirstNote(sharedPreferences: SharedPreferences) {
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        val time = dateFormat.format(currentDateTime)
        if (isFirstRun) {
            val firstNote = Note(
                0,
                "Hi, how are you? (tap to open)",
                "Thank you for downloading Notepad Free. This is a welcome message.\n" +
                        "You can delete this message by clicking Delete button in the top right corner.\n" +
                        "You can revert any unwanted changes during note edition with the \"Undo\" and \"Redo\" buttons. Try to edit this text, and click the buttons in the top right corner.\n" +
                        "Please check the main menu for additional functions, like Help screen, backup functions, or settings. It can be opened with the button in the top left corner of the main screen.\n" +
                        "\n" +
                        "Have a nice day.\n" +
                        "☺\uFE0F",
                time
            )

            viewModelScope.launch {
                repository.insert(firstNote)
            }
            with(sharedPreferences.edit()) {
                putBoolean("isFirstRun", false)
                apply()
            }
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

    fun delete(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(note)
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

    fun searchNoteWithCategory(
        searchQuery: String,
        categoryId: Int
    ): LiveData<List<NoteWithCategories>> {
        return repository.searchNoteWithCategory("%$searchQuery%", categoryId)
    }

    fun searchNoteWithoutCategory(searchQuery: String): LiveData<List<NoteWithCategories>> {
        return repository.searchNoteWithoutCategory("%$searchQuery%")
    }

    fun search(searchQuery: String): LiveData<List<Note>> {
        return repository.searchNote("%$searchQuery%")
    }

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
}