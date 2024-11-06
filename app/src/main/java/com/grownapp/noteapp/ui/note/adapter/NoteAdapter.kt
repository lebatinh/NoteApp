package com.grownapp.noteapp.ui.note.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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

            itemView.apply {
                val isSelected = isEditMode && listNoteSelectedAdapter.contains(note)
                updateBackground(this, note, isSelected)
            }

            itemView.setOnClickListener {
                if (isEditMode) {
                    if (listNoteSelectedAdapter.contains(note)) {
                        listNoteSelectedAdapter.remove(note)
                        updateBackground(itemView, note, false)
                    } else {
                        // Chọn note, đặt nền cho chế độ chọn
                        listNoteSelectedAdapter.add(note)
                        updateBackground(itemView, note, true)
                    }
                    updateCountCallback() // Cập nhật số lượng item được chọn
                    notifyDataSetChanged()
                } else {
                    listNoteSelectedAdapter.clear() // Xóa hết các item đã chọn nếu không ở chế độ Edit
                    exitEditMode()
                    onClickNote(note)// Xử lý sự kiện khi click vào note
                }
                Log.d("listNoteSelectedAdapter", listNoteSelectedAdapter.size.toString())
            }

            itemView.setOnLongClickListener {
                if (!isEditMode) {
                    isEditMode = true
                    listNoteSelectedAdapter.clear()
                    listNoteSelectedAdapter.add(note)
                    updateCountCallback()
                    updateBackground(itemView, note, true)
                }
                Log.d("listNoteSelectedAdapter", listNoteSelectedAdapter.size.toString())
                onLongClickNote(note)
                true
            }
        }

        private val title: TextView = itemView.findViewById(R.id.noteTitle)
        private val content: TextView = itemView.findViewById(R.id.noteContent)
        private val noteTime: TextView = itemView.findViewById(R.id.noteTime)

        // Hàm trộn màu
        private fun blendColors(color1: Int, color2: Int): Int {
            val r1 = Color.red(color1)
            val g1 = Color.green(color1)
            val b1 = Color.blue(color1)

            val r2 = Color.red(color2)
            val g2 = Color.green(color2)
            val b2 = Color.blue(color2)

            val r = (r1 * (1 - 0.5) + r2 * 0.5).toInt()
            val g = (g1 * (1 - 0.5) + g2 * 0.5).toInt()
            val b = (b1 * (1 - 0.5) + b2 * 0.5).toInt()

            return Color.rgb(r, g, b)
        }

        private fun updateBackground(view: View, note: Note, isSelected: Boolean) {
            val drawable: GradientDrawable

            // Nếu note không có màu nền, sử dụng background mặc định
            if (note.backgroundColor == null) {
                drawable = ContextCompat.getDrawable(
                    view.context,
                    R.drawable.border_item_note
                ) as GradientDrawable
            } else {
                drawable = ContextCompat.getDrawable(
                    view.context,
                    R.drawable.border_item_note
                ) as GradientDrawable
                drawable.mutate() // Để tránh ảnh hưởng đến các item khác
            }

            if (isSelected) {
                // Khi item được chọn trong chế độ long click hoặc editMode
                if (note.backgroundColor == null) {
                    // Nếu không có màu nền, chỉ sử dụng viền long click
                    view.background = ContextCompat.getDrawable(
                        view.context,
                        R.drawable.long_click_item_background
                    )
                } else {
                    // Nếu có màu nền, trộn màu giữa note và long click background
                    val longClickColor =
                        ContextCompat.getColor(view.context, R.color.bottomBackgroundColorLongClick)
                    val blendedColor = blendColors(longClickColor, note.backgroundColor!!)
                    drawable.colors = intArrayOf(Color.WHITE, blendedColor)
                    view.background = drawable
                }
            } else {
                // Khi không chọn
                if (note.backgroundColor == null) {
                    // Nếu không có màu nền, sử dụng viền mặc định
                    view.setBackgroundResource(R.drawable.border_item_note)
                } else {
                    // Nếu có màu nền, tạo gradient từ white đến màu của note
                    drawable.colors = intArrayOf(Color.WHITE, note.backgroundColor!!)
                    view.background = drawable
                }
            }
        }

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

    fun exitEditMode() {
        isEditMode = false
        listNoteSelectedAdapter.clear()
        updateCountCallback()
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
        updateCountCallback()
        notifyDataSetChanged() // Gọi lại callback để cập nhật giao diện
    }
    // TODO: Thêm replace backgroundColor trong room và đặt màu chỉ cho danh sách item chọn chứ k phải all
    // TODO: xem lại cách trộn màu khi item có màu nền khác null (khi click và longclick)
    // TODO: khi tìm kiếm thì cập nhật ui của noteItem
}