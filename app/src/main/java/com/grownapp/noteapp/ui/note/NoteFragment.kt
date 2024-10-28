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
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteBinding
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2

class NoteFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel =
            ViewModelProvider(this)[NoteViewModel::class.java]

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        noteViewModel.insertFirstNote(sharedPreferences)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val isVisible = sharedPreferences.getBoolean("isVisible", true)
        if (isVisible) {
            binding.ctlInstruct.visibility = View.VISIBLE
        } else {
            binding.ctlInstruct.visibility = View.GONE
        }

        noteAdapter = NoteAdapter(
            onClickNote = {
                val action = NoteFragmentDirections.actionNavNoteToNoteDetailFragment(it.noteId)
                findNavController().navigate(action)
            },
            onDelete = {
                noteViewModel.delete(it)
            }
        )

        binding.rcvNote.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNote.adapter = noteAdapter

        noteViewModel.allNote.observe(viewLifecycleOwner) { notes ->
            notes.let {
//                val noteItems = notes.map { note ->
//                    NoteWithCategories(note, emptyList())
//                }
                noteAdapter.updateListNote(notes)
            }
        }

        // Lấy danh sách các category cho từng note
//        noteViewModel.allNote.observe(viewLifecycleOwner) { notes ->
//            notes.forEach { note ->
//                noteViewModel.getAllCategoryOfNote(note.id)
//                    .observe(viewLifecycleOwner) { categories ->
//                        val noteItems = notes.map { noteItem ->
//                            NoteItem.NoteWithCategories(noteItem, categories)
//                        }
//                        noteAdapter.updateListNote(noteItems)
//                    }
//            }
//        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.fab.setOnClickListener {
            createNewNote()
            sharedPreferences.edit().putBoolean("isVisible", false).apply()
        }

        // Lấy kích thước màn hình
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

        // Xoay mũi tên về góc dưới bên phải
        rotateArrowToCorner(binding.arrowImageView, screenWidth, screenHeight)

        return root
    }

    private fun getCategoriesForNote(id: Int): List<Category> {
        var listCategory = emptyList<Category>()
//        noteViewModel.getAllCategoryOfNote(id).observe(viewLifecycleOwner){
//            listCategory = it
//        }
        return listCategory
    }

    private fun rotateArrowToCorner(arrowImageView: ImageView, cornerX: Float, cornerY: Float) {
        val location = IntArray(2)
        arrowImageView.getLocationOnScreen(location)
        val arrowX = location[0].toFloat()
        val arrowY = location[1].toFloat()

        val deltaX = cornerX - arrowX
        val deltaY = cornerY - arrowY
        val angle = Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble()))

        arrowImageView.rotation = angle.toFloat()
    }

    private fun createNewNote() {
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        val time = dateFormat.format(currentDateTime)

        viewLifecycleOwner.lifecycleScope.launch {
            noteViewModel.insert(Note(time = time)) {}

            noteViewModel.noteId.observe(viewLifecycleOwner) { id ->
                id?.let {
                    val action = NoteFragmentDirections.actionNavNoteToNoteDetailFragment(it)
                    findNavController().navigate(action)
                    noteViewModel.clearNoteId()
                    noteViewModel.noteId.removeObservers(viewLifecycleOwner)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                            noteViewModel.search("%$it%").observe(viewLifecycleOwner) { notes ->
//                                val noteItems = notes.map { note ->
//                                    NoteWithCategories(note, emptyList())
//                                }
                                noteAdapter.updateListNote(notes)
                            }
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let {
                            noteViewModel.search(it).observe(viewLifecycleOwner) { notes ->
//                                val noteItems = notes.map { note ->
//                                    NoteWithCategories(note, emptyList())
//                                }
                                noteAdapter.updateListNote(notes)
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
                    R.id.redo -> {}
                    R.id.undo_all -> {}
                    R.id.share -> {}
                    R.id.delete -> {}
                    R.id.search -> {}
                    R.id.export_to_a_text_files -> {}
                    R.id.categorize -> {}
                    R.id.colorize -> {}
                    R.id.convert_to_checklist -> {}
                    R.id.switch_to_read_mode -> {}
                    R.id.print -> {}
                    R.id.show_formatting_bar -> {}
                    R.id.show_info -> {}
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