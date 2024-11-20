package com.grownapp.noteapp.ui.note

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteListBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.support.FileProcess
import com.grownapp.noteapp.ui.note.support.FormatTextSupport
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import kotlinx.coroutines.launch
import kotlin.math.atan2

class NoteListFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteListBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var categoryId: Int? = null

    private var hideCreated = MutableLiveData(true)

    private var isOnTrash = true
    private var isEditMode = false
    private lateinit var listCategory: MutableList<Category>
    private var selectedDirectoryUri: Uri? = null
    private var lastEditOrCreate = true
    private var notesToExport: List<Note> = emptyList()
    private var allNote = emptyList<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        categoryViewModel =
            ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        listCategory = mutableListOf()
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

        val categoryName = arguments?.getString("name")
        categoryId = arguments?.getString("id")?.toInt()
        (activity as AppCompatActivity).supportActionBar?.subtitle = categoryName ?: "Uncategorized"

        hideCreated.observe(viewLifecycleOwner) {
            lastEditOrCreate = it
        }
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

        rotateArrowToCorner(binding.arrowImageView, screenWidth, screenHeight)
        sortBy()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteAdapter = NoteAdapter(onClickNote = {
            if (!isEditMode) {
                val action = NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(it.noteId)
                findNavController().navigate(action)
            }
        }, onLongClickNote = {
            if (!isEditMode) {
                isEditMode = true
                startEditMode(true)
                binding.fab.visibility = View.GONE
            }
        },
            hideCreated = lastEditOrCreate,
            listNoteSelectedAdapter = MutableLiveData(mutableListOf()),
            getCategoryOfNote = { noteId -> noteViewModel.getCategoryOfNote(noteId) },
            lifecycleOwner = viewLifecycleOwner
        )
        noteAdapter.listNoteSelectedAdapter.observe(viewLifecycleOwner) { selectedNotes ->
            updateCountNoteSelected(selectedNotes.size)
        }
        binding.rcvNoteList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteList.adapter = noteAdapter

        sortBy()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.fab.setOnClickListener {
            addNote(categoryId)
            sharedPreferences.edit().putBoolean("isVisible", false).apply()
        }

        categoryViewModel.allCategory.observe(viewLifecycleOwner) {
            listCategory.clear()
            listCategory.addAll(it)
        }
        noteViewModel.setCategoryId(categoryId)
        noteViewModel.searchResultCategory.observe(viewLifecycleOwner) { notes ->
            val note = notes.map { it.note }
            noteAdapter.updateListNote(note)
        }
    }
    override fun onPause() {
        super.onPause()
        startEditMode(false)
    }

    private fun updateCountNoteSelected(cout: Int) {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val tvCountSelected = toolbar?.findViewById<TextView>(R.id.tvCountSeleted)
        tvCountSelected?.text = cout.toString()
    }

    private fun startEditMode(isVisible: Boolean) {
        isEditMode = isVisible
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val btnSearch = toolbar.findViewById<View>(R.id.item_search)
        val itemSort = toolbar.findViewById<View>(R.id.item_sort)
        val itemMore = toolbar.findViewById<View>(R.id.item_more)

        if (isVisible) {
            btnSearch?.visibility = View.GONE
            itemSort?.visibility = View.GONE
            itemMore?.visibility = View.GONE

            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )

            val noteLayout = layoutInflater.inflate(R.layout.custom_note_toolbar, toolbar, false)
            toolbar.addView(noteLayout, layoutParams)

            val tvCountSelected = toolbar.findViewById<TextView>(R.id.tvCountSeleted)
            val imgDelete = toolbar.findViewById<ImageView>(R.id.imgDelete)
            val imgSelectAll = toolbar.findViewById<ImageView>(R.id.imgSelectAll)

            noteAdapter.listNoteSelectedAdapter.observe(viewLifecycleOwner) { selectedNotes ->
                tvCountSelected.text = selectedNotes.size.toString()
            }

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                startEditMode(false)
                noteAdapter.exitEditMode()
                binding.fab.visibility = View.VISIBLE
                tvCountSelected?.visibility = View.GONE
                imgDelete?.visibility = View.GONE
                imgSelectAll?.visibility = View.GONE
                toolbar.title = "Notepad Free"
                toolbar.setNavigationIcon(R.drawable.nav)
                toolbar.setNavigationOnClickListener {
                    (activity as MainActivity).setupDefaultToolbar()
                }
            }
            imgSelectAll.setOnClickListener {
                if (noteAdapter.listNoteSelectedAdapter.value.isNullOrEmpty()) {
                    noteAdapter.selectAllNotes()
                } else {
                    noteAdapter.updateListNoteSelected(emptyList())
                }
            }

            imgDelete.setOnClickListener {
                showDialogDelete()
            }
        } else {
            btnSearch?.visibility = View.VISIBLE
            itemSort?.visibility = View.VISIBLE
            itemMore?.visibility = View.VISIBLE

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

        deleteLog.text = getString(R.string.delete_the_selected_notes)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        btnDelete.text = getString(R.string.ok)

        btnDelete.setOnClickListener {
            noteAdapter.listNoteSelectedAdapter.value?.forEach {
                if (isOnTrash) {
                    noteViewModel.pushInTrash(true, it.noteId)
                } else {
                    noteViewModel.delete(it.noteId)
                }
            }
            startEditMode(false)
            noteAdapter.listNoteSelectedAdapter.value?.clear()
            noteAdapter.exitEditMode()
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
                        query?.let { noteViewModel.setSearchQuery("%${it}%") }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let { noteViewModel.setSearchQuery("%${it}%") }
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

        val sort = sharedPreferences.getString("sort", null)
        when (sort) {
            getString(R.string.editnewest) -> rdgSort.check(R.id.rdbEditNewest)
            getString(R.string.editoldest) -> rdgSort.check(R.id.rdbEditOldest)
            getString(R.string.a_z) -> rdgSort.check(R.id.rdbA_Z)
            getString(R.string.z_a) -> rdgSort.check(R.id.rdbZ_A)
            getString(R.string.createnewest) -> rdgSort.check(R.id.rdbCreateNewest)
            getString(R.string.createoldest) -> rdgSort.check(R.id.rdbCreateOldest)
            getString(R.string.color) -> rdgSort.check(R.id.rdbColor)
        }

        buttonSort.setOnClickListener {
            val selectedRadioButtonId = rdgSort.checkedRadioButtonId
            val editor = sharedPreferences.edit()

            when (selectedRadioButtonId) {
                R.id.rdbEditNewest -> {
                    editor.putString("sort", getString(R.string.editnewest))
                    noteAdapter.setHideCreated(true)
                    hideCreated.postValue(true)
                }

                R.id.rdbEditOldest -> {
                    editor.putString("sort", getString(R.string.editoldest))
                    noteAdapter.setHideCreated(true)
                    hideCreated.postValue(true)
                }

                R.id.rdbA_Z -> {
                    editor.putString("sort", getString(R.string.a_z))
                    noteAdapter.setHideCreated(true)
                    hideCreated.postValue(true)
                }

                R.id.rdbZ_A -> {
                    editor.putString("sort", getString(R.string.z_a))
                    noteAdapter.setHideCreated(true)
                    hideCreated.postValue(true)
                }

                R.id.rdbCreateNewest -> {
                    editor.putString("sort", getString(R.string.createnewest))
                    noteAdapter.setHideCreated(false)
                    hideCreated.postValue(false)
                }

                R.id.rdbCreateOldest -> {
                    editor.putString("sort", getString(R.string.createoldest))
                    noteAdapter.setHideCreated(false)
                    hideCreated.postValue(false)
                }

                R.id.rdbColor -> {
                    editor.putString("sort", getString(R.string.color))
                    noteAdapter.setHideCreated(true)
                    hideCreated.postValue(true)
                }
            }
            hideCreated.value?.let { it1 -> editor.putBoolean("hideCreated", it1) }
            editor.apply()

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
                    if (allNote.isNotEmpty()) {
                        startEditMode(true)
                        binding.fab.visibility = View.VISIBLE
                        noteAdapter.selectAllNotes()
                    }
                    true
                }

                R.id.import_text_files -> {
                    FileProcess().checkAndRequestPermissions {
                        importNotesFromFiles()
                    }
                    noteAdapter.listNoteSelectedAdapter.value?.clear()
                    noteAdapter.exitEditMode()
                    isEditMode = false
                    startEditMode(false)
                    true
                }

                R.id.export_notes_to_text_files -> {
                    val selectedNotes = noteAdapter.listNoteSelectedAdapter.value ?: emptyList()
                    if (isEditMode && selectedNotes.isNotEmpty()) {
                        notesToExport = selectedNotes
                        FileProcess().checkAndRequestPermissions {
                            openDirectoryChooser()
                        }
                        isEditMode = false
                        startEditMode(false)
                    }
                    true
                }

                R.id.export_notes_to_text_files_all_note -> {
                    if (categoryId != null) {
                        noteViewModel.getNotesByCategory(categoryId!!)
                            .removeObservers(viewLifecycleOwner)
                        noteViewModel.getNotesByCategory(categoryId!!)
                            .observe(viewLifecycleOwner) { allNotes ->
                                if (allNotes.isNotEmpty()) {
                                    val note = allNotes.map { it.note }
                                    notesToExport = note
                                    FileProcess().checkAndRequestPermissions {
                                        openDirectoryChooser()
                                    }
                                }
                            }
                    } else {
                        noteViewModel.allNoteWithoutCategory.removeObservers(viewLifecycleOwner)
                        noteViewModel.allNoteWithoutCategory.observe(viewLifecycleOwner) { allNotes ->
                            if (allNotes.isNotEmpty()) {
                                notesToExport = allNotes
                                FileProcess().checkAndRequestPermissions {
                                    openDirectoryChooser()
                                }
                            }
                        }
                    }

                    true
                }

                R.id.categorize -> {
                    if (listCategory.isNotEmpty()) {
                        showCategorizeDialog()
                    }
                    true
                }

                R.id.colorize -> {
                    dialogPickColor()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
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
                noteAdapter.exitEditMode()
            }
        }

    private fun openDirectoryChooser() {
        selectDirectoryLauncher.launch(null)
    }

    private fun importNotesFromFiles() {
        openFileLauncher.launch(arrayOf(getString(R.string.text_plain)))
    }

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                importFile(it)
            }
        }

    private fun importFile(uri: Uri) {
        try {
            context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }.trimIndent()

                val fileName = uri.lastPathSegment?.substringAfterLast('/')
                    ?.substringBeforeLast(".txt")

                val noteContent = SpannableStringBuilder(content)
                val spannableContent =
                    FormatTextSupport().spannableToNoteContent(requireContext(), noteContent)

                val note = Note(
                    title = fileName ?: getString(R.string.untitled),
                    note = Gson().toJson(spannableContent)
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    noteViewModel.insert(note) { noteId ->
                        if (categoryId != null) {
                            val noteCategoryCrossRef =
                                NoteCategoryCrossRef(
                                    noteId = noteId.toInt(),
                                    categoryId = categoryId!!
                                )
                            noteViewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
                        }
                    }
                    noteViewModel.noteId.observe(viewLifecycleOwner) { id ->
                        id?.let {
                            val action =
                                NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(
                                    it
                                )
                            findNavController().navigate(action)
                            noteViewModel.clearNoteId()
                            noteViewModel.noteId.removeObservers(viewLifecycleOwner)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun showCategorizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.category_list_dialog, null)
        val categoryListView = dialogView.findViewById<RecyclerView>(R.id.rcvCategory)
        val cancelButton = dialogView.findViewById<TextView>(R.id.btnCancel)
        val okButton = dialogView.findViewById<TextView>(R.id.btnOk)

        val categoryMutableList = mutableListOf<Category>()
        val selectedCategory = mutableSetOf<Int>()

        categoryListView.layoutManager = LinearLayoutManager(requireContext())

        val categoryForNoteAdapter =
            CategoryForNoteAdapter(categoryMutableList.toList(), selectedCategory)
        categoryListView.adapter = categoryForNoteAdapter

        if (noteAdapter.listNoteSelectedAdapter.value?.isNotEmpty() == true) {
            categoryViewModel.allCategory.observe(this) { categories ->
                categoryMutableList.clear()
                categoryMutableList.addAll(categories)
                categoryForNoteAdapter.updateListCategory(categoryMutableList)
            }
        }
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            if (noteAdapter.listNoteSelectedAdapter.value?.isNotEmpty() == true) {
                for (categoryId in selectedCategory) {
                    noteAdapter.listNoteSelectedAdapter.value?.forEach {
                        val noteCategoryCrossRef =
                            NoteCategoryCrossRef(noteId = it.noteId, categoryId = categoryId)
                        noteViewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
                    }
                }
            }
            Toast.makeText(
                requireContext(),
                getString(R.string.update_categories),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
            noteAdapter.exitEditMode()
            startEditMode(false)
        }
        dialog.show()
    }

    private fun dialogPickColor() {
        val dialogView = layoutInflater.inflate(R.layout.pick_color, null)
        val tvColor = dialogView.findViewById<TextView>(R.id.tvColor)
        val gridlayoutColor = dialogView.findViewById<GridLayout>(R.id.gridlayoutColor)
        val tvOpacity = dialogView.findViewById<TextView>(R.id.tvOpacity)
        val sbPercentOpacity = dialogView.findViewById<SeekBar>(R.id.sbPercentOpacity)
        val btnRemoveColor = dialogView.findViewById<Button>(R.id.btnRemoveColor)
        val tvOK = dialogView.findViewById<TextView>(R.id.tvOK)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        tvOpacity.visibility = View.GONE
        sbPercentOpacity.visibility = View.GONE

        var colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
        tvColor.setBackgroundColor(colorFillBackground)

        val colorList = ColorPicker().colorBackgroundItem

        btnRemoveColor.setOnClickListener {
            colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
            tvColor.setBackgroundColor(colorFillBackground)

            for (i in 0 until gridlayoutColor.childCount) {
                val childView = gridlayoutColor.getChildAt(i) as? TextView
                childView?.text = null
            }
        }

        dialogView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val dialogWidth = dialogView.width
                val itemWidth = dialogWidth / 6
                gridlayoutColor.removeAllViews()
                gridlayoutColor.columnCount = 6

                for (color in colorList) {
                    val colorView = TextView(requireContext()).apply {
                        setBackgroundColor(Color.parseColor(color))
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = itemWidth
                            height = itemWidth
                        }
                        gravity = Gravity.CENTER
                        text = null
                        textSize = 30f
                    }
                    colorView.setOnClickListener {
                        val parseColor = Color.parseColor(color)

                        colorFillBackground = parseColor

                        tvColor.setBackgroundColor(colorFillBackground)

                        for (i in 0 until gridlayoutColor.childCount) {
                            val childView = gridlayoutColor.getChildAt(i) as? TextView
                            childView?.text = null
                            childView?.setBackgroundColor(Color.parseColor(colorList[i]))

                        }

                        "+".also { colorView.text = it }

                        colorView.setBackgroundColor(parseColor)
                    }

                    gridlayoutColor.addView(colorView)
                }
                dialogView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        tvOK.setOnClickListener {
            noteAdapter.listNoteSelectedAdapter.value?.let { it1 ->
                noteViewModel.updateBackgroundColor(
                    it1.map { it.noteId },
                    colorFillBackground
                )
            }
            noteAdapter.listNoteSelectedAdapter.value?.clear()
            noteAdapter.exitEditMode()
            isEditMode = false
            startEditMode(false)
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun sortBy() {
        val sort = sharedPreferences.getString("sort", null)
        val sortObserver = Observer<List<Note>> { notes ->
            noteAdapter.updateListNote(notes)
            allNote = notes
        }
        when (sort) {
            getString(R.string.editnewest) -> if (categoryId != null) {
                noteViewModel.sortedByUpdatedTimeDescByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByUpdatedTimeDescWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            getString(R.string.editoldest) -> if (categoryId != null) {
                noteViewModel.sortedByUpdatedTimeAscByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByUpdatedTimeAscWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            getString(R.string.a_z) -> if (categoryId != null) {
                noteViewModel.sortedByTitleAscByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByTitleAscWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            getString(R.string.z_a) -> if (categoryId != null) {
                noteViewModel.sortedByTitleDescByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByTitleDescWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            getString(R.string.createnewest) -> if (categoryId != null) {
                noteViewModel.sortedByCreatedTimeDescByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByCreatedTimeDescWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            getString(R.string.createoldest) -> if (categoryId != null) {
                noteViewModel.sortedByCreatedTimeAscByCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByCreatedTimeAscWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            getString(R.string.color) -> if (categoryId != null) {
                noteViewModel.sortedByColorWithCategory(categoryId!!)
                    .observe(viewLifecycleOwner, sortObserver)
            } else {
                noteViewModel.sortedByColorWithoutCategory()
                    .observe(viewLifecycleOwner, sortObserver)
            }

            null -> if (categoryId != null) {
                noteViewModel.getNotesByCategory(categoryId!!)
                    .observe(viewLifecycleOwner) { notes ->
                        val note = notes.map { it.note }
                        noteAdapter.updateListNote(note)
                    }
            } else {
                noteViewModel.allNoteWithoutCategory.observe(viewLifecycleOwner, sortObserver)
            }
        }
    }
}