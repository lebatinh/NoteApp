package com.grownapp.noteapp.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteDetailBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteCategoryViewModel: NoteCategoryViewModel

    private var noteId: Int? = null
    private var category: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewModel
        noteViewModel =
            ViewModelProvider(this)[NoteViewModel::class.java]

        noteCategoryViewModel = ViewModelProvider(this)[NoteCategoryViewModel::class.java]
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
                id = it,
                title = binding.edtTitle.text.toString(),
                note = binding.edtNote.text.toString(),
                time = time
            )
        }

        if (updateNote != null) {
            if (category.isNullOrEmpty()){
                noteViewModel.upsert(updateNote)
            }else{
                noteId?.let { noteCategoryViewModel.upsertNoteCategories(it, category!!) }
            }
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
                    showCategorizeDialog()
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

    private fun showCategorizeDialog() {
        noteId?.let { id ->
            noteCategoryViewModel.getAllCategoryOfNote(id).observe(viewLifecycleOwner) { categories ->
                if (categories.isEmpty()) {
                    Toast.makeText(requireContext(), "Không có danh mục nào.", Toast.LENGTH_SHORT).show()
                } else {
                    val selectedCategories = mutableSetOf<String>()
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Chọn danh mục")

                    val categoryNames = categories.map { it.name }.toTypedArray()
                    val checkedItems = BooleanArray(categoryNames.size) { false }

                    builder.setMultiChoiceItems(categoryNames, checkedItems) { _, which, isChecked ->
                        if (isChecked) selectedCategories.add(categoryNames[which])
                        else selectedCategories.remove(categoryNames[which])
                    }

                    builder.setPositiveButton("Lưu") { _, _ ->
                        noteId?.let { id ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                noteCategoryViewModel.removeNoteCategory(id)
                                selectedCategories.forEach { category ->
                                    noteCategoryViewModel.upsertNoteCategories(id, category)
                                }
                            }
                            Toast.makeText(requireContext(), "Danh mục đã cập nhật", Toast.LENGTH_SHORT).show()
                        }
                    }

                    builder.setNegativeButton("Hủy") { dialog, _ ->
                        dialog.dismiss()
                    }

                    builder.create().show()
                }
            }
        }
    }

}