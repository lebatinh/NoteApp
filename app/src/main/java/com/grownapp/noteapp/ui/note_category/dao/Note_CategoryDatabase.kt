package com.grownapp.noteapp.ui.note_category.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

@Database(entities = [Note::class, Category::class, Note_Category::class], version = 1, exportSchema = false)
abstract class Note_CategoryDatabase : RoomDatabase() {
    abstract fun noteCategoryDao(): Note_CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: Note_CategoryDatabase? = null

        fun getDatabase(context: Context): Note_CategoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Note_CategoryDatabase::class.java,
                    "note_category_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}