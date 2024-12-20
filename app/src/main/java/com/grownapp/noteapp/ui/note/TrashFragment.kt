package com.grownapp.noteapp.ui.note

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentTrashBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.support.FileProcess
import com.grownapp.noteapp.ui.note.support.FormatTextSupport
import com.grownapp.noteapp.ui.note.support.NoteContent

class TrashFragment : Fragment(), MenuProvider {

    private var _binding: FragmentTrashBinding? = null

    private val binding get() = _binding!!
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel

    private var editMode = false
    private var selectedDirectoryUri: Uri? = null

    private var notesToExport: List<Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
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
            onLongClickNote = {
                startEditMode(!editMode)
            },
            hideCreated = true,
            listNoteSelectedAdapter = MutableLiveData(mutableListOf()),
            getCategoryOfNote = { noteId -> noteViewModel.getCategoryOfNote(noteId) },
            lifecycleOwner = viewLifecycleOwner
        )
        noteAdapter.listNoteSelectedAdapter.observe(viewLifecycleOwner) { selectedNotes ->
            updateCountNoteSelected(selectedNotes.size)
        }
        binding.rcvNoteTrash.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteTrash.adapter = noteAdapter

        noteViewModel.allTrashNote.observe(viewLifecycleOwner) {
            it?.let {
                noteAdapter.updateListNote(it)
            }
        }
        return root
    }

    private fun updateCountNoteSelected(count: Int) {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val tvCountSelectedTrash = toolbar?.findViewById<TextView>(R.id.tvCountSeletedTrash)
        tvCountSelectedTrash?.text = count.toString()
    }

    override fun onPause() {
        super.onPause()
        startEditMode(false)
    }

    private fun startEditMode(isVisible: Boolean) {
        editMode = isVisible
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        if (isVisible) {
            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )

            val trashLayout = layoutInflater.inflate(R.layout.custom_trash_toolbar, toolbar, false)
            toolbar.addView(trashLayout, layoutParams)

            val tvCountSelectedTrash = toolbar.findViewById<TextView>(R.id.tvCountSeletedTrash)
            val imgRestore = toolbar.findViewById<ImageView>(R.id.imgRestore)
            val imgSelectAll = toolbar.findViewById<ImageView>(R.id.imgSelectAll)

            noteAdapter.listNoteSelectedAdapter.observe(viewLifecycleOwner) { selectedNotes ->
                tvCountSelectedTrash.text = selectedNotes.size.toString()
            }

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                startEditMode(false)
                noteAdapter.exitEditMode()
                tvCountSelectedTrash?.visibility = View.GONE
                imgRestore?.visibility = View.GONE
                imgSelectAll?.visibility = View.GONE
                toolbar.title = "Trash"
                toolbar.setNavigationIcon(R.drawable.nav)
                toolbar.setNavigationOnClickListener {
                    (activity as MainActivity).setupDefaultToolbar()
                }
            }

            imgRestore.setOnClickListener {
                notesToExport = noteAdapter.listNoteSelectedAdapter.value ?: emptyList()
                restoreDialog(isEmptyTrash = false, isAllNote = false)
            }

            imgSelectAll.setOnClickListener {
                if (noteAdapter.listNoteSelectedAdapter.value.isNullOrEmpty()) {
                    noteAdapter.selectAllNotes()
                } else {
                    noteAdapter.updateListNoteSelected(emptyList())
                }
            }
        } else {
            val trashLayout = toolbar.findViewById<View>(R.id.custom_trash_toolbar)
            if (trashLayout != null) {
                toolbar.removeView(trashLayout)
            }
        }
    }

    private fun dialogDeleteOrUndelete(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.undelete_dialog, null)

        val noteTitle = dialogView.findViewById<TextView>(R.id.noteTitle)
        val noteContent = dialogView.findViewById<TextView>(R.id.noteContent)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radiogroup)
        val btnOk = dialogView.findViewById<TextView>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        btnCancel.setOnClickListener {
            dialog.dismiss()
            noteAdapter.exitEditMode()
            startEditMode(false)
        }
        var isDelete = false

        noteTitle.text = note.title ?: ""

        if (note.note != null) {
            noteContent.text =
                FormatTextSupport().noteContentToSpannable(
                    requireContext(),
                    Gson().fromJson(note.note, NoteContent::class.java)
                )
        } else {
            "".also { noteContent.text = it }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            isDelete = checkedId == R.id.rdbDelete
        }

        btnOk.setOnClickListener {
            if (isDelete) {
                dialogDelete(note)
            } else {
                noteViewModel.pushInTrash(false, note.noteId)
                noteAdapter.exitEditMode()
                startEditMode(false)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.restore_note), Toast.LENGTH_SHORT
                ).show()
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
        noteViewModel.getNoteById(note.noteId).observe(viewLifecycleOwner) { n ->
            n?.let {
                deleteLog.text = buildString {
                    getString(
                        R.string.delete_log,
                        n.title ?: n.note?.take(20) ?: getString(R.string.untitled)
                    )
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            noteViewModel.delete(note.noteId)

            Toast.makeText(requireContext(), getString(R.string.deleted_notes), Toast.LENGTH_SHORT)
                .show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dialogDeleteSelected() {
        val dialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            noteAdapter.listNoteSelectedAdapter.value?.forEach {
                noteViewModel.delete(it.noteId)
            }
            Toast.makeText(requireContext(), getString(R.string.deleted_notes), Toast.LENGTH_SHORT)
                .show()
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
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_more_trash -> {
                showMoreDialog(editMode)
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
                // trash more
                R.id.undelete_all -> {
                    noteViewModel.allTrashNote.observe(viewLifecycleOwner) { allTrashNote ->
                        if (allTrashNote.isNotEmpty()) {
                            notesToExport = allTrashNote
                        }
                    }
                    if (notesToExport.isNotEmpty()) {
                        restoreDialog(isEmptyTrash = false, isAllNote = true)
                    }
                    true
                }

                R.id.export_notes_to_text_files_all_trash -> {
                    noteViewModel.allTrashNote.removeObservers(viewLifecycleOwner)
                    noteViewModel.allTrashNote.observe(viewLifecycleOwner) { allTrashNote ->
                        if (allTrashNote.isNotEmpty()) {
                            notesToExport = allTrashNote
                            FileProcess().checkAndRequestPermissions {
                                openDirectoryChooser()
                            }
                        }
                    }
                    true
                }

                R.id.empty_trash -> {
                    var allTrashNote = emptyList<Note>()
                    noteViewModel.allTrashNote.observe(viewLifecycleOwner) {
                        allTrashNote = it
                        notesToExport = it
                    }
                    if (allTrashNote.isNotEmpty()) {
                        restoreDialog(isEmptyTrash = true, isAllNote = true)
                    }
                    true
                }

                //trash more editmode
                R.id.export_notes_to_text_files_trash -> {
                    val selectedNotes = noteAdapter.listNoteSelectedAdapter.value ?: emptyList()
                    if (isEditMode && selectedNotes.isNotEmpty()) {
                        notesToExport = selectedNotes
                        FileProcess().checkAndRequestPermissions {
                            openDirectoryChooser()
                        }
                        editMode = false
                        startEditMode(false)
                    }
                    true
                }

                R.id.delete_trash -> {
                    val selectedNotes = noteAdapter.listNoteSelectedAdapter.value ?: emptyList()
                    if (isEditMode && selectedNotes.isNotEmpty()) {
                        notesToExport = selectedNotes
                        dialogDeleteSelected()
                    }
                    true
                }

                else -> false
            }
        }

        // Hiển thị PopupMenu
        popupMenu.show()
    }

    private fun openDirectoryChooser() {
        selectDirectoryLauncher.launch(null)
    }

    private val selectDirectoryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                selectedDirectoryUri = it
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)

                FileProcess().exportNotesToDirectory(
                    selectedDirectoryUri,
                    requireContext(),
                    notesToExport
                )
            }
            noteAdapter.exitEditMode()
        }

    private fun restoreDialog(isEmptyTrash: Boolean, isAllNote: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.restore_dialog, null)
        val tvRestore = dialogView.findViewById<TextView>(R.id.tvRestore)
        val tvYes = dialogView.findViewById<TextView>(R.id.tvYes)
        val tvNo = dialogView.findViewById<TextView>(R.id.tvNo)

        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()

        tvRestore.text =
            if (!isEmptyTrash) {
                if (isAllNote) {
                    getString(R.string.restore_all_notes)
                } else {
                    getString(R.string.restore_the_selected_notes)
                }
            } else {
                if (isAllNote) {
                    getString(R.string.delete_trash_log)
                } else {
                    getString(R.string.delete_selected_trash_log)
                }
            }

        tvNo.setOnClickListener {
            dialog.dismiss()
        }

        tvYes.setOnClickListener {
            if (!isEmptyTrash) {
                if (isAllNote) {
                    noteViewModel.restoreAllNote()
                } else {
                    notesToExport.forEach {
                        noteViewModel.pushInTrash(false, it.noteId)
                    }
                }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.restored_notes), Toast.LENGTH_SHORT
                ).show()

            } else {
                if (isAllNote) {
                    noteViewModel.emptyTrash()
                } else {
                    notesToExport.forEach {
                        noteViewModel.delete(it.noteId)
                    }
                }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deleted_notes),
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()

            noteAdapter.exitEditMode()
            startEditMode(false)
        }
        dialog.show()
    }
}