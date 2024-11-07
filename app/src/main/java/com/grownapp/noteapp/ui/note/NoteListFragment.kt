package com.grownapp.noteapp.ui.note

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteListBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import kotlin.math.atan2

class NoteListFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteListBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var categoryId: Int? = null
    private var hideCreated: Boolean = false
    private lateinit var listNoteSelected: MutableList<Note>
    private var isEditMode = false
    private var isOnTrash = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        categoryViewModel =
            ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        listNoteSelected = mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val isVisible = sharedPreferences.getBoolean("isVisible", true)
        isOnTrash = sharedPreferences.getBoolean("isOnTrash", true)
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
            startEditMode(!isEditMode)
        },
            listNoteSelectedAdapter = listNoteSelected,
            updateCountCallback = { updateCountNoteSeleted() },
            getCategoryOfNote = { noteId -> noteViewModel.getCategoryOfNote(noteId) }
        )

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

    private fun updateCountNoteSeleted() {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val tvCountSeleted = toolbar?.findViewById<TextView>(R.id.tvCountSeleted)
        tvCountSeleted?.text = listNoteSelected.size.toString()
    }

    private fun startEditMode(isVisible: Boolean) {
        isEditMode = isVisible
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val btnSearch = toolbar.findViewById<View>(R.id.item_search)
        val item_sort = toolbar.findViewById<View>(R.id.item_sort)
        val item_more = toolbar.findViewById<View>(R.id.item_more)

        if (isVisible) {
            btnSearch?.visibility = View.GONE
            item_sort?.visibility = View.GONE
            item_more?.visibility = View.GONE

            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )

            val noteLayout = layoutInflater.inflate(R.layout.custom_note_toolbar, toolbar, false)
            toolbar.addView(noteLayout, layoutParams)

            val tvCountSeleted = toolbar.findViewById<TextView>(R.id.tvCountSeleted)
            val imgDelete = toolbar.findViewById<ImageView>(R.id.imgDelete)
            val imgSelectAll = toolbar.findViewById<ImageView>(R.id.imgSelectAll)

            Log.d("listNoteSelectedFragment", listNoteSelected.size.toString())
            tvCountSeleted.text = listNoteSelected.size.toString()

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                startEditMode(false)
                noteAdapter.exitEditMode()
                binding.fab.visibility = View.VISIBLE
                tvCountSeleted?.visibility = View.GONE
                imgDelete?.visibility = View.GONE
                imgSelectAll?.visibility = View.GONE
                toolbar.title = "Notepad Free"
                toolbar.setNavigationIcon(R.drawable.nav)
                toolbar.setNavigationOnClickListener {
                    (activity as MainActivity).setupDefaultToolbar()
                }
            }
            imgSelectAll.setOnClickListener {
                if (listNoteSelected.isEmpty()) {
                    noteViewModel.allNote.observe(viewLifecycleOwner) {
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

            imgDelete.setOnClickListener {
                showDialogDelete()
            }
        } else {
            btnSearch?.visibility = View.VISIBLE
            item_sort?.visibility = View.VISIBLE
            item_more?.visibility = View.VISIBLE

            val noteLayout = toolbar.findViewById<View>(R.id.custom_note_toolbar)
            if (noteLayout != null) {
                toolbar.removeView(noteLayout)
            }
        }
    }

    private fun showDialogDelete() {
        val dialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
        val deleteLog = dialogView.findViewById<TextView>(R.id.delete_log)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()

        deleteLog.text = buildString {
            append("Delete the selected notes?")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        btnDelete.setText("OK")

        btnDelete.setOnClickListener {
            listNoteSelected.forEach {
                if (isOnTrash) {
                    noteViewModel.pushInTrash(true, it.noteId)
                } else {
                    noteViewModel.delete(it.noteId)
                }
            }
            startEditMode(false)
            dialog.dismiss()
        }
        dialog.show()
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
        menu.clear()
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

            R.id.item_more_note -> {
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
        val buttonCancel = dialogView.findViewById<TextView>(R.id.buttonCancel)
        val buttonSort = dialogView.findViewById<TextView>(R.id.buttonSort)

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
        val anchorView = requireActivity().findViewById<View>(R.id.item_more_note)

        val popupMenu = PopupMenu(requireContext(), anchorView)

        if (isEditMode) {
            popupMenu.menuInflater.inflate(R.menu.menu_note_longclick, popupMenu.menu)
        } else {
            popupMenu.menuInflater.inflate(R.menu.note_more, popupMenu.menu)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.select_all_notes -> {
                    startEditMode(true)
                    noteAdapter.selectAllNotes()
                    true
                }

                R.id.import_text_files -> {
                    true
                }

                R.id.export_notes_to_text_files -> {
                    true
                }
                R.id.categorize -> {
                    showCategorizeDialog()
                    true
                }

                R.id.colorize -> {
                    true
                }
                else -> false
            }
        }

        // Hiển thị PopupMenu
        popupMenu.show()
    }
    private fun showCategorizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.category_list_dialog, null)
        val categoryListView = dialogView.findViewById<RecyclerView>(R.id.rcvCategory)
        val cancelButon = dialogView.findViewById<TextView>(R.id.btnCancel)
        val okButton = dialogView.findViewById<TextView>(R.id.btnOk)

        val categoryMutableList = mutableListOf<Category>()
        val selectedCategory = mutableSetOf<Int>()

        categoryListView.layoutManager = LinearLayoutManager(requireContext())

        val categoryForNoteAdapter =
            CategoryForNoteAdapter(categoryMutableList.toList(), selectedCategory)
        categoryListView.adapter = categoryForNoteAdapter

        if (listNoteSelected.isNotEmpty()) {
            categoryViewModel.allCategory.observe(this) { categories ->
                categoryMutableList.clear()
                categoryMutableList.addAll(categories)
                categoryForNoteAdapter.updateListCategory(categoryMutableList)
            }
        }
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        cancelButon.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            if (listNoteSelected.isNotEmpty()) {
                for (categoryId in selectedCategory) {
                    listNoteSelected.forEach {
                        val noteCategoryCrossRef =
                            NoteCategoryCrossRef(noteId = it.noteId, categoryId = categoryId)
                        noteViewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
                    }
                }
            }
            Toast.makeText(requireContext(), "Update categories", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            noteAdapter.exitEditMode()
            startEditMode(false)
        }
        dialog.show()
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

            "color" -> if (categoryId != null) {
                noteViewModel.sortedByColorWithCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByColorWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }
        }
    }
}