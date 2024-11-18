package com.grownapp.noteapp.ui.note.adapter

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.R

class NoteContentListAdapter(
    private var formattedTextSegments: SpannableStringBuilder,
    private var isChecklistMode: Boolean
): RecyclerView.Adapter<NoteContentListAdapter.NoteContentListViewHolder>() {

    private var items: List<CharSequence> = parseItems()

    private fun parseItems(): List<CharSequence> {
        return if (isChecklistMode) {
            formattedTextSegments.split("\n").map { SpannableStringBuilder(it.trim()) }
        } else {
            listOf(formattedTextSegments)
        }
    }

    class NoteContentListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val ckbNoteItem: CheckBox = itemView.findViewById(R.id.ckbNoteItem)
        val edtItemNote: EditText = itemView.findViewById(R.id.edtItemNote)
        val imgDeleteNoteItem: ImageView = itemView.findViewById(R.id.imgDeleteNoteItem)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteContentListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_content_item, parent, false)
        return NoteContentListViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NoteContentListViewHolder,
        position: Int
    ) {
        val item = items[position]

        holder.edtItemNote.text = item as? Editable ?: SpannableStringBuilder(item)
        holder.edtItemNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    updateItem(position, SpannableStringBuilder(s))
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        if (isChecklistMode) {
            holder.imgDeleteNoteItem.setOnClickListener {
                removeItem(position)
            }
        }
    }

    override fun getItemCount(): Int =  items.size

    private fun removeItem(position: Int) {
        if (isChecklistMode) {
            items = items.toMutableList().apply { removeAt(position) }
            formattedTextSegments = SpannableStringBuilder(items.joinToString("\n"))
            notifyItemRemoved(position)
        }
    }

    private fun updateItem(position: Int, newText: CharSequence) {
        val mutableItems = items.toMutableList()
        mutableItems[position] = newText
        items = mutableItems
        formattedTextSegments = SpannableStringBuilder(items.joinToString("\n"))
    }
    fun addItem(newText: CharSequence) {
        val mutableItems = items.toMutableList()
        mutableItems.add(newText)
        items = mutableItems

        formattedTextSegments = SpannableStringBuilder(items.joinToString("\n"))

        notifyItemInserted(items.size - 1)
    }

    fun updateChecklistMode(isChecklistMode: Boolean) {
        this.isChecklistMode = isChecklistMode
        items = if (isChecklistMode) {
            formattedTextSegments.split("\n").map { SpannableStringBuilder(it) }
        } else {
            listOf(formattedTextSegments)
        }
        notifyDataSetChanged()
    }
    fun getFormattedText(): SpannableStringBuilder {
        return if (isChecklistMode) {
            SpannableStringBuilder(items.joinToString("\n"))
        } else {
            formattedTextSegments
        }
    }

}