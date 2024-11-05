package com.grownapp.noteapp.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentTrashBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note

class TrashFragment : Fragment(), MenuProvider {

    private var _binding: FragmentTrashBinding? = null

    private val binding get() = _binding!!
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel

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

            },
            hideCreated = true
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

    private fun dialogDeleteOrUndelete(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.undelete_dialog, null)

        val noteTitle = dialogView.findViewById<TextView>(R.id.noteTitle)
        val noteContent = dialogView.findViewById<TextView>(R.id.noteContent)
        val radiogroup = dialogView.findViewById<RadioGroup>(R.id.radiogroup)
        val rdbUndelete = dialogView.findViewById<RadioButton>(R.id.rdbUndelete)
        val rdbDelete = dialogView.findViewById<RadioButton>(R.id.rdbDelete)
        val btnOk = dialogView.findViewById<TextView>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        var isDelete = false

        noteTitle.text = note.title ?: ""
        noteContent.text = note.note ?: ""
        radiogroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rdbUndelete -> {
                    rdbUndelete.isChecked = !isDelete
                    isDelete = !rdbUndelete.isChecked
                }

                R.id.rdbDelete -> {
                    rdbDelete.isChecked = isDelete
                    isDelete = rdbDelete.isChecked
                }
            }
            dialog.dismiss()
        }

        btnOk.setOnClickListener {
            if (isDelete) {
                noteViewModel.delete(note.noteId)
                Toast.makeText(requireContext(), "Restore note", Toast.LENGTH_SHORT).show()
            } else {
                noteViewModel.pushInTrash(false, note.noteId)
                dialogDelete(note)
            }
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
            deleteLog.text = "The note will be deleted permanently!\n" +
                    "Are you sure that you want to delete the '${
                        note.title ?: note.note?.substring(
                            0,
                            20
                        ) ?: "Untitled"
                    }' note?"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.trash_more, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_more -> {}
        }
        return false
    }


}