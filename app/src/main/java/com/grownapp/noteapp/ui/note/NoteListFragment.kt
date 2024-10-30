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
import android.widget.RadioGroup
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
import kotlin.math.atan2

class NoteListFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteListBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var categoryId: Int? = null
    private var hideCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val isVisible = sharedPreferences.getBoolean("isVisible", true)
        if (isVisible) {
            binding.ctlInstruct.visibility = View.VISIBLE
        } else {
            binding.ctlInstruct.visibility = View.GONE
        }

        // Lấy kích thước màn hình
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

        // Xoay mũi tên về góc dưới bên phải
        rotateArrowToCorner(binding.arrowImageView, screenWidth, screenHeight)
        sortBy()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteAdapter = NoteAdapter(onClickNote = {
            val action =
                NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(it.noteId)
            findNavController().navigate(action)
        }, onLongClickNote = {
            noteViewModel.delete(it)
        })

        binding.rcvNoteList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteList.adapter = noteAdapter

        val categoryName = arguments?.getString("name")
        categoryId = arguments?.getString("id")?.toInt()
        (activity as AppCompatActivity).supportActionBar?.subtitle = categoryName ?: "Uncategorized"

        if (categoryId != null) {
            noteViewModel.getNotesByCategory(categoryId!!).observe(viewLifecycleOwner) { notes ->
                val note = notes.map { it.note }
                noteAdapter.updateListNote(note)
            }
        } else {
            noteViewModel.allNoteWithoutCategory.observe(viewLifecycleOwner) { notes ->
                noteAdapter.updateListNote(notes)
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
        noteViewModel.insert(Note()) { noteId ->
            if (categoryId != null) {
                val noteCategoryCrossRef =
                    NoteCategoryCrossRef(noteId = noteId.toInt(), categoryId = categoryId)
                noteViewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
            }
        }
        noteViewModel.noteId.observe(viewLifecycleOwner) { id ->
            id?.let {
                val action =
                    NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(it)
                findNavController().navigate(action)
                noteViewModel.clearNoteId()
                noteViewModel.noteId.removeObservers(viewLifecycleOwner)
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
                        query?.let { q ->
                            if (categoryId != null) {
                                noteViewModel.searchNoteWithCategory("%$q%", categoryId!!)
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        noteAdapter.updateListNote(note)
                                    }
                            } else {
                                noteViewModel.searchNoteWithoutCategory("%$q%")
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        noteAdapter.updateListNote(note)
                                    }
                            }

                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let { q ->
                            if (categoryId != null) {
                                noteViewModel.searchNoteWithCategory("%$q%", categoryId!!)
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        noteAdapter.updateListNote(note)
                                    }
                            } else {
                                noteViewModel.searchNoteWithoutCategory("%$q%")
                                    .observe(viewLifecycleOwner) { notes ->
                                        val note = notes.map { it.note }
                                        noteAdapter.updateListNote(note)
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

        // Đặt RadioButton checked dựa trên giá trị "sort" trong SharedPreferences
        val sort = sharedPreferences.getString("sort", null)
        when (sort) {
            "editnewest" -> rdgSort.check(R.id.rdbEditNewest)
            "editoldest" -> rdgSort.check(R.id.rdbEditOldest)
            "a_z" -> rdgSort.check(R.id.rdbA_Z)
            "z_a" -> rdgSort.check(R.id.rdbZ_A)
            "createnewest" -> rdgSort.check(R.id.rdbCreateNewest)
            "createoldest" -> rdgSort.check(R.id.rdbCreateOldest)
            "color" -> rdgSort.check(R.id.rdbColor)
        }

        buttonSort.setOnClickListener {
            val selectedRadioButtonId = rdgSort.checkedRadioButtonId
            val editor = sharedPreferences.edit()

            // Lưu giá trị "sort" mới vào SharedPreferences và xử lý trạng thái "hideCreated"
            when (selectedRadioButtonId) {
                R.id.rdbEditNewest -> {
                    editor.putString("sort", "editnewest")
                    hideCreated = false
                }

                R.id.rdbEditOldest -> {
                    editor.putString("sort", "editoldest")
                    hideCreated = false
                }

                R.id.rdbA_Z -> {
                    editor.putString("sort", "a_z")
                    hideCreated = false
                }

                R.id.rdbZ_A -> {
                    editor.putString("sort", "z_a")
                    hideCreated = false
                }

                R.id.rdbCreateNewest -> {
                    editor.putString("sort", "createnewest")
                    hideCreated = true
                }

                R.id.rdbCreateOldest -> {
                    editor.putString("sort", "createoldest")
                    hideCreated = true
                }

                R.id.rdbColor -> {
                    editor.putString("sort", "color")
                    hideCreated = false
                }
            }
            editor.apply() // Lưu thay đổi

            // Gọi hàm sắp xếp
            sortBy()
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

    private fun sortBy() {
        val sort = sharedPreferences.getString("sort", null)
        val sortObserver = { notes: List<Note> -> noteAdapter.updateListNote(notes) }

        when (sort) {
            "editnewest" -> if (categoryId != null) {
                noteViewModel.sortedByUpdatedTimeDescByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByUpdatedTimeDescWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            "editoldest" -> if (categoryId != null) {
                noteViewModel.sortedByUpdatedTimeAscByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByUpdatedTimeAscWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            "a_z" -> if (categoryId != null) {
                noteViewModel.sortedByTitleAscByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByTitleAscWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            "z_a" -> if (categoryId != null) {
                noteViewModel.sortedByTitleDescByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByTitleDescWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            "createnewest" -> if (categoryId != null) {
                noteViewModel.sortedByCreatedTimeDescByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByCreatedTimeDescWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            "createoldest" -> if (categoryId != null) {
                noteViewModel.sortedByCreatedTimeAscByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByCreatedTimeAscWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            "color" -> {
                // Xử lý tùy chọn "color" nếu cần
            }
        }
    }
}