package com.grownapp.noteapp.ui.note.adapter

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.databinding.NoteContentItemBinding

class NoteContentListAdapter(
    private val onItemCheckedChanged: (position: Int, isChecked: Boolean) -> Unit,
    private val onItemTextChanged: (position: Int, text: SpannableStringBuilder) -> Unit,
    private val onItemDeleted: (position: Int) -> Unit
) : RecyclerView.Adapter<NoteContentListAdapter.ViewHolder>() {

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
        }
    }

    fun convertCheckListToSpannable(): SpannableStringBuilder {
        val spannableBuilder = SpannableStringBuilder()
        if (items.isEmpty()) {
            return spannableBuilder
        }

        for (item in items) {
            spannableBuilder.append(item.text).append("\n")
        }
        if (spannableBuilder.isNotEmpty()) spannableBuilder.delete(
            spannableBuilder.length - 1,
            spannableBuilder.length
        )
        return spannableBuilder
    }

    inner class ViewHolder(private val binding: NoteContentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item = items[position]

            binding.ckbNoteItem.isChecked = item.isChecked
            binding.edtItemNote.text = item.text

            binding.ckbNoteItem.setOnCheckedChangeListener { _, isChecked ->
                if (item.isChecked != isChecked) {
                    onItemCheckedChanged(position, isChecked)
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
                    val newText = SpannableStringBuilder(s ?: "")
                    if (item.text.toString() != newText.toString()) {
                        onItemTextChanged(position, newText)
                        item.text = newText
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            binding.imgDeleteNoteItem.setOnClickListener {
                onItemDeleted(position)
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

data class ChecklistItem(
    var text: SpannableStringBuilder,
    var isChecked: Boolean
)
