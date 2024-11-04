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
    private val onLongClickNote: (Note) -> Unit,
    private var hideCreated: (Boolean) = true
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList = listOf<Note>()

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(note: Note) {
            title.text = if (note.title.isNullOrEmpty()) "Untitled" else note.title
//            content.text = note.note?.let { stripFormatting(it) }
            noteTime.text =
                if (hideCreated) "Last edit: ${note.timeLastEdit}" else "Created: ${note.timeCreate}"

            itemView.setOnClickListener {
                onClickNote(note)
            }

            itemView.setOnLongClickListener {
                onLongClickNote(note)
                true
            }

        }

        private val title: TextView = itemView.findViewById(R.id.noteTitle)
        private val content: TextView = itemView.findViewById(R.id.noteContent)
        private val noteTime: TextView = itemView.findViewById(R.id.noteTime)
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