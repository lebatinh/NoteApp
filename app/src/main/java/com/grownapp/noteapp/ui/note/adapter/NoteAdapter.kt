package com.grownapp.noteapp.ui.note.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.NoteItemBinding
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.support.NoteContent

class NoteAdapter(
    private val onClickNote: (Note) -> Unit,
    private val onLongClickNote: (Note) -> Unit,
    private var hideCreated: (Boolean) = true,
    var listNoteSelectedAdapter: MutableLiveData<MutableList<Note>>,
    private val getCategoryOfNote: (Int) -> LiveData<List<Category>>,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList = listOf<Note>()
    private var isEditMode = false
        set(value) {
            field = value
            listNoteSelectedAdapter.value = mutableListOf()
        }

    private fun updateSelectedNotes(action: (MutableList<Note>) -> Unit) {
        val currentList = listNoteSelectedAdapter.value ?: mutableListOf()
        action(currentList)
        listNoteSelectedAdapter.value = currentList
    }

    inner class NoteViewHolder(private val binding: NoteItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            bindTitleAndContent(note)
            bindCategory(note)
            bindTime(note)
            setupClickListeners(note)
            updateBackground(note)
        }

        private fun bindTitleAndContent(note: Note) {
            val titleText = note.title?.takeIf { it.isNotBlank() }
            val contentText = note.note?.takeIf { it.isNotBlank() }?.let {
                val noteContent = Gson().fromJson(it, NoteContent::class.java)
                noteContentToPlainText(noteContent)
            }

            when {
                !titleText.isNullOrEmpty() -> {
                    binding.noteTitle.text = titleText
                    binding.noteTitle.visibility = View.VISIBLE
                    binding.noteContent.visibility = View.GONE
                }

                !contentText.isNullOrEmpty() -> {
                    binding.noteContent.text = contentText
                    binding.noteContent.visibility = View.VISIBLE
                    binding.noteTitle.visibility = View.GONE
                }

                else -> {
                    binding.noteTitle.visibility = View.VISIBLE
                    "Untitled".also { binding.noteTitle.text = it }
                    binding.noteContent.visibility = View.GONE
                }
            }
        }

        private fun bindCategory(note: Note) {
            getCategoryOfNote(note.noteId).removeObservers(lifecycleOwner)
            getCategoryOfNote(note.noteId).observe(lifecycleOwner) { categoryList ->
                if (!categoryList.isNullOrEmpty()) {
                    binding.noteCategory.visibility = View.VISIBLE
                    val categoryName = categoryList.joinToString(", ") { it.name }
                    binding.noteCategory.text = categoryName
                } else {
                    binding.noteCategory.visibility = View.GONE
                }
            }
        }

        private fun bindTime(note: Note) {
            binding.noteTime.text =
                if (hideCreated) "Last edit: ${note.timeLastEdit}" else "Created: ${note.timeCreate}"
        }

        private fun setupClickListeners(note: Note) {
            binding.root.setOnClickListener {
                if (isEditMode) {
                    toggleNoteSelection(note)
                } else {
                    onClickNote(note)
                }
            }

            binding.root.setOnLongClickListener {
                if (!isEditMode) {
                    enterEditMode(note)
                }
                onLongClickNote(note)
                true
            }
        }

        private fun toggleNoteSelection(note: Note) {
            updateSelectedNotes { list ->
                if (list.contains(note)) {
                    list.remove(note)
                } else {
                    list.add(note)
                }
            }
            updateBackground(note)
        }

        private fun enterEditMode(note: Note) {
            isEditMode = true
            updateSelectedNotes { list -> list.add(note) }
            updateBackground(note)
        }

        private fun updateBackground(note: Note) {
            val isSelected = listNoteSelectedAdapter.value?.contains(note)
            val drawable = getNoteBackgroundDrawable(binding.root.context, note.backgroundColor)

            if (isSelected == true) {
                val blendedDrawable = if (note.backgroundColor == null) {
                    ContextCompat.getDrawable(binding.root.context, R.drawable.long_click_item_background)
                } else {
                    val blendedColor = blendColors(
                        ContextCompat.getColor(binding.root.context, R.color.bottomBackgroundColorLongClick),
                        note.backgroundColor!!
                    )
                    applyBlendedColorsToDrawable(drawable, blendedColor, binding.root.context)
                }
                binding.root.background = blendedDrawable
            } else {
                binding.root.background = drawable
            }
        }

        private fun getNoteBackgroundDrawable(context: Context, backgroundColor: Int?): GradientDrawable {
            val drawable = ContextCompat.getDrawable(context, R.drawable.border_item_note) as GradientDrawable
            if (backgroundColor != null) {
                drawable.mutate()
                val white = ContextCompat.getColor(context, R.color.topBackgroundItem)
                drawable.colors = intArrayOf(white, backgroundColor)
            }
            return drawable
        }

        private fun applyBlendedColorsToDrawable(
            drawable: GradientDrawable,
            blendedColor: Int,
            context: Context
        ): GradientDrawable {
            val white = ContextCompat.getColor(context, R.color.topBackgroundItem)
            drawable.colors = intArrayOf(white, blendedColor)
            return drawable
        }

        private fun blendColors(color1: Int, color2: Int): Int {
            fun blendComponent(c1: Int, c2: Int) = (c1 * 0.5 + c2 * 0.5).toInt()
            return Color.rgb(
                blendComponent(Color.red(color1), Color.red(color2)),
                blendComponent(Color.green(color1), Color.green(color2)),
                blendComponent(Color.blue(color1), Color.blue(color2))
            )
        }

        private fun noteContentToPlainText(noteContent: NoteContent): String {
            val plainTextBuilder = StringBuilder()
            for (segment in noteContent.segments) {
                plainTextBuilder.append(segment.text)
            }
            return plainTextBuilder.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = NoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(noteList[position])
    }

    override fun getItemCount(): Int = noteList.size

    fun updateListNote(newListNote: List<Note>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = noteList.size
            override fun getNewListSize() = newListNote.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                noteList[oldItemPosition].noteId == newListNote[newItemPosition].noteId
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                noteList[oldItemPosition] == newListNote[newItemPosition]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        noteList = newListNote
        diffResult.dispatchUpdatesTo(this)
    }

    fun setHideCreated(hide: Boolean) {
        if (hideCreated != hide) {
            hideCreated = hide
            notifyDataSetChanged()
        }
    }

    fun exitEditMode() {
        isEditMode = false
        listNoteSelectedAdapter.value = mutableListOf()
        notifyDataSetChanged()
    }

    fun updateListNoteSelected(selectedNotes: List<Note>) {
        isEditMode = true
        listNoteSelectedAdapter.value = selectedNotes.toMutableList()
        notifyDataSetChanged()
    }

    fun selectAllNotes() {
        if (!isEditMode){
            isEditMode = true
        }
        if (listNoteSelectedAdapter.value?.size != noteList.size) {
            listNoteSelectedAdapter.value = noteList.toMutableList()
            notifyDataSetChanged()
        }
    }
}