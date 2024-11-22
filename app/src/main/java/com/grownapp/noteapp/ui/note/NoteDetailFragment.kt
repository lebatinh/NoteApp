package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.adapter.NoteContentListAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.support.ChecklistItem
import com.grownapp.noteapp.ui.note.support.FileProcess
import com.grownapp.noteapp.ui.note.support.FormatTextSupport
import com.grownapp.noteapp.ui.note.support.NoteContent
import com.grownapp.noteapp.ui.note.support.TextFormat
import com.grownapp.noteapp.ui.note.support.TextSegment
import com.grownapp.noteapp.ui.note.support.UndoRedoManager
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef

class NoteDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteDetailBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var adapter = NoteContentListAdapter()

    private var noteId: Int = 0
    private var category: String? = null
    private val undoRedoManager = UndoRedoManager()
    private var formattedTextSegments = SpannableStringBuilder()
    private var currentFormat = TextFormat()
    private var defaultFormat = TextFormat()

    private var isOnTrash = true
    private var isShowSearch = false
    private var selectedDirectoryUri: Uri? = null
    private lateinit var listNoteSelected: MutableList<Note>

    private var isReadMode: Boolean = false
    private var isChecklistMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        isOnTrash = sharedPreferences.getBoolean("isOnTrash", true)
        currentFormat = currentFormat.copy(
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent),
            textColor = ContextCompat.getColor(requireContext(), R.color.text)
        )
        defaultFormat = currentFormat
        listNoteSelected = mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        arguments?.let {
            noteId = NoteDetailFragmentArgs.fromBundle(it).id
            category = arguments?.getString("categoryName")
        }

        noteViewModel.getNoteById(noteId).observe(viewLifecycleOwner) { note ->
            note?.let {
                listNoteSelected.clear()
                listNoteSelected.add(it)
                binding.edtTitle.setText(it.title)

                isChecklistMode = note.checklistMode == true

                if (isChecklistMode) {
                    binding.constraintNoteContentList.visibility = View.VISIBLE
                    binding.edtNote.visibility = View.GONE

                    val checklistItems = if (note.note != null) {
                        try {
                            val jsonElement = JsonParser.parseString(note.note)
                            if (jsonElement.isJsonArray) {
                                Gson().fromJson(note.note, Array<ChecklistItem>::class.java).toList()
                            } else {
                                val noteContent = Gson().fromJson(note.note, NoteContent::class.java)
                                noteContent.segments.map { segment ->
                                    ChecklistItem(NoteContent(listOf(segment)), false)
                                }
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                    adapter.setItems(checklistItems)

                    binding.rcvNoteContentList.layoutManager = LinearLayoutManager(requireContext())
                    binding.rcvNoteContentList.adapter = adapter
                } else {
                    val noteContent = if (it.note != null) {
                        Gson().fromJson(it.note, NoteContent::class.java)
                    } else {
                        NoteContent(emptyList())
                    }

                    formattedTextSegments = SpannableStringBuilder(
                        FormatTextSupport().noteContentToSpannable(requireContext(), noteContent)
                    )

                    undoRedoManager.addState(formattedTextSegments)

                    binding.edtNote.text = formattedTextSegments
                    binding.constraintNoteContentList.visibility = View.GONE
                    binding.edtNote.visibility = View.VISIBLE
                }
                binding.rcvNoteContentList.layoutManager = LinearLayoutManager(requireContext())
                binding.rcvNoteContentList.adapter = adapter

                binding.tvAddCategories.setOnClickListener {
                    val newItem = ChecklistItem(NoteContent(listOf(TextSegment(""))), false)
                    adapter.addItem(newItem)
                    binding.rcvNoteContentList.scrollToPosition(adapter.itemCount - 1)
                }

                val startColor = note.backgroundColor
                startColor?.let { it1 -> applyBackgroundGradient(it1) }
            }
        }

        val isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
        binding.constraint.visibility = if (isShowFormattingBar) View.VISIBLE else View.GONE
        if (isShowFormattingBar) {
            showFormattingBar()
        }

        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.back)
        toolbar.setNavigationOnClickListener {
            view?.let {
                findNavController().navigateUp()
            }
        }

        applyFormatting(binding.edtNote)
        return root
    }

    override fun onPause() {
        super.onPause()
        saveNote()

        toggleSearchToolbar(false)
        readMode(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).setupDefaultToolbar()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.topBarBackgroundLight
            )
        )
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isShowSearch) {
                        isShowSearch = false
                        toggleSearchToolbar(false)
                    } else {
                        isEnabled = false
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    }
                }
            })

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_note_detail, menu)

        val undoItem = menu.findItem(R.id.item_undo)
        undoItem.isEnabled = !isChecklistMode
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_save -> {
                saveNote()
                return true
            }

            R.id.item_undo -> {
                val previousState = undoRedoManager.undo()
                if (previousState != null) {
                    binding.edtNote.text = previousState
                    formattedTextSegments = previousState
                    binding.edtNote.selectionEnd
                }
            }

            R.id.item_more -> {
                showPopupMenuMore()
                return true
            }
        }
        return false
    }

    private fun saveNote() {
        val currentNoteContent = if (isChecklistMode) {
            Gson().toJson(adapter.getItems())
        } else {
            Gson().toJson(FormatTextSupport().spannableToNoteContent(requireContext(), formattedTextSegments))
        }

        val updateNote = Note().copy(
            noteId = noteId,
            title = binding.edtTitle.text.toString(),
            note = currentNoteContent,
            checklistMode = isChecklistMode
        )

        noteViewModel.insert(updateNote) {}
        Toast.makeText(requireContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show()
    }

    private fun showPopupMenuMore() {
        val anchorView = requireActivity().findViewById<View>(R.id.item_more)

        val popupMenu = PopupMenu(requireContext(), anchorView)

        if (isReadMode) {
            popupMenu.menuInflater.inflate(R.menu.menu_more_note_detail_read_mode, popupMenu.menu)
        } else {
            popupMenu.menuInflater.inflate(R.menu.menu_more_note_detail, popupMenu.menu)
        }

        val redo = popupMenu.menu.findItem(R.id.redo)
        val undoAll = popupMenu.menu.findItem(R.id.undo_all)
        val showFormattingBar = popupMenu.menu.findItem(R.id.show_formatting_bar)
        val convertToChecklistItem = popupMenu.menu.findItem(R.id.convert_to_checklist)

        var isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
        showFormattingBar.isEnabled = isShowFormattingBar

        if (isChecklistMode) {
            convertToChecklistItem?.title = getString(R.string.convert_to_text)
            redo.isEnabled = false
            undoAll.isEnabled = false
            showFormattingBar.isEnabled = false
        } else {
            convertToChecklistItem?.title = getString(R.string.convert_to_checklist)
            if (!isReadMode) {
                redo.isEnabled = true
                undoAll.isEnabled = true
                sharedPreferences.edit().putBoolean("isShowFormattingBar", false).apply()
                isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
                showFormattingBar.isEnabled = !isShowFormattingBar
            }
        }

        val search = popupMenu.menu.findItem(R.id.search)
        search.isVisible = !isShowSearch

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.redo -> {
                    val previousState = undoRedoManager.redo()
                    if (previousState != null) {
                        binding.edtNote.text = previousState
                        formattedTextSegments = previousState
                        binding.edtNote.selectionEnd
                    }
                    true
                }

                R.id.undo_all -> {
                    val previousState = undoRedoManager.undoAll()
                    if (previousState != null) {
                        binding.edtNote.text = previousState
                        formattedTextSegments = previousState
                        binding.edtNote.selectionEnd
                    }
                    true
                }

                R.id.share -> {
                    true
                }

                R.id.delete -> {
                    showDialogDelete()
                    true
                }

                R.id.search -> {
                    isShowSearch = true
                    toggleSearchToolbar(isShowSearch)
                    true
                }

                R.id.export_to_a_text_files -> {
                    FileProcess().checkAndRequestPermissions {
                        openDirectoryChooser()
                    }
                    true
                }

                R.id.categorize -> {
                    showCategorizeDialog(noteId)
                    true
                }

                R.id.colorize -> {
                    dialogPickBackgroundColor()
                    true
                }

                R.id.convert_to_checklist -> {
                    currentFormat = defaultFormat
                    isChecklistMode = !isChecklistMode
                    convertToChecklist()
                    saveNote()
                    noteViewModel.updateChecklistMode(noteId, isChecklistMode)
                    true
                }

                R.id.switch_to_read_mode -> {
                    readMode(true)
                    true
                }

                R.id.print -> {
                    true
                }

                R.id.show_formatting_bar -> {
                    showFormattingBar()
                    sharedPreferences.edit().putBoolean("isShowFormattingBar", true).apply()
                    true
                }

                R.id.show_info -> {
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun convertToChecklist() {
        if (isChecklistMode) {
            val noteContent = FormatTextSupport().spannableToNoteContent(requireContext(), formattedTextSegments)
            adapter.setItemsFromNoteContent(Gson().toJson(noteContent))

            binding.constraintNoteContentList.visibility = View.VISIBLE
            binding.edtNote.visibility = View.GONE
            binding.constraint.visibility = View.GONE
        } else {
            val noteContent = adapter.convertChecklistToNoteContent()
            formattedTextSegments = SpannableStringBuilder(
                FormatTextSupport().noteContentToSpannable(
                    requireContext(),
                    Gson().fromJson(noteContent, NoteContent::class.java)
                )
            )
            binding.edtNote.text = formattedTextSegments
            binding.constraintNoteContentList.visibility = View.GONE
            binding.edtNote.visibility = View.VISIBLE
        }
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
                    listNoteSelected
                )
            }
        }

    private fun dialogPickBackgroundColor() {
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

                for (color in ColorPicker().colorBackgroundItem) {
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
                            childView?.setBackgroundColor(Color.parseColor(ColorPicker().colorBackgroundItem[i]))

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
            noteViewModel.updateBackgroundColor(
                listOf(noteId),
                colorFillBackground
            )
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun toggleSearchToolbar(isVisible: Boolean) {
        isShowSearch = isVisible
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val btnSave = toolbar.findViewById<View>(R.id.item_save)
        val btnUndo = toolbar.findViewById<View>(R.id.item_undo)

        if (isShowSearch) {
            btnSave?.visibility = View.GONE
            btnUndo?.visibility = View.GONE

            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )

            val searchLayout =
                layoutInflater.inflate(R.layout.custom_search_toolbar, toolbar, false)

            val searchCountView = searchLayout.findViewById<TextView>(R.id.searchCount)
            val btnPrev = searchLayout.findViewById<ImageView>(R.id.imgArrowUp)
            val btnNext = searchLayout.findViewById<ImageView>(R.id.imgArrowDown)
            val search = searchLayout.findViewById<SearchView>(R.id.search)

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                toggleSearchToolbar(false)

                searchCountView?.visibility = View.GONE
                btnPrev?.visibility = View.GONE
                btnNext?.visibility = View.GONE
                search?.visibility = View.GONE

                clearSearchHighlights()
            }
            toolbar.addView(searchLayout, layoutParams)

            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        searchKeyword(it)
                    }
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        searchKeyword(it)
                    }
                    return false
                }
            })
            btnPrev.setOnClickListener {
                highlightPreviousResult()
            }
            btnNext.setOnClickListener {
                highlightNextResult()
            }
        } else {
            btnSave?.visibility = View.VISIBLE
            btnUndo?.visibility = View.VISIBLE

            val searchLayout =
                toolbar.findViewById<View>(R.id.custom_search_toolbar)
            if (searchLayout != null) {
                toolbar.removeView(searchLayout)
            }

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                view?.let {
                    findNavController().navigateUp()
                }
            }

            clearSearchHighlights()
        }
    }

    private fun readMode(enableReadMode: Boolean) {
        isReadMode = enableReadMode
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val btnSave = toolbar.findViewById<View>(R.id.item_save)
        val btnUndo = toolbar.findViewById<View>(R.id.item_undo)

        if (isReadMode) {
            btnSave?.visibility = View.GONE
            btnUndo?.visibility = View.GONE

            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )
            val readModeLayout =
                layoutInflater.inflate(R.layout.custom_readmode_toolbar, toolbar, false)

            val existingReadModeLayout = toolbar.findViewById<View>(R.id.custom_readmode_toolbar)
            if (existingReadModeLayout != null) {
                toolbar.removeView(existingReadModeLayout)
            }
            toolbar.addView(readModeLayout, layoutParams)

            binding.edtTitle.isEnabled = false
            binding.edtNote.isEnabled = false

            binding.edtTitle.hint = ""
            binding.edtNote.hint = ""
            binding.constraint.visibility = View.GONE
            sharedPreferences.edit().putBoolean("isShowFormattingBar", false).apply()

            val imgExportNote = toolbar.findViewById<ImageView>(R.id.imgExportNote)
            val imgEdit = toolbar.findViewById<ImageView>(R.id.imgEdit)

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                imgExportNote?.visibility = View.GONE
                imgEdit?.visibility = View.GONE
                readMode(false)
            }

            imgExportNote.setOnClickListener {
                FileProcess().checkAndRequestPermissions {
                    openDirectoryChooser()
                }

                readMode(true)
            }
            imgEdit.setOnClickListener {
                readMode(false)
            }
        } else {
            btnSave?.visibility = View.VISIBLE
            btnUndo?.visibility = View.VISIBLE

            val readModeLayout =
                toolbar.findViewById<View>(R.id.custom_readmode_toolbar)
            if (readModeLayout != null) {
                toolbar.removeView(readModeLayout)
            }
            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                view?.let {
                    findNavController().navigateUp()
                }
            }

            binding.edtTitle.isEnabled = true
            binding.edtNote.isEnabled = true
        }
    }

    private fun searchKeyword(query: String) {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val searchLayout = layoutInflater.inflate(R.layout.custom_search_toolbar, toolbar, false)
        val searchCountView = searchLayout.findViewById<TextView>(R.id.searchCount)
        val text = binding.edtNote.text?.toString() ?: ""

        if (text.isEmpty()) return

        clearSearchHighlights()

        val pattern = Regex(query, RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(text).map { it.range }.toList()
        val validMatches = matches.filter { it.first >= 0 && it.last < text.length }
        highlightMatches(validMatches)

        searchCountView.text = buildString {
            append("1/")
            append(matches.size)
        }
    }

    private fun highlightMatches(matches: List<IntRange>) {
        val editable = binding.edtNote.editableText
        val highlightColor = ContextCompat.getColor(requireContext(), R.color.searchHighlightColor)

        // Xóa các spans cũ trước khi thêm spans mới
        val spans = editable.getSpans(0, editable.length, BackgroundColorSpan::class.java)
        for (span in spans) {
            editable.removeSpan(span)
        }

        // Thêm spans mới
        for (range in matches) {
            if (range.first >= 0 && range.last < editable.length) {
                editable.setSpan(
                    BackgroundColorSpan(highlightColor),
                    range.first,
                    range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun clearSearchHighlights() {
        binding.edtNote.text = SpannableStringBuilder(binding.edtNote.text)
    }

    private fun highlightPreviousResult() {}

    private fun highlightNextResult() {}

    private fun showDialogDelete() {
        val dialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
        val deleteLog = dialogView.findViewById<TextView>(R.id.delete_log)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        noteViewModel.getNoteById(noteId).observe(viewLifecycleOwner) { note ->
            if (isOnTrash) {
                getString(
                    R.string.delete_log_detail, note.title ?: note.note?.substring(
                        0,
                        20
                    ) ?: getString(R.string.untitled)
                ).also { deleteLog.text = it }
            } else {
                deleteLog.text = buildString {
                    getString(
                        R.string.delete_log_detail_permanently, note.title ?: note.note?.substring(
                            0,
                            20
                        ) ?: getString(R.string.untitled)
                    )
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            if (isOnTrash) {
                noteViewModel.pushInTrash(true, noteId)
                val editor = sharedPreferences.edit()
                editor.putBoolean("isOnTrash", true).apply()
            } else {
                noteViewModel.delete(noteId)
            }
            Toast.makeText(requireContext(), "deleted $noteId", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_noteDetailFragment_to_nav_note)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showCategorizeDialog(noteId: Int?) {
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

        if (noteId != null) {
            noteViewModel.getCategoryOfNote(noteId).observe(viewLifecycleOwner) { c ->
                selectedCategory.addAll(c.map { it.categoryId })
                categoryViewModel.allCategory.observe(this) { categories ->
                    categoryMutableList.clear()
                    categoryMutableList.addAll(categories)
                    categoryForNoteAdapter.updateListCategory(categoryMutableList)
                }
            }
        }
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            if (noteId != null) {
                noteViewModel.deleteCategoriesForNote(noteId)
                for (categoryId in selectedCategory) {
                    val noteCategoryCrossRef =
                        NoteCategoryCrossRef(noteId = noteId, categoryId = categoryId)
                    noteViewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
                }
            }
            Toast.makeText(
                requireContext(),
                getString(R.string.update_categories),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showFormattingBar() {
        binding.constraint.visibility = View.VISIBLE
        binding.hideFormattingBar.setOnClickListener {
            binding.constraint.visibility = View.GONE
            sharedPreferences.edit().putBoolean("isShowFormattingBar", false).apply()
        }

        val colorUncheck = ContextCompat.getColor(requireContext(), R.color.background)
        val colorChecked = ContextCompat.getColor(requireContext(), R.color.backgroundIconChecked)

        binding.bold.setOnClickListener {
            currentFormat = currentFormat.copy(isBold = !currentFormat.isBold)

            val start = binding.edtNote.selectionStart
            val end = binding.edtNote.selectionEnd
            val spannable = SpannableStringBuilder(binding.edtNote.text)

            if (start != end) {
                val boldSpans = spannable.getSpans(start, end, StyleSpan::class.java)
                if (!currentFormat.isBold) {
                    for (span in boldSpans) {
                        if (span.style == Typeface.BOLD) {
                            spannable.removeSpan(span)
                        }
                    }
                } else {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            binding.bold.setBackgroundColor(if (!currentFormat.isBold) colorUncheck else colorChecked)
        }

        binding.italic.setOnClickListener {
            currentFormat = currentFormat.copy(isItalic = !currentFormat.isItalic)

            val start = binding.edtNote.selectionStart
            val end = binding.edtNote.selectionEnd
            val spannable = SpannableStringBuilder(binding.edtNote.text)

            if (start != end) {
                val italicSpans = spannable.getSpans(start, end, StyleSpan::class.java)
                if (!currentFormat.isItalic) {
                    for (span in italicSpans) {
                        if (span.style == Typeface.ITALIC) {
                            spannable.removeSpan(span)
                        }
                    }
                } else {
                    spannable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            binding.italic.setBackgroundColor(if (!currentFormat.isItalic) colorUncheck else colorChecked)
        }

        binding.underline.setOnClickListener {
            currentFormat = currentFormat.copy(isUnderline = !currentFormat.isUnderline)

            val start = binding.edtNote.selectionStart
            val end = binding.edtNote.selectionEnd
            val spannable = SpannableStringBuilder(binding.edtNote.text)

            if (start != end) {
                val underlineSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
                if (!currentFormat.isUnderline) {
                    for (span in underlineSpans) {
                        spannable.removeSpan(span)
                    }
                } else {
                    spannable.setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            binding.underline.setBackgroundColor(if (!currentFormat.isUnderline) colorUncheck else colorChecked)
        }

        binding.strikethrough.setOnClickListener {
            currentFormat = currentFormat.copy(isStrikethrough = !currentFormat.isStrikethrough)

            val start = binding.edtNote.selectionStart
            val end = binding.edtNote.selectionEnd
            val spannable = SpannableStringBuilder(binding.edtNote.text)

            if (start != end) {
                val strikeSpans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
                if (!currentFormat.isStrikethrough) {
                    for (span in strikeSpans) {
                        spannable.removeSpan(span)
                    }
                } else {
                    spannable.setSpan(
                        StrikethroughSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            binding.strikethrough.setBackgroundColor(if (!currentFormat.isStrikethrough) colorUncheck else colorChecked)
        }

        binding.fillColorBackground.setOnClickListener {
            dialogPickColor(true)
        }

        binding.fillColorText.setOnClickListener {
            dialogPickColor(false)
        }

        binding.fontSize.setOnClickListener {
            dialogPickTextSize(currentFormat.textSize, colorChecked, colorUncheck)
        }
    }

    private fun dialogPickColor(isBackground: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.pick_color, null)
        val tvColor = dialogView.findViewById<TextView>(R.id.tvColor)
        val gridlayoutColor = dialogView.findViewById<GridLayout>(R.id.gridlayoutColor)
        val tvOpacity = dialogView.findViewById<TextView>(R.id.tvOpacity)
        val sbPercentOpacity = dialogView.findViewById<SeekBar>(R.id.sbPercentOpacity)
        val btnRemoveColor = dialogView.findViewById<Button>(R.id.btnRemoveColor)
        val tvOK = dialogView.findViewById<TextView>(R.id.tvOK)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        var backgroundColor = currentFormat.backgroundColor
        var textColor = currentFormat.textColor

        var colorFillBackground = backgroundColor
        var colorFillTextColor = textColor
        var alpha: Int

        var isTextColorRemoved = false

        if (isBackground) {
            alpha = Color.alpha(backgroundColor)
            sbPercentOpacity.progress = (alpha * 100 / 255)
            tvColor.setBackgroundColor(backgroundColor)
        } else {
            alpha = Color.alpha(textColor)
            sbPercentOpacity.progress = (alpha * 100 / 255)
            tvColor.setTextColor(textColor)
        }

        btnRemoveColor.setOnClickListener {
            if (isBackground) {
                colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
                tvColor.setBackgroundColor(colorFillBackground)
            } else {
                isTextColorRemoved = true
                colorFillTextColor = ContextCompat.getColor(requireContext(), R.color.text)
                tvColor.setTextColor(colorFillTextColor)
            }
            applyRangeFormatting()
            for (i in 0 until gridlayoutColor.childCount) {
                val childView = gridlayoutColor.getChildAt(i) as? TextView
                childView?.text = null
            }
        }

        "Opacity (${sbPercentOpacity.progress}%)".also { tvOpacity.text = it }
        sbPercentOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "Opacity (${progress}%)".also { tvOpacity.text = it }
                alpha = (progress * 255 / 100)

                if (isBackground) {
                    val colorWithOpacity = Color.argb(
                        alpha,
                        Color.red(backgroundColor),
                        Color.green(backgroundColor),
                        Color.blue(backgroundColor)
                    )
                    tvColor.setBackgroundColor(colorWithOpacity)
                    colorFillBackground = colorWithOpacity
                } else {
                    val colorWithOpacity = Color.argb(
                        alpha,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    )
                    tvColor.setTextColor(colorWithOpacity)
                    colorFillTextColor = colorWithOpacity
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val dialogWidth = dialogView.width
                val itemWidth = dialogWidth / 8
                gridlayoutColor.removeAllViews()

                for (color in ColorPicker().colors) {
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
                        if (isBackground) {
                            backgroundColor = parseColor

                            colorFillBackground = Color.argb(
                                alpha,
                                Color.red(parseColor),
                                Color.green(parseColor),
                                Color.blue(parseColor)
                            )
                            tvColor.setBackgroundColor(colorFillBackground)
                        } else {
                            textColor = parseColor
                            colorFillTextColor = Color.argb(
                                alpha,
                                Color.red(parseColor),
                                Color.green(parseColor),
                                Color.blue(parseColor)
                            )
                            tvColor.setTextColor(colorFillTextColor)
                        }

                        for (i in 0 until gridlayoutColor.childCount) {
                            (gridlayoutColor.getChildAt(i) as? TextView)?.text = null
                        }

                        colorView.text = getString(R.string.sum)

                        colorView.setTextColor(
                            ContextCompat.getColor(
                                requireContext(), R.color.backgroundPickColorDialog
                            )
                        )
                    }

                    gridlayoutColor.addView(colorView)
                }
                dialogView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        tvOK.setOnClickListener {
            if (isBackground) {
                binding.fillColorBackground.setBackgroundColor(colorFillBackground)
                currentFormat = currentFormat.copy(backgroundColor = colorFillBackground)
            } else {
                if (isTextColorRemoved) {
                    binding.fillColorText.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.transparent
                        )
                    )
                } else {
                    binding.fillColorText.setBackgroundColor(colorFillTextColor)
                }
                currentFormat = currentFormat.copy(textColor = colorFillTextColor)
            }
            applyRangeFormatting()
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dialogPickTextSize(size: Int, colorChecked: Int, colorUncheck: Int) {
        val dialogView = layoutInflater.inflate(R.layout.pick_size, null)
        val tvTextSize = dialogView.findViewById<TextView>(R.id.tvTextSize)
        val sbTextSize = dialogView.findViewById<SeekBar>(R.id.sbTextSize)
        val btnSetDefault = dialogView.findViewById<Button>(R.id.btnSetDefault)
        val tvOK = dialogView.findViewById<TextView>(R.id.tvOK)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        var s = size
        sbTextSize.progress = currentFormat.textSize
        "Text size ${currentFormat.textSize}".also { tvTextSize.text = it }
        tvTextSize.textSize = currentFormat.textSize.toFloat()

        sbTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "Text size $progress".also { tvTextSize.text = it }
                tvTextSize.textSize = progress.toFloat()
                s = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        })
        btnSetDefault.setOnClickListener {
            tvTextSize.textSize = 18f
            tvTextSize.text = getString(R.string.text_size_18)
            sbTextSize.setProgress(18, true)
            currentFormat.textSize = 18
            applyRangeFormatting()
        }
        tvOK.setOnClickListener {
            binding.fontSize.setBackgroundColor(if (s == 18) colorUncheck else colorChecked)
            currentFormat = currentFormat.copy(textSize = s)
            applyRangeFormatting()
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyFormatting(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var startPos = 0
            var previousSelectionStart = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                startPos = start + count
                previousSelectionStart = start
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val endPos = editText.selectionEnd

                    editText.removeTextChangedListener(this)
                    if (startPos < endPos) {
                        val newText = it.subSequence(startPos, endPos)
                        formattedTextSegments.insert(startPos, newText)

                        FormatTextSupport().applyCurrentFormat(
                            requireContext(),
                            currentFormat,
                            formattedTextSegments,
                            startPos,
                            endPos
                        )

                        editText.text = formattedTextSegments
                        editText.setSelection(endPos)
                        undoRedoManager.addState(formattedTextSegments)
                    } else if (endPos in 0 until startPos) {
                        if (previousSelectionStart == endPos) {
                            if (formattedTextSegments.isNotEmpty()) {
                                formattedTextSegments.delete(endPos, startPos)
                            }
                        } else if (previousSelectionStart < endPos) {
                            if (formattedTextSegments.isNotEmpty()) {
                                formattedTextSegments.delete(previousSelectionStart, startPos)
                            }
                            val replaceText = it.subSequence(previousSelectionStart, endPos)

                            formattedTextSegments.insert(previousSelectionStart, replaceText)

                            FormatTextSupport().applyCurrentFormat(
                                requireContext(),
                                currentFormat,
                                formattedTextSegments,
                                previousSelectionStart,
                                previousSelectionStart + replaceText.length
                            )

                            editText.text = formattedTextSegments
                            editText.setSelection(endPos)
                            undoRedoManager.addState(formattedTextSegments)
                        }
                    }
                    editText.addTextChangedListener(this)
                }
            }
        })
    }

    private fun applyRangeFormatting() {
        val start = binding.edtNote.selectionStart
        val end = binding.edtNote.selectionEnd
        val spannable = SpannableStringBuilder(binding.edtNote.text)

        if (start != end) {
            val textSpans = spannable.getSpans(start, end, Any::class.java)

            for (span in textSpans) {
                if (span is StyleSpan) {
                    if (span.style == Typeface.BOLD && !currentFormat.isBold) spannable.removeSpan(
                        span
                    )
                    if (span.style == Typeface.ITALIC && !currentFormat.isItalic) spannable.removeSpan(
                        span
                    )
                }
                if (span is UnderlineSpan && !currentFormat.isUnderline) {
                    spannable.removeSpan(span)
                }
                if (span is StrikethroughSpan && !currentFormat.isStrikethrough) {
                    spannable.removeSpan(span)
                }
                if (span is BackgroundColorSpan && currentFormat.backgroundColor != span.backgroundColor) {
                    spannable.removeSpan(span)
                }
                if (span is ForegroundColorSpan && currentFormat.textColor != span.foregroundColor) {
                    spannable.removeSpan(span)
                }
                if (span is AbsoluteSizeSpan && span.size != currentFormat.textSize) {
                    spannable.removeSpan(span)
                }
            }

            if (currentFormat.isBold) spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (currentFormat.isItalic) spannable.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (currentFormat.isUnderline) spannable.setSpan(
                UnderlineSpan(),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (currentFormat.isStrikethrough) spannable.setSpan(
                StrikethroughSpan(),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (currentFormat.backgroundColor != Color.TRANSPARENT) spannable.setSpan(
                BackgroundColorSpan(currentFormat.backgroundColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (currentFormat.textColor != Color.BLACK) spannable.setSpan(
                ForegroundColorSpan(
                    currentFormat.textColor
                ), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannable.setSpan(
                AbsoluteSizeSpan(currentFormat.textSize, true),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyBackgroundGradient(startColor: Int) {
        val endColor = Color.WHITE
        if (startColor != 0) {
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(startColor, endColor)
            )
            gradientDrawable.setStroke(0, Color.TRANSPARENT)

            val borderDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.background_add_note)
                    ?.mutate()
            (borderDrawable as? GradientDrawable)?.setColor(Color.TRANSPARENT)
            val layerDrawable = LayerDrawable(arrayOf(gradientDrawable, borderDrawable))

            binding.fragmentNoteDetail.background = layerDrawable

            val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
            toolbar.setBackgroundColor(startColor)
        } else {
            binding.fragmentNoteDetail.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.background_add_note)
        }
    }
}

// TODO nếu chuyển từ checklist về chế độ thường rồi viết chữ ở bất kì vị trí nào khác 0 thì sẽ crash (lúc này formattedTextSegments rỗng)
// nếu thêm mã dưới vào trước formattedTextSegments.insert thì sẽ không còn lỗi crash nhưng sẽ có lỗi thêm 1 chữ không có định dạng ở sau chữ vừa được thêm vào (sau con trỏ)
// if (formattedTextSegments.isEmpty()) {
//        formattedTextSegments.append(binding.edtNote.text)
//    }