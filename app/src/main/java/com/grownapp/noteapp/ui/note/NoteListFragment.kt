package com.grownapp.noteapp.ui.note

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteListBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteListFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteListBinding? = null

    private val binding get() = _binding!!

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var categoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NoteAdapter(onClickNote = {
            val action =
                NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(it.noteId)
            findNavController().navigate(action)
        }, onDelete = {
            viewModel.delete(it)
        })

        binding.rcvNoteList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteList.adapter = adapter

        val categoryName = arguments?.getString("name")
        categoryId = arguments?.getString("id")?.toInt()
        (activity as AppCompatActivity).supportActionBar?.subtitle = categoryName ?: "Uncategorized"

        Toast.makeText(requireContext(), "$categoryId - $categoryName", Toast.LENGTH_SHORT).show()
        if (categoryId != null) {
            viewModel.getNotesByCategory(categoryId!!).observe(viewLifecycleOwner) { notes ->
                val note = notes.map { it.note }
                adapter.updateListNote(note)
            }
        } else {
            viewModel.allNoteWithoutCategory.observe(viewLifecycleOwner) { notes ->
                adapter.updateListNote(notes)
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.fab.setOnClickListener {
            addNote(categoryId)
            sharedPreferences.edit().putBoolean("isVisible", false).apply()
        }
    }

    private fun addNote(categoryId: Int?) {
        viewModel.insert(Note()) { noteId ->
            if (categoryId != null) {
                val noteCategoryCrossRef =
                    NoteCategoryCrossRef(noteId = noteId.toInt(), categoryId = categoryId)
                viewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
            }
        }
        viewModel.noteId.observe(viewLifecycleOwner) { id ->
            id?.let {
                val action =
                    NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(it)
                findNavController().navigate(action)
                viewModel.clearNoteId()
                viewModel.noteId.removeObservers(viewLifecycleOwner)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_note, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_search -> {
                val searchView = menuItem.actionView as SearchView
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        query?.let {
                            if (categoryId != null) {
                                viewModel.searchNoteWithCategory("%$it%", categoryId!!)
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        adapter.updateListNote(note)
                                    }
                            } else {
                                viewModel.searchNoteWithoutCategory("%$it%")
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        adapter.updateListNote(note)
                                    }
                            }

                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let {
                            if (categoryId != null) {
                                viewModel.searchNoteWithCategory("%$it%", categoryId!!)
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        adapter.updateListNote(note)
                                    }
                            } else {
                                viewModel.searchNoteWithoutCategory("%$it%")
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        adapter.updateListNote(note)
                                    }
                            }
                        }
                        return true
                    }

                })
                return true
            }

            R.id.item_sort -> {
                showSortDialog()
                return true
            }

            R.id.item_more -> {
                showPopupMenuMore()
                return true
            }
        }
        return false
    }

    private fun showSortDialog() {
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.menu_sort, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val rdgSort = dialogView.findViewById<RadioGroup>(R.id.rdgSort)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val buttonSort = dialogView.findViewById<Button>(R.id.buttonSort)

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonSort.setOnClickListener {
            val selectedRadioButtonId = rdgSort.checkedRadioButtonId
            val selectedRadioButton =
                dialogView.findViewById<RadioButton>(selectedRadioButtonId)

            if (selectedRadioButton != null) {
                val selectedId = selectedRadioButton.id

                when (selectedId) {

                }
            }

            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showPopupMenuMore() {
        val anchorView = requireActivity().findViewById<View>(R.id.item_more)

        val popupMenu = PopupMenu(requireContext(), anchorView)

        popupMenu.menuInflater.inflate(R.menu.note_more, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.select_all_notes -> {
                    true
                }

                R.id.import_text_files -> {
                    true
                }

                R.id.export_notes_to_text_files -> {
                    true
                }

                else -> false
            }
        }

        // Hiển thị PopupMenu
        popupMenu.show()
    }
}