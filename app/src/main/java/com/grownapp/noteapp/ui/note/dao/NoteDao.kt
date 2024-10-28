package com.grownapp.noteapp.ui.note.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import com.grownapp.noteapp.ui.note_category.NoteWithCategories

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM note")
    fun getAllNote(): LiveData<List<Note>>

    @Query("SELECT * FROM note WHERE noteId = :id LIMIT 1")
    fun getNoteById(id: Int): LiveData<Note>

    @Query("SELECT * FROM note WHERE title LIKE :searchQuery OR note LIKE :searchQuery")
    fun searchNote(searchQuery: String): LiveData<List<Note>>

    @Transaction
    @Query("SELECT * FROM Note WHERE noteId IN (SELECT noteId FROM note_category WHERE categoryId = :categoryId)")
    fun getNotesByCategory(categoryId: Int): LiveData<List<NoteWithCategories>>

    @Insert
    suspend fun insertNoteCategoryCrossRef(noteCategoryCrossRef: NoteCategoryCrossRef)

    @Query("""
        SELECT * FROM Note 
        WHERE noteId NOT IN (SELECT noteId FROM note_category)
    """)
    fun getNotesWithoutCategory(): LiveData<List<Note>>
}