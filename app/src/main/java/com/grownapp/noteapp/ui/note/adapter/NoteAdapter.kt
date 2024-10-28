package com.grownapp.noteapp.ui.note.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.R
import com.grownapp.noteapp.ui.note.NoteItem
import com.grownapp.noteapp.ui.note.dao.Note

class NoteAdapter(
    private val onClickNote: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteItems: List<NoteItem> = emptyList()

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.noteTitle)
        val content: TextView = itemView.findViewById(R.id.noteContent)
        val time: TextView = itemView.findViewById(R.id.noteTime)
        val noteCategory: TextView = itemView.findViewById(R.id.noteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        when (val noteItem = noteItems[position]) {
            is NoteItem.NoteWithCategories -> {
                holder.title.text = if (noteItem.note.title.isNullOrEmpty()) "Untitled" else noteItem.note.title
                holder.content.text = noteItem.note.note
                holder.time.text = "Last edit: ${noteItem.note.time}"
                holder.noteCategory.text = if (noteItem.categories.isNotEmpty()) {
                    noteItem.categories.joinToString(", ")
                } else null

                holder.itemView.setOnClickListener {
                    onClickNote(noteItem.note)
                }
                holder.itemView.setOnLongClickListener {
                    onDelete(noteItem.note)
                    true
                }
            }
            is NoteItem.NoteWithoutCategories -> {
                holder.title.text = if (noteItem.note.title.isNullOrEmpty()) "Untitled" else noteItem.note.title
                holder.content.text = noteItem.note.note
                holder.time.text = "Last edit: ${noteItem.note.time}"
                holder.noteCategory.text = null

                holder.itemView.setOnClickListener {
                    onClickNote(noteItem.note)
                }
                holder.itemView.setOnLongClickListener {
                    onDelete(noteItem.note)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int = noteItems.size

    fun updateListNote(newNoteItems: List<NoteItem>) {
        noteItems = newNoteItems
        notifyDataSetChanged()
    }

    // TODO: khi tìm kiếm thì cập nhật ui của noteItem
}