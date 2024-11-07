package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewModel
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        isOnTrash = sharedPreferences.getBoolean("isOnTrash", true)
        currentFormat = currentFormat.copy(
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent),
            textColor = ContextCompat.getColor(requireContext(), R.color.text)
        )
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
                binding.edtTitle.setText(it.title)

                val startColor = note.backgroundColor
                val endColor = Color.WHITE
                if (startColor!= null && startColor != 0){

                    // Tạo GradientDrawable với hai màu
                    val gradientDrawable = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM, // Hướng gradient từ trên xuống dưới
                        intArrayOf(startColor, endColor)         // Mảng chứa hai màu gradient
                    )
                    gradientDrawable.setStroke(0, Color.TRANSPARENT)

                    // Tải drawable `background_add_note` từ tài nguyên
                    val borderDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.background_add_note)?.mutate()
                    (borderDrawable as? GradientDrawable)?.setColor(Color.TRANSPARENT)
                    val layerDrawable = LayerDrawable(arrayOf(gradientDrawable, borderDrawable))

                    binding.fragmentNoteDetail.background = layerDrawable

                    val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
                    toolbar.setBackgroundColor(startColor)
                }else{
                    binding.fragmentNoteDetail.background= ContextCompat.getDrawable(requireContext(), R.drawable.background_add_note)
                }

                if (it.note != null) {
                    binding.edtNote.text = noteContentToSpannable(
                        Gson().fromJson(
                            note.note!!,
                            NoteContent::class.java
                        )
                    )
                    formattedTextSegments =
                        noteContentToSpannable(Gson().fromJson(note.note, NoteContent::class.java))
                } else {
                    // Trường hợp `note` null hoặc không có nội dung
                    binding.edtNote.text = SpannableStringBuilder("") // Đặt `edtNote` là chuỗi rỗng
                    formattedTextSegments =
                        SpannableStringBuilder("") // Đặt chuỗi định dạng là rỗng
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

        applyFormatting(binding.edtNote)

        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.back)
        toolbar.setNavigationOnClickListener {
            view?.let {
                findNavController().navigateUp()
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
        (activity as MainActivity).setupDefaultToolbar()
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.topBarBackgroundLight))
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
                    binding.edtNote.setSelection(previousState.length.coerceAtMost(previousState.length))
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
        val spannableText = binding.edtNote.text as SpannableStringBuilder
        val noteContent = spannableToNoteContent(spannableText)

        Log.d("noteContent", noteContent.toString())
        val updateNote = Note().copy(
            noteId = noteId,
            title = binding.edtTitle.text.toString(),
            note = Gson().toJson(noteContent)
        )

        noteViewModel.insert(updateNote) {}
        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
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
                        binding.edtNote.setSelection(previousState.length)
                    }
                    true
                }

                R.id.undo_all -> {
                    val previousState = undoRedoManager.undoAll()
                    if (previousState != null) {
                        binding.edtNote.text = previousState
                        binding.edtNote.setSelection(previousState.length)
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

        // Hiển thị PopupMenu
        popupMenu.show()
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

            // Cài đặt listener cho `SearchView`
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
                toolbar.findViewById<View>(R.id.custom_search_toolbar) // Tìm kiếm layout
            if (searchLayout != null) {
                toolbar.removeView(searchLayout) // Xóa searchLayout
            }

            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                // nếu dùng thì k dùng back và menu điều hướng đc
                view?.let {
                    findNavController().navigateUp()
                }
            }

            clearSearchHighlights()
        }
    }

    // Hàm tìm kiếm từ khóa và cập nhật vị trí
    private fun searchKeyword(query: String) {
        val searchLayout = layoutInflater.inflate(R.layout.custom_search_toolbar, null)
        val searchCountView = searchLayout.findViewById<TextView>(R.id.searchCount)
        // Lấy nội dung văn bản từ edtNote
        val text = binding.edtNote.text.toString()
        // Xóa highlight cũ
        clearSearchHighlights()

        // Tìm tất cả vị trí của từ khóa
        val pattern = Regex(query, RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(text).map { it.range }.toList()

        // Hiển thị số lượng tìm thấy
        searchCountView.text = "1/${matches.size}"

        // Tô màu cho từ khóa tìm thấy
        highlightMatches(matches)
    }

    // Hàm tô màu kết quả tìm kiếm
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

    // Hàm xóa highlight khi thoát tìm kiếm
    private fun clearSearchHighlights() {
        binding.edtNote.text = SpannableStringBuilder(binding.edtNote.text.toString())
    }

    // Hàm di chuyển đến kết quả trước đó
    private fun highlightPreviousResult() {
        // Xử lý logic di chuyển đến kết quả trước đó
    }

    // Hàm di chuyển đến kết quả tiếp theo
    private fun highlightNextResult() {
        // Xử lý logic di chuyển đến kết quả tiếp theo
    }

    private fun showDialogDelete() {
        val dialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
        val deleteLog = dialogView.findViewById<TextView>(R.id.delete_log)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        noteViewModel.getNoteById(noteId).observe(viewLifecycleOwner) { note ->
            if (isOnTrash) {
                "The '${
                    note.title ?: note.note?.substring(
                        0,
                        20
                    ) ?: "Untitled"
                }' note will be deleted.\nAre you sure?".also { deleteLog.text = it }
            } else {
                deleteLog.text = buildString {
                    append("The note will be deleted permanently!\n")
                    append(
                        "Are you sure that you want to delete the '${
                            note.title ?: note.note?.substring(
                                0,
                                20
                            ) ?: "Untitled"
                        }' note?"
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
        val cancelButon = dialogView.findViewById<TextView>(R.id.btnCancel)
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

        cancelButon.setOnClickListener {
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
            Toast.makeText(requireContext(), "Update categories", Toast.LENGTH_SHORT).show()
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
            Log.d("isBold", currentFormat.isBold.toString())
        }

        binding.italic.setOnClickListener {
            currentFormat = currentFormat.copy(isItalic = !currentFormat.isItalic)
            binding.italic.setBackgroundColor(if (!currentFormat.isItalic) colorUncheck else colorChecked)
            Log.d("isItalic", currentFormat.isItalic.toString())
        }

        binding.underline.setOnClickListener {
            currentFormat = currentFormat.copy(isUnderline = !currentFormat.isUnderline)
            binding.underline.setBackgroundColor(if (!currentFormat.isUnderline) colorUncheck else colorChecked)
            Log.d("isUnderline", currentFormat.isUnderline.toString())
        }

        binding.strikethrough.setOnClickListener {
            currentFormat = currentFormat.copy(isStrikethrough = !currentFormat.isStrikethrough)
            binding.strikethrough.setBackgroundColor(if (!currentFormat.isStrikethrough) colorUncheck else colorChecked)
            Log.d("isStrikethrough", currentFormat.isStrikethrough.toString())
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

                        "+".also { colorView.text = it }

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
                Log.d("backgroundColor", colorFillBackground.toString())
            } else {
                if (isTextColorRemoved){
                    binding.fillColorText.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                }else{
                    binding.fillColorText.setBackgroundColor(colorFillTextColor)
                }
                currentFormat = currentFormat.copy(textColor = colorFillTextColor)
                Log.d("textColor", colorFillTextColor.toString())
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
            Log.d("fontSize", currentFormat.textSize.toString())
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyFormatting(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var startPos = 0
            var previousText: String = ""
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isFormatting) return
                startPos = start // Lưu vị trí bắt đầu của thay đổi
                previousText = s.toString() // Lưu văn bản trước khi thay đổi
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    try {
                        isFormatting = true
                        val currentLength = it.length
                        if (startPos < 0 || startPos >= currentLength) return@let

                        val endPos = startPos + (it.length - previousText.length)
                        if (endPos < 0 || endPos > it.length) return@let

                        val newText = it.subSequence(
                            startPos.coerceAtMost(it.length),
                            endPos.coerceAtMost(it.length)
                        ).toString()
                        applyCurrentFormat(it, startPos, endPos)

//                        for (i in newText.indices) {
//                            val charStart = startPos + i
//                            val charEnd = charStart + 1
//
//                            if (editText.selectionStart < editText.selectionEnd) {
//                                insertTextAtCursorPosition(editText, newText)
//                                Log.d("afterTextChanged", "insertTextAtCursorPosition:$editText/$newText")
//                            } else {
//                                applyCurrentFormat(it, charStart, charEnd)
//                                Log.d("afterTextChanged", "applyCurrentFormat:$it/$charStart/$charEnd")
//                            }
//                        }

                        editText.setSelection(endPos)
                        undoRedoManager.addState(SpannableStringBuilder(it))

                        if (s.toString() != previousText) {
                            requireActivity().invalidateOptionsMenu()
                        }
                    } finally {
                        isFormatting = false
                    }
                }
            }
        })
    }

    // Hàm xử lý chèn văn bản vào giữa đoạn văn bản
    fun insertTextAtCursorPosition(editText: EditText, insertText: String) {
        val startPos = editText.selectionStart
        val endPos = editText.selectionEnd
        val currentText = editText.text as SpannableStringBuilder

        // Xóa đoạn văn bản được chọn (nếu có)
        currentText.delete(startPos, endPos)
        Log.d("insertTextAtCursorPosition", "delete:$startPos-$endPos")

        // Chèn văn bản mới vào vị trí con trỏ
        currentText.insert(startPos, insertText)
        Log.d("insertTextAtCursorPosition", "insert:$startPos-$insertText")

        // Áp dụng định dạng ngay sau khi chèn
        val newEndPos = startPos + insertText.length
        applyCurrentFormat(currentText, startPos, newEndPos)
        Log.d("insertTextAtCursorPosition", "applyCurrentFormat:$currentText/$startPos/$newEndPos")

        // Đặt con trỏ vào cuối đoạn văn bản chèn
        editText.setSelection(newEndPos)
    }

    // Hàm mở rộng cho TextSegment để áp dụng định dạng vào Editable
    private fun applyCurrentFormat(text: Editable, start: Int, end: Int) {

        val defautBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.transparent)
        val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)

        if (currentFormat.isBold) text.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.isItalic) text.setSpan(
            StyleSpan(Typeface.ITALIC),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.isUnderline) text.setSpan(
            UnderlineSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.isStrikethrough) text.setSpan(
            StrikethroughSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.backgroundColor != defautBackgroundColor) text.setSpan(
            BackgroundColorSpan(
                currentFormat.backgroundColor
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.textColor != defautTextColor) text.setSpan(
            ForegroundColorSpan(
                currentFormat.textColor
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        text.setSpan(
            AbsoluteSizeSpan(currentFormat.textSize, true),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun spannableToNoteContent(spannable: SpannableStringBuilder): NoteContent {
        val defaultBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.transparent)
        val defaultTextColor = ContextCompat.getColor(requireContext(), R.color.text)
        val segments = mutableListOf<TextSegment>()
        var start = 0

        // Lặp qua từng ký tự trong spannable
        while (start < spannable.length) {
            val end = spannable.nextSpanTransition(start, spannable.length, Any::class.java)
            val text = spannable.subSequence(start, end).toString()

            // Lấy các định dạng hiện có
            val isBold = spannable.getSpans(start, end, StyleSpan::class.java)
                .any { it.style == Typeface.BOLD }
            val isItalic = spannable.getSpans(start, end, StyleSpan::class.java)
                .any { it.style == Typeface.ITALIC }
            val isUnderline =
                spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
            val isStrikethrough =
                spannable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()

            // Lấy backgroundColor và textColor
            val backgroundColor =
                spannable.getSpans(start, end, BackgroundColorSpan::class.java)
                    .firstOrNull()?.backgroundColor ?: defaultBackgroundColor
            val textColor = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
                .firstOrNull()?.foregroundColor ?: defaultTextColor
            val textSize = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java)
                .firstOrNull()?.size ?: 18

            // Thêm vào danh sách TextSegment
            segments.add(
                TextSegment(
                    text,
                    isBold,
                    isItalic,
                    isUnderline,
                    isStrikethrough,
                    backgroundColor,
                    textColor,
                    textSize
                )
            )

            // Cập nhật start cho lần lặp tiếp theo
            start = end
            Log.d(
                "SpanInfo",
                "Text: $text, isBold: $isBold, isItalic: $isItalic, isUnderline: $isUnderline, isStrikethrough: $isStrikethrough, background: $backgroundColor, textcolor: $textColor, textsize: $textSize"
            )
        }
        return NoteContent(segments)
    }

    private fun noteContentToSpannable(noteContent: NoteContent): SpannableStringBuilder {
        val defautBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.transparent)
        val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)
        val spannable = SpannableStringBuilder()

        for (segment in noteContent.segments) {
            val start = spannable.length
            spannable.append(segment.text)
            val end = spannable.length

            segment.apply {
                if (isBold == true) spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isItalic == true) spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isUnderline == true) spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isStrikethrough == true) spannable.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                backgroundColor.let {
                    spannable.setSpan(
                        BackgroundColorSpan(it ?: defautBackgroundColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textColor.let {
                    spannable.setSpan(
                        ForegroundColorSpan(it ?: defautTextColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textSize.let {
                    spannable.setSpan(
                        AbsoluteSizeSpan(it ?: 18, true),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return spannable
    }

    private fun updateCurrentFormatFromCursorPosition() {
        val selectionStart = binding.edtNote.selectionStart
        val selectionEnd = binding.edtNote.selectionEnd
        if (selectionStart in 0..<selectionEnd) {
            val spannable = binding.edtNote.text as SpannableStringBuilder

            // Lấy các spans trong phạm vi vùng chọn
            val boldSpans = spannable.getSpans(selectionStart, selectionEnd, StyleSpan::class.java)
            val colorSpans =
                spannable.getSpans(selectionStart, selectionEnd, ForegroundColorSpan::class.java)
            val backgroundColorSpans =
                spannable.getSpans(selectionStart, selectionEnd, BackgroundColorSpan::class.java)
            val underlineSpans =
                spannable.getSpans(selectionStart, selectionEnd, UnderlineSpan::class.java)
            val strikethroughSpans =
                spannable.getSpans(selectionStart, selectionEnd, StrikethroughSpan::class.java)
            val sizeSpans =
                spannable.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)

            // Kiểm tra định dạng chung trong toàn bộ vùng chọn
            val isBold = boldSpans.all { it.style == Typeface.BOLD }
            val isItalic = boldSpans.all { it.style == Typeface.ITALIC }
            val isUnderline =
                underlineSpans.isNotEmpty() && underlineSpans.size == spannable.getSpans(
                    selectionStart,
                    selectionEnd,
                    UnderlineSpan::class.java
                ).size
            val isStrikethrough =
                strikethroughSpans.isNotEmpty() && strikethroughSpans.size == spannable.getSpans(
                    selectionStart,
                    selectionEnd,
                    StrikethroughSpan::class.java
                ).size

            val textColor = colorSpans.firstOrNull()?.foregroundColor ?: currentFormat.textColor
            val backgroundColor =
                backgroundColorSpans.firstOrNull()?.backgroundColor ?: currentFormat.backgroundColor
            val textSize = sizeSpans.firstOrNull()?.size ?: currentFormat.textSize

            // Cập nhật currentFormat với các thuộc tính lấy được
            currentFormat = currentFormat.copy(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isStrikethrough = isStrikethrough,
                textColor = textColor,
                backgroundColor = backgroundColor,
                textSize = textSize
            )
        } else {
            val spannable = binding.edtNote.text as SpannableStringBuilder

            // Lấy định dạng ngay tại vị trí con trỏ
            val boldSpans =
                spannable.getSpans(selectionStart, selectionStart, StyleSpan::class.java)
            val colorSpans =
                spannable.getSpans(selectionStart, selectionStart, ForegroundColorSpan::class.java)
            val backgroundColorSpans =
                spannable.getSpans(selectionStart, selectionStart, BackgroundColorSpan::class.java)
            val underlineSpans =
                spannable.getSpans(selectionStart, selectionStart, UnderlineSpan::class.java)
            val strikethroughSpans =
                spannable.getSpans(selectionStart, selectionStart, StrikethroughSpan::class.java)
            val sizeSpans =
                spannable.getSpans(selectionStart, selectionStart, AbsoluteSizeSpan::class.java)

            // Cập nhật currentFormat với các thuộc tính tìm thấy
            currentFormat = currentFormat.copy(
                isBold = boldSpans.isNotEmpty() && boldSpans[0].style == Typeface.BOLD,
                isItalic = boldSpans.isNotEmpty() && boldSpans[0].style == Typeface.ITALIC,
                isUnderline = underlineSpans.isNotEmpty(),
                isStrikethrough = strikethroughSpans.isNotEmpty(),
                textColor = colorSpans.firstOrNull()?.foregroundColor ?: currentFormat.textColor,
                backgroundColor = backgroundColorSpans.firstOrNull()?.backgroundColor
                    ?: currentFormat.backgroundColor,
                textSize = sizeSpans.firstOrNull()?.size ?: currentFormat.textSize
            )
        }

        Log.d("currentFormat", currentFormat.toString())
    }

    // Hàm kiểm tra định dạng của đoạn mới với đoạn cuối trong stack
    private fun isSameFormat(newFormat: TextFormat, lastFormat: TextFormat): Boolean {
        return newFormat.isBold == lastFormat.isBold &&
                newFormat.isItalic == lastFormat.isItalic &&
                newFormat.isUnderline == lastFormat.isUnderline &&
                newFormat.isStrikethrough == lastFormat.isStrikethrough &&
                newFormat.backgroundColor == lastFormat.backgroundColor &&
                newFormat.textColor == lastFormat.textColor &&
                newFormat.textSize == lastFormat.textSize
    }

    private fun getCurrentFormat(): TextFormat {
        // Lấy và trả về định dạng hiện tại từ trạng thái của người dùng
        return currentFormat
    }
}