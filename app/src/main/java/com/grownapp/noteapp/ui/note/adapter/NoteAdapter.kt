package com.grownapp.noteapp.ui.note.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.R
import com.grownapp.noteapp.ui.note.dao.Note

class NoteAdapter(
    private val onClickNote: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()

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
        val note = notes[position]
        holder.title.text = if (note.title.toString()
                .isEmpty() || note.title.toString() == ""
        ) "Untitled" else note.title
        holder.content.text = note.note
        holder.time.text = "Last edit: ${note.time}"
        holder.noteCategory.text = note.category

        holder.itemView.setOnClickListener {
            onClickNote(note)
        }
        holder.itemView.setOnLongClickListener {
            onDelete(note)
            true
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateListNote(newNote: List<Note>) {
        notes = newNote
        notifyDataSetChanged()
    }

    // TODO: khi tìm kiếm thì cập nhật ui của noteItem
}