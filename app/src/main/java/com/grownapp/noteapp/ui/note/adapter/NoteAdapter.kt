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
    private val selectedNotes: MutableSet<Note> = mutableSetOf()

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(note: Note) {
            title.text = if (note.title.isNullOrEmpty()) "Untitled" else note.title
//            content.text = note.note?.let { stripFormatting(it) }
            noteTime.text =
                if (hideCreated) "Last edit: ${note.timeLastEdit}" else "Created: ${note.timeCreate}"

            itemView.setOnClickListener {
                if (selectedNotes.isNotEmpty()) {
                    toggleSelection(note) // Chuyển đổi lựa chọn nếu đang ở chế độ chọn
                } else {
                    onClickNote(note)
                }
            }

            itemView.setOnLongClickListener {
                onLongClickNote(note)
                true
            }
            itemView.isSelected = selectedNotes.contains(note)
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

    fun updateHideCreated(hide: Boolean) {
        hideCreated = hide
        notifyDataSetChanged()
    }

    private fun toggleSelection(note: Note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note)
        } else {
            selectedNotes.add(note)
        }
        notifyDataSetChanged()
    }
    fun clearSelection() {
        selectedNotes.clear()
        notifyDataSetChanged() // Cập nhật lại danh sách để ẩn lựa chọn
    }
    fun selectAllNotes() {
        selectedNotes.clear()
        selectedNotes.addAll(noteList) // Chọn tất cả ghi chú
        notifyDataSetChanged() // Cập nhật lại danh sách
    }
    fun getAllNotes(): List<Note> {
        return noteList // Trả về danh sách tất cả ghi chú
    }
    // TODO: khi tìm kiếm thì cập nhật ui của noteItem
}