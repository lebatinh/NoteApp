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
    private val onDelete: (Note) -> Unit,
    private val hideCreated: (Boolean) = false
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList = listOf<Note>()

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(note: Note) {
            title.text = if (note.title.isNullOrEmpty()) "Untitled" else note.title
            content.text = note.note
            timeCreated.text = "Last edit: ${note.timeLastEdit}"
            timeLastEdit.text = "Created: ${note.timeCreate}"

            itemView.setOnClickListener {
                onClickNote(note)
            }

            itemView.setOnLongClickListener {
                onDelete(note)
                true
            }

            if (hideCreated){
                timeLastEdit.visibility = View.VISIBLE
                timeCreated.visibility = View.GONE
            }else{
                timeLastEdit.visibility = View.GONE
                timeCreated.visibility = View.VISIBLE
            }
        }

        private val title: TextView = itemView.findViewById(R.id.noteTitle)
        private val content: TextView = itemView.findViewById(R.id.noteContent)
        private val timeCreated: TextView = itemView.findViewById(R.id.noteTimeCreate)
        private val noteCategory: TextView = itemView.findViewById(R.id.noteCategory)
        private val timeLastEdit: TextView = itemView.findViewById(R.id.noteTimeLastEdit)
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