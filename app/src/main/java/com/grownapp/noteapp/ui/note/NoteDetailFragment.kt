package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteDetailBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoriesViewModel

    private var noteId: Int? = null
    private var category: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewModel
        noteViewModel =
            ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel =
            ViewModelProvider(this)[CategoriesViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        arguments?.let {
            noteId = NoteDetailFragmentArgs.fromBundle(it).id
            category = arguments?.getString("categoryName")
        }
        noteId?.let { notes ->
            noteViewModel.getNoteById(notes).observe(viewLifecycleOwner) { note ->
                note?.let {
                    binding.edtTitle.setText(it.title)
                    binding.edtNote.setText(it.note)
                }
            }
        }
        return root
    }

    override fun onPause() {
        super.onPause()
        saveNote()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_note_detail, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_save -> {
                saveNote()
                return true
            }

            R.id.item_undo -> {}
            R.id.item_more -> {
                showPopupMenuMore()
                return true
            }
        }
        return false
    }

    private fun saveNote() {
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        val time = dateFormat.format(currentDateTime)

        val updateNote = noteId?.let {
            Note(
                noteId = it,
                title = binding.edtTitle.text.toString(),
                note = binding.edtNote.text.toString(),
                time = time
            )
        }

        if (updateNote != null) {
            noteViewModel.insert(updateNote) {}
        }
        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun showPopupMenuMore() {
        val anchorView = requireActivity().findViewById<View>(R.id.item_more)

        val popupMenu = PopupMenu(requireContext(), anchorView)

        popupMenu.menuInflater.inflate(R.menu.menu_more_note_detail, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.redo -> {
                    true
                }

                R.id.undo_all -> {
                    true
                }

                R.id.share -> {
                    true
                }

                R.id.delete -> {
                    true
                }

                R.id.search -> {
                    true
                }

                R.id.export_to_a_text_files -> {
                    true
                }

                R.id.categorize -> {
                    showCategorizeDialog(noteId)
                    true
                }

                R.id.colorize -> {
                    true
                }

                R.id.convert_to_checklist -> {
                    true
                }

                R.id.switch_to_read_mode -> {
                    true
                }

                R.id.print -> {
                    true
                }

                R.id.show_formatting_bar -> {
                    true
                }

                R.id.show_info -> {
                    true
                }

                else -> false
            }
        }

        // Hiển thị PopupMenu
        popupMenu.show()
    }

    private fun showCategorizeDialog(noteId: Int?) {
        val dialogView = layoutInflater.inflate(R.layout.category_list_dialog, null)
        val categoryListView = dialogView.findViewById<RecyclerView>(R.id.rcvCategory)
        val cancelButon = dialogView.findViewById<TextView>(R.id.btnCancel)
        val okButton = dialogView.findViewById<TextView>(R.id.btnOk)

        val categoryMutableList = mutableListOf<Category>()
        val selectedCategory = mutableSetOf<Int>()

        categoryListView.layoutManager = LinearLayoutManager(requireContext())

        val categoryForNoteAdapter = CategoryForNoteAdapter(categoryMutableList.toList(), selectedCategory)
        categoryListView.adapter = categoryForNoteAdapter

        if (noteId != null) {
            noteViewModel.getCategoryOfNote(noteId).observe(viewLifecycleOwner) { c ->
                selectedCategory.addAll(c.map { it.categoryId })
                // Lắng nghe toàn bộ danh mục từ ViewModel và cập nhật danh sách
                categoryViewModel.allCategory.observe(this) { categories ->
                    categoryMutableList.clear()
                    categoryMutableList.addAll(categories)
                    categoryForNoteAdapter.updateListCategory(categoryMutableList)
                }
            }
        }
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        cancelButon.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            Log.e("selectedCategory", selectedCategory.toString())
            if (noteId != null) {
                noteViewModel.deleteCategoriesForNote(noteId)
                for (categoryId in selectedCategory) {
                    val noteCategoryCrossRef =
                        NoteCategoryCrossRef(noteId = noteId, categoryId = categoryId)
                    noteViewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
                }
            }
            dialog.dismiss()
        }
        dialog.show()
    }
}