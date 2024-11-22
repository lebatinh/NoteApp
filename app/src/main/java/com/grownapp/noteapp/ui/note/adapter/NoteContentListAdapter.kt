package com.grownapp.noteapp.ui.note.adapter

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.grownapp.noteapp.databinding.NoteContentItemBinding
import com.grownapp.noteapp.ui.note.support.ChecklistItem
import com.grownapp.noteapp.ui.note.support.FormatTextSupport
import com.grownapp.noteapp.ui.note.support.NoteContent
import com.grownapp.noteapp.ui.note.support.TextSegment

class NoteContentListAdapter : RecyclerView.Adapter<NoteContentListAdapter.ViewHolder>() {

    private val items = mutableListOf<ChecklistItem>()

    fun setItems(newItems: List<ChecklistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<ChecklistItem> {
        return items
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

    fun convertChecklistToNoteContent(): String {
        val segments = items.flatMapIndexed { index, checklistItem ->
            val lineSegments = checklistItem.text.segments.toMutableList()
            if (index < items.size - 1 && lineSegments.isNotEmpty()) {
                val lastSegment = lineSegments.last()
                lineSegments[lineSegments.lastIndex] = lastSegment.copy(
                    text = lastSegment.text + "\n"
                )
            }
            lineSegments
        }
        val noteContent = NoteContent(segments = segments)
        return Gson().toJson(noteContent)
    }

    fun setItemsFromNoteContent(json: String) {
        val noteContent = Gson().fromJson(json, NoteContent::class.java)
        val checklistItems = splitSegmentsByLines(noteContent.segments).map { lineSegments ->
            ChecklistItem(
                text = NoteContent(lineSegments),
                isChecked = false
            )
        }
        setItems(checklistItems)
    }

    private fun splitSegmentsByLines(segments: List<TextSegment>): List<List<TextSegment>> {
        val lines = mutableListOf<MutableList<TextSegment>>()
        var currentLine = mutableListOf<TextSegment>()

        for (segment in segments) {
            val parts = segment.text?.split("\n") ?: listOf("")
            for ((index, part) in parts.withIndex()) {
                if (index > 0) {
                    lines.add(currentLine)
                    currentLine = mutableListOf()
                }
                if (part.isNotEmpty()) {
                    currentLine.add(segment.copy(text = part))
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    inner class ViewHolder(private val binding: NoteContentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item = items[position]

            val spannableText =
                FormatTextSupport().noteContentToSpannable(binding.root.context, item.text)
            binding.edtItemNote.text = spannableText

            binding.ckbNoteItem.isChecked = item.isChecked
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
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let {
                        val newText = SpannableStringBuilder(it)
                        val newNoteContent =
                            FormatTextSupport().spannableToNoteContent(
                                binding.root.context,
                                newText
                            )
                        if (item.text != newNoteContent) {
                            item.text = newNoteContent
                        }
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
