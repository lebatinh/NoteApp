package com.grownapp.noteapp.ui.note

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentTrashBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note

class TrashFragment : Fragment(), MenuProvider {

    private var _binding: FragmentTrashBinding? = null

    private val binding get() = _binding!!
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel

    private lateinit var listNoteSelected: MutableList<Note>

    private var isEditMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        listNoteSelected = mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        noteAdapter = NoteAdapter(
            onClickNote = {
                dialogDeleteOrUndelete(it)
            },
            onLongClickNote = { note ->
                startEditMode(!isEditMode)
            },
            hideCreated = true,
            listNoteSelectedAdapter = listNoteSelected,
            updateCountCallback = { updateCountNoteSeleted() },
            getCategoryOfNote = { noteId -> noteViewModel.getCategoryOfNote(noteId) }
        )

        binding.rcvNoteTrash.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteTrash.adapter = noteAdapter

        noteViewModel.allTrashNote.observe(viewLifecycleOwner) {
            it.let {
                noteAdapter.updateListNote(it)
            }
        }
        return root
    }

    private fun updateCountNoteSeleted() {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val tvCountSeletedTrash = toolbar?.findViewById<TextView>(R.id.tvCountSeletedTrash)
        Log.d("listNoteSelectedTrash", listNoteSelected.toString())
        tvCountSeletedTrash?.text = listNoteSelected.size.toString()
    }

    private fun startEditMode(isVisible: Boolean) {
        isEditMode = isVisible
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        Log.d("startEditMode", "isEditMode: $isEditMode")
        if (isVisible) {
            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )

            val trashLayout = layoutInflater.inflate(R.layout.custom_trash_toolbar, toolbar, false)
            toolbar.addView(trashLayout, layoutParams)

            val tvCountSeletedTrash = toolbar.findViewById<TextView>(R.id.tvCountSeletedTrash)
            val imgRestore = toolbar.findViewById<ImageView>(R.id.imgRestore)
            val imgSelectAll = toolbar.findViewById<ImageView>(R.id.imgSelectAll)

            Log.d("startEditMode", listNoteSelected.size.toString())
            tvCountSeletedTrash.text = listNoteSelected.size.toString()

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                startEditMode(false)
                noteAdapter.exitEditMode()
                tvCountSeletedTrash?.visibility = View.GONE
                imgRestore?.visibility = View.GONE
                imgSelectAll?.visibility = View.GONE
                toolbar.title = "Trash"
                toolbar.setNavigationIcon(R.drawable.nav)
                toolbar.setNavigationOnClickListener {
                    (activity as MainActivity).setupDefaultToolbar()
                }
            }

            imgRestore.setOnClickListener {
                restoreDialog(true)
            }

            imgSelectAll.setOnClickListener {
                if (listNoteSelected.isEmpty()) {
                    noteViewModel.allTrashNote.observe(viewLifecycleOwner) {
                        listNoteSelected = it.toMutableList()
                        noteAdapter.updateListNoteSelected(listNoteSelected)
                        Log.d("TrashFragment", "imgSelectAll")
                        Log.d("TrashFragment", listNoteSelected.size.toString())
                    }
                } else {
                    listNoteSelected.clear()
                    noteAdapter.updateListNoteSelected(listNoteSelected)
                    Log.d("TrashFragment", "imgSelectAll")
                    Log.d("TrashFragment", listNoteSelected.size.toString())
                }
                updateCountNoteSeleted()
            }
        } else {
            Log.d("startEditMode", "Exiting Edit Mode")
            val trashLayout = toolbar.findViewById<View>(R.id.custom_trash_toolbar)
            if (trashLayout != null) {
                toolbar.removeView(trashLayout)
            }
            toolbar.setNavigationIcon(R.drawable.nav)
            toolbar.setNavigationOnClickListener {
                (activity as MainActivity).setupDefaultToolbar()
            }
        }

        requireActivity().invalidateOptionsMenu()
    }

    private fun dialogDeleteOrUndelete(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.undelete_dialog, null)

        val noteTitle = dialogView.findViewById<TextView>(R.id.noteTitle)
        val noteContent = dialogView.findViewById<TextView>(R.id.noteContent)
        val radiogroup = dialogView.findViewById<RadioGroup>(R.id.radiogroup)
        val btnOk = dialogView.findViewById<TextView>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        var isDelete = false

        noteTitle.text = note.title ?: ""

        if (note.note != null) {
            noteContent.text =
                noteContentToSpannable(Gson().fromJson(note.note, NoteContent::class.java))
        } else {
            "".also { noteContent.text = it }
        }

        radiogroup.setOnCheckedChangeListener { _, checkedId ->
            isDelete = checkedId == R.id.rdbDelete
        }

        btnOk.setOnClickListener {
            if (isDelete) {
                dialogDelete(note)
            } else {
                noteViewModel.pushInTrash(false, note.noteId)
                noteAdapter.exitEditMode()
                startEditMode(false)
                Toast.makeText(requireContext(), "Restore note", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dialogDelete(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
        val deleteLog = dialogView.findViewById<TextView>(R.id.delete_log)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()
        noteViewModel.getNoteById(note.noteId).observe(viewLifecycleOwner) { note ->
            note?.let {
                deleteLog.text = buildString {
                    append("The note will be deleted permanently!\n")
                    append(
                        "Are you sure that you want to delete the '${
                            note.title ?: note.note?.take(20) ?: "Untitled"
                        }' note?"
                    )
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            noteViewModel.delete(note.noteId)

            Toast.makeText(requireContext(), "Deleted notes", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dialogDeleteSeleted() {
        val dialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
        val deleteLog = dialogView.findViewById<TextView>(R.id.delete_log)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()

        deleteLog.text = buildString {
            append(
                "Are you sure that you want to delete the selected notes?\n"
            )
            append("The notes will be deleted permanently!")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            for (note in listNoteSelected) {
                noteViewModel.delete(note.noteId)
            }
            Toast.makeText(requireContext(), "Deleted notes", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            noteAdapter.exitEditMode()
            startEditMode(false)
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()

        menuInflater.inflate(R.menu.menu_trash, menu)
        Log.d("TrashFragment", isEditMode.toString())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_more_trash -> {
                showMoreDialog(isEditMode)
                true
            }
        }
        return false
    }

    private fun showMoreDialog(isEditMode: Boolean) {
        val anchorView = requireActivity().findViewById<View>(R.id.item_more_trash)

        val popupMenu = PopupMenu(requireContext(), anchorView)

        if (isEditMode) {
            popupMenu.menuInflater.inflate(R.menu.trash_more_editmode, popupMenu.menu)
        } else {
            popupMenu.menuInflater.inflate(R.menu.trash_more, popupMenu.menu)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.undelete_all -> {
                    restoreDialog(true)
                    Log.d("TrashFragment", "undelete_all")
                    true
                }

                R.id.export_notes_to_text_files_trash -> {
                    Log.d("TrashFragment", "export_notes_to_text_files_trash")
                    true
                }

                R.id.empty_trash -> {
                    Log.d("TrashFragment", "empty_trash")
                    restoreDialog(false)
                    true
                }

                R.id.delete_trash -> {
                    if (isEditMode) {
                        dialogDeleteSeleted()
                    }

                    Log.d("TrashFragment", "delete_trash")
                    true
                }

                else -> false
            }
        }

        // Hiển thị PopupMenu
        popupMenu.show()
    }

    private fun restoreDialog(isEmptyTrash: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.restore_dialog, null)
        val tvRestore = dialogView.findViewById<TextView>(R.id.tvRestore)
        val tvYes = dialogView.findViewById<TextView>(R.id.tvYes)
        val tvNo = dialogView.findViewById<TextView>(R.id.tvNo)

        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()

        tvRestore.text =
            if (isEmptyTrash) "Restore all notes?" else "All trashed notes will be deleted permanently. Are you sure that you want to delete all of the trashed notes?"

        tvNo.setOnClickListener {
            dialog.dismiss()
        }

        tvYes.setOnClickListener {
            if (!isEmptyTrash) {
                for (note in listNoteSelected) {
                    noteViewModel.emptyTrash()
                }
                Toast.makeText(requireContext(), "Deleted notes", Toast.LENGTH_SHORT).show()
            } else {
                for (note in listNoteSelected) {
                    noteViewModel.restoreAllNote()
                }
                Toast.makeText(requireContext(), "Restored notes", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()

            noteAdapter.exitEditMode()
            startEditMode(false)
        }
        dialog.show()
    }

    private fun noteContentToSpannable(noteContent: NoteContent): SpannableStringBuilder {
        val defautBackgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)
        val spannable = SpannableStringBuilder()

        for (segment in noteContent.segments) {
            val start = spannable.length
            spannable.append(segment.text)
            val end = spannable.length

            segment.apply {
                if (isBold == true) spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isItalic == true) spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isUnderline == true) spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isStrikethrough == true) spannable.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                backgroundColor.let {
                    spannable.setSpan(
                        BackgroundColorSpan(it ?: defautBackgroundColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textColor.let {
                    spannable.setSpan(
                        ForegroundColorSpan(it ?: defautTextColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textSize.let {
                    spannable.setSpan(
                        AbsoluteSizeSpan(it?.toInt() ?: 18),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return spannable
    }
}