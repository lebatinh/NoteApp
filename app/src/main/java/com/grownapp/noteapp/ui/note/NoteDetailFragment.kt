package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.util.Log
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
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note.support.FileProcess
import com.grownapp.noteapp.ui.note.support.FormatTextSupport
import com.grownapp.noteapp.ui.note.support.NoteContent
import com.grownapp.noteapp.ui.note.support.TextFormat
import com.grownapp.noteapp.ui.note.support.UndoRedoManager
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef


class NoteDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteDetailBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var sharedPreferences: SharedPreferences

    private var noteId: Int = 0
    private var category: String? = null
    private var formattedTextSegments = SpannableStringBuilder()
    private val undoRedoManager = UndoRedoManager()
    private var currentFormat = TextFormat()

    private var isOnTrash = true
    private var isShowSearch = false
    private var selectedDirectoryUri: Uri? = null
    private lateinit var listNoteSelected: MutableList<Note>
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
                listNoteSelected.add(it)
                binding.edtTitle.setText(it.title)
                if (it.note != null) {
                    val formattedText =
                        FormatTextSupport().noteContentToSpannable(
                            requireContext(),
                            Gson().fromJson(note.note!!, NoteContent::class.java)
                        )
                    binding.edtNote.text = formattedText
                    formattedTextSegments = formattedText
                } else {
                    binding.edtNote.text = SpannableStringBuilder("")
                    formattedTextSegments =
                        SpannableStringBuilder("")
                }

                val startColor = note.backgroundColor
                val endColor = Color.WHITE

                if (startColor != null && startColor != 0) {

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
                undoRedoManager.addState(formattedTextSegments)
                requireActivity().invalidateOptionsMenu()
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

        Log.d("formattedTextSegments", formattedTextSegments.toString())
        return root
    }

    override fun onPause() {
        super.onPause()
        saveNote()
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
        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_note_detail, menu)
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
        Log.d("Save Note", "Save Note")
        val spannableText = SpannableStringBuilder(binding.edtNote.text)
        val noteContent =
            FormatTextSupport().spannableToNoteContent(requireContext(), spannableText)

        val updateNote = Note().copy(
            noteId = noteId,
            title = binding.edtTitle.text.toString(),
            note = Gson().toJson(noteContent)
        )

        noteViewModel.insert(updateNote) {}
        Toast.makeText(requireContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show()
    }

    private fun showPopupMenuMore() {
        val anchorView = requireActivity().findViewById<View>(R.id.item_more)

        val popupMenu = PopupMenu(requireContext(), anchorView)

        popupMenu.menuInflater.inflate(R.menu.menu_more_note_detail, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.redo -> {
                    val previousState = undoRedoManager.redo()
                    if (previousState != null) {
                        binding.edtNote.text = previousState
                        binding.edtNote.selectionEnd
                    }
                    true
                }

                R.id.undo_all -> {
                    val previousState = undoRedoManager.undoAll()
                    if (previousState != null) {
                        binding.edtNote.text = previousState
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
                    menuItem.isVisible = !isShowSearch
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
                    true
                }

                R.id.switch_to_read_mode -> {
                    true
                }

                R.id.print -> {
                    true
                }

                R.id.show_formatting_bar -> {
                    showFormattingBar()
                    sharedPreferences.edit().putBoolean("isShowFormattingBar", true).apply()
                    requireActivity().invalidateOptionsMenu()
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

    private fun openDirectoryChooser() {
        selectDirectoryLauncher.launch(null)
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
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val btnSave = toolbar.findViewById<View>(R.id.item_save)
        val btnUndo = toolbar.findViewById<View>(R.id.item_undo)

        if (isVisible) {
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

    private fun searchKeyword(query: String) {
        val searchLayout = layoutInflater.inflate(R.layout.custom_search_toolbar, null)
        val searchCountView = searchLayout.findViewById<TextView>(R.id.searchCount)
        val text = binding.edtNote.text.toString()
        clearSearchHighlights()

        val pattern = Regex(query, RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(text).map { it.range }.toList()

        searchCountView.text = "1/${matches.size}"

        highlightMatches(matches)
    }

    private fun highlightMatches(matches: List<IntRange>) {
        val spannable = SpannableStringBuilder(binding.edtNote.text)
        val highlightColor =
            ContextCompat.getColor(requireContext(), R.color.searchHighlightColor)

        for (range in matches) {
            spannable.setSpan(
                BackgroundColorSpan(highlightColor),
                range.first,
                range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.edtNote.text = spannable
    }

    private fun clearSearchHighlights() {
        binding.edtNote.text = SpannableStringBuilder(binding.edtNote.text.toString())
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
            requireActivity().invalidateOptionsMenu()
        }

        val colorUncheck = ContextCompat.getColor(requireContext(), R.color.background)
        val colorChecked = ContextCompat.getColor(requireContext(), R.color.backgroundIconChecked)
        binding.bold.setOnClickListener {
            currentFormat = currentFormat.copy(isBold = !currentFormat.isBold)
            binding.bold.setBackgroundColor(if (!currentFormat.isBold) colorUncheck else colorChecked)
        }

        binding.italic.setOnClickListener {
            currentFormat = currentFormat.copy(isItalic = !currentFormat.isItalic)
            binding.italic.setBackgroundColor(if (!currentFormat.isItalic) colorUncheck else colorChecked)
        }

        binding.underline.setOnClickListener {
            currentFormat = currentFormat.copy(isUnderline = !currentFormat.isUnderline)
            binding.underline.setBackgroundColor(if (!currentFormat.isUnderline) colorUncheck else colorChecked)
        }

        binding.strikethrough.setOnClickListener {
            currentFormat = currentFormat.copy(isStrikethrough = !currentFormat.isStrikethrough)
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
        }
        tvOK.setOnClickListener {
            binding.fontSize.setBackgroundColor(if (s == 18) colorUncheck else colorChecked)
            dialog.dismiss()
            currentFormat = currentFormat.copy(textSize = s)
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyFormatting(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var startPos = 0
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                startPos = start+count
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val endPos = editText.selectionEnd
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

                        editText.removeTextChangedListener(this)
                        editText.text =
                            formattedTextSegments
                        editText.setSelection(endPos)

                        undoRedoManager.addState(formattedTextSegments)

                        editText.addTextChangedListener(this)
                    } else if (endPos in 0..<startPos && startPos <= formattedTextSegments.length) {
                        formattedTextSegments.delete(endPos, startPos)

                        undoRedoManager.addState(SpannableStringBuilder(formattedTextSegments))
                    }
                }
            }
        })
    }
    // TODO: lỗi khi thay thế lỗi ở vị trí thay thế đầu tiên thành chữ ở trạng thái trước đó
}