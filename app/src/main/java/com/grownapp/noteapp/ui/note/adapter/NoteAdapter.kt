package com.grownapp.noteapp.ui.note.adapter

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

    private var noteList = listOf<Note>()

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(note: Note) {
            title.text = note.title
            content.text = note.note
            time.text = note.time

            itemView.setOnClickListener {
                onClickNote(note)
            }

            itemView.setOnLongClickListener {
                onDelete(note)
                true
            }
        }

        val title: TextView = itemView.findViewById(R.id.noteTitle)
        val content: TextView = itemView.findViewById(R.id.noteContent)
        val time: TextView = itemView.findViewById(R.id.noteTime)
        val noteCategory: TextView = itemView.findViewById(R.id.noteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(noteList[position])
    }

    override fun getItemCount(): Int = noteList.size

    fun updateListNote(newListNote: List<Note>) {
        noteList = newListNote
        notifyDataSetChanged()
    }

    // TODO: khi tìm kiếm thì cập nhật ui của noteItem
}