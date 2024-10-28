package com.grownapp.noteapp.ui.note.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM note")
    fun getAllNote(): LiveData<List<Note>>

    @Query("SELECT * FROM note WHERE id = :id LIMIT 1")
    fun getNoteById(id: Int): LiveData<Note>

    @Query("SELECT * FROM note WHERE title LIKE :searchQuery OR note LIKE :searchQuery")
    fun searchNote(searchQuery: String): LiveData<List<Note>>
}