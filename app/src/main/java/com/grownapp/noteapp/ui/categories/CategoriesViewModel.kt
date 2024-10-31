package com.grownapp.noteapp.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grownapp.noteapp.ui.note.dao.NoteDatabase
import com.grownapp.noteapp.ReturnResult
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.categories.dao.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CategoryRepository
    val allCategory: LiveData<List<Category>>

    private val _returnResult = MutableLiveData<ReturnResult>()

    init {
        val categoryDao = NoteDatabase.getDatabase(application).categoryDao()
        repository = CategoryRepository(categoryDao)
        allCategory = repository.allCategory
    }

    fun insertCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insert(category)
                _returnResult.postValue(ReturnResult.Success)
            } catch (e: Exception) {
                _returnResult.postValue(ReturnResult.Error("Tạo danh mục thất bại! Hãy thử lại sau"))
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insert(category)
                _returnResult.postValue(ReturnResult.Success)
            } catch (e: Exception) {
                _returnResult.postValue(ReturnResult.Error("Cập nhật danh mục thất bại! Hãy thử lại sau"))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(category)
                _returnResult.postValue(ReturnResult.Success)
            } catch (e: Exception) {
                _returnResult.postValue(ReturnResult.Error("Xóa danh mục thất bại! Hãy thử lại sau"))
            }
        }
    }
}