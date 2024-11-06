package com.grownapp.noteapp.ui.note.adapter

import android.annotation.SuppressLint
import android.util.Log
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
    private var hideCreated: (Boolean) = true,
    private var listNoteSelectedAdapter: (MutableList<Note>),
    private val updateCountCallback: () -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList = listOf<Note>()
    private var isEditMode = false

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(note: Note) {
            title.text = if (note.title.isNullOrEmpty()) "Untitled" else note.title
//            content.text = note.note?.let { stripFormatting(it) }
            noteTime.text =
                if (hideCreated) "Last edit: ${note.timeLastEdit}" else "Created: ${note.timeCreate}"

            itemView.setBackgroundResource(
                if (isEditMode && listNoteSelectedAdapter.contains(note)) R.drawable.long_click_item_background
                else R.drawable.border_item_note
            )

            itemView.setOnClickListener {
                if (isEditMode) {
                    if (listNoteSelectedAdapter.contains(note)) {
                        listNoteSelectedAdapter.remove(note)
                        itemView.setBackgroundResource(R.drawable.border_item_note) // nền mặc định khi bỏ chọn
                    } else {
                        listNoteSelectedAdapter.add(note)
                        itemView.setBackgroundResource(R.drawable.long_click_item_background)
                    }
                    updateCountCallback()
                } else {
                    listNoteSelectedAdapter.clear()
                    onClickNote(note)
                }
                Log.d("listNoteSelectedAdapter", listNoteSelectedAdapter.size.toString())
            }

            itemView.setOnLongClickListener {
                if (!isEditMode) {
                    isEditMode = true
                    listNoteSelectedAdapter.clear()
                    listNoteSelectedAdapter.add(note)
                    itemView.setBackgroundResource(R.drawable.long_click_item_background)
                }
                Log.d("listNoteSelectedAdapter", listNoteSelectedAdapter.size.toString())
                updateCountCallback()
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

    fun exitEditMode(){
        isEditMode = false
        listNoteSelectedAdapter.clear()
        notifyDataSetChanged()
    }

    fun updateListNoteSelected(selectedNotes: List<Note>) {
        listNoteSelectedAdapter.clear()
        listNoteSelectedAdapter.addAll(selectedNotes)
        notifyDataSetChanged()
    }

    fun selectAllNotes() {
        isEditMode = true
        listNoteSelectedAdapter.clear()
        listNoteSelectedAdapter.addAll(noteList)
        notifyDataSetChanged()
        updateCountCallback() // Gọi lại callback để cập nhật giao diện
    }

    // TODO: khi tìm kiếm thì cập nhật ui của noteItem
}