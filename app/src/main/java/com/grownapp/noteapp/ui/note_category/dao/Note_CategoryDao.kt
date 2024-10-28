package com.grownapp.noteapp.ui.note_category.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note

@Dao
interface Note_CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNoteToCategory(noteCategory: Note_Category)

    @Query("DELETE FROM note_category WHERE noteId = :noteId")
    suspend fun removeNoteCategory(noteId: Int)

    // Lấy all note mà k thuộc category nào (Uncategorized)
    @Query(
        """
    SELECT * FROM note
    WHERE id NOT IN (
        SELECT noteId FROM note_category
    )
    """
    )
    fun getNotesWithoutCategory(): LiveData<List<Note>>

    // Lấy all note cho danh mục nào đó
    @Transaction
    @Query(
        """
        SELECT note.*
        FROM note
        INNER JOIN note_category ON note.id = note_category.noteId
        WHERE note_category.categoryId  = :categoryId 
    """
    )
    fun getAllNoteOnCategory(categoryId : Int): LiveData<List<NoteWithCategories>>

    // Lấy all danh mục của note
    @Transaction
    @Query(
        """
        SELECT category.*
        FROM category
        INNER JOIN note_category ON category.id = note_category.categoryId 
        WHERE note_category.noteId = :noteId
    """
    )
    fun getAllCategoryOfNote(noteId: Int): LiveData<List<Category>>
}