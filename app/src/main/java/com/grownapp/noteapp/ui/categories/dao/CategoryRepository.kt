package com.grownapp.noteapp.ui.categories.dao

import androidx.lifecycle.LiveData

class CategoryRepository(private val categoryDao: CategoryDao) {
    val allCategory: LiveData<List<Category>> = categoryDao.getAllCategory()

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }
}