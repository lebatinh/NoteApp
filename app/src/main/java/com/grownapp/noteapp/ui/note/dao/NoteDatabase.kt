package com.grownapp.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.categories.dao.CategoryDao
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.dao.NoteDao
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef

@Database(
    entities = [Note::class, Category::class, NoteCategoryCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}