package com.grownapp.noteapp.ui.note.adapter

import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.databinding.NoteContentItemBinding
import com.grownapp.noteapp.ui.note.support.ChecklistItem
import com.grownapp.noteapp.ui.note.support.FormatTextSupport

class NoteContentListAdapter : RecyclerView.Adapter<NoteContentListAdapter.ViewHolder>() {

    private val items = mutableListOf<ChecklistItem>()

    fun setItems(newItems: List<ChecklistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: ChecklistItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size - position)
        }
    }

    fun convertCheckListToSpannable(context: Context): SpannableStringBuilder {
        val spannableBuilder = SpannableStringBuilder()
        if (items.isEmpty()) {
            return spannableBuilder
        }

        for (item in items) {
            val spannable = FormatTextSupport().noteContentToSpannable(context, item.text)
            spannableBuilder.append(spannable).append("\n")
        }
        if (spannableBuilder.isNotEmpty()) spannableBuilder.delete(
            spannableBuilder.length - 1,
            spannableBuilder.length
        )
        return spannableBuilder
    }

    fun spannableToChecklistItems(context: Context, spannable: SpannableStringBuilder): List<ChecklistItem> {
        val items = mutableListOf<ChecklistItem>()
        val lines = spannable.split("\n")
        var start = 0
        for (line in lines) {
            val end = start + line.length
            val lineSpannable = SpannableStringBuilder(spannable.subSequence(start, end))
            val noteContent = FormatTextSupport().spannableToNoteContent(context, lineSpannable)
            items.add(ChecklistItem(noteContent, false))
            start = end + 1
        }
        return items
    }

    inner class ViewHolder(private val binding: NoteContentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item = items[position]

            val spannableText = FormatTextSupport().noteContentToSpannable(binding.root.context, item.text)
            binding.edtItemNote.text = spannableText

            binding.ckbNoteItem.setOnCheckedChangeListener { _, isChecked ->
                if (item.isChecked != isChecked) {
                    item.isChecked = isChecked
                }
            }

            binding.edtItemNote.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val newText = SpannableStringBuilder(s ?: "")
                    val newNoteContent = FormatTextSupport().spannableToNoteContent(binding.root.context, newText)
                    if (item.text != newNoteContent) {
                        item.text = newNoteContent
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            binding.imgDeleteNoteItem.setOnClickListener {
                removeItem(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            NoteContentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size
}
