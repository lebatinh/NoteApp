package com.grownapp.noteapp.ui.categories.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val categoryId: Int = 0,
    val name: String
)