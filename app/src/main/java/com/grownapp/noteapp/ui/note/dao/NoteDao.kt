package com.grownapp.noteapp.ui.note.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import com.grownapp.noteapp.ui.note_category.NoteWithCategories

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note): Long

    @Query("UPDATE note SET title = :title, note = :content, timeLastEdit = :updatedTime WHERE noteId = :noteId")
    suspend fun update(noteId: Int, title: String, content: String, updatedTime: String)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM note")
    fun getAllNote(): LiveData<List<Note>>

    @Query("SELECT * FROM note WHERE noteId = :id LIMIT 1")
    fun getNoteById(id: Int): LiveData<Note>

    @Query("SELECT * FROM note WHERE title LIKE :searchQuery OR note LIKE :searchQuery")
    fun searchNote(searchQuery: String): LiveData<List<Note>>

    @Query("""
    SELECT *
    FROM note
    INNER JOIN note_category nc ON note.noteId = nc.noteId
    WHERE (note.title LIKE '%' || :searchQuery || '%' OR note.note LIKE '%' || :searchQuery || '%')
      AND nc.categoryId = :categoryId
""")
    fun searchNoteWithCategory(searchQuery: String, categoryId: Int): LiveData<List<NoteWithCategories>>

    @Query("""
    SELECT *
    FROM note
    WHERE (title LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%')
      AND NOT EXISTS (
          SELECT 1
          FROM note_category nc
          WHERE nc.noteId = note.noteId
      )
    """)
    fun searchNoteWithoutCategory(searchQuery: String): LiveData<List<NoteWithCategories>>

    @Query("""
    SELECT c.*
    FROM category c
    INNER JOIN note_category nc ON c.categoryId = nc.categoryId
    INNER JOIN note n ON nc.noteId = n.noteId
    WHERE n.noteId = :noteId
    """)fun getCategoryOfNote(noteId: Int): LiveData<List<Category>>

    @Transaction
    @Query("SELECT * FROM Note WHERE noteId IN (SELECT noteId FROM note_category WHERE categoryId = :categoryId)")
    fun getNotesByCategory(categoryId: Int): LiveData<List<NoteWithCategories>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteCategoryCrossRef(noteCategoryCrossRef: NoteCategoryCrossRef)

    @Query("DELETE FROM note_category WHERE noteId = :noteId")
    suspend fun deleteCategoriesForNote(noteId: Int)

    @Query("""
        SELECT * FROM Note 
        WHERE noteId NOT IN (SELECT noteId FROM note_category)
    """)
    fun getNotesWithoutCategory(): LiveData<List<Note>>

    @Query("SELECT * FROM Note ORDER BY timeLastEdit DESC")
    fun sortedByUpdatedTimeDesc(): LiveData<List<Note>>

    @Query("SELECT * FROM Note ORDER BY timeLastEdit ASC")
    fun sortedByUpdatedTimeAsc(): LiveData<List<Note>>

    @Query("SELECT * FROM Note ORDER BY title DESC")
    fun sortedByTitleDesc(): LiveData<List<Note>>

    @Query("SELECT * FROM Note ORDER BY title ASC")
    fun sortedByTitleAsc(): LiveData<List<Note>>

    @Query("SELECT * FROM Note ORDER BY timeCreate DESC")
    fun sortedByCreatedTimeDesc(): LiveData<List<Note>>

    @Query("SELECT * FROM Note ORDER BY timeCreate ASC")
    fun sortedByCreatedTimeAsc(): LiveData<List<Note>>
}