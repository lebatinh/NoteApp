package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.FormatConverter
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.ColorPicker
import com.grownapp.noteapp.ui.TextFormat
import com.grownapp.noteapp.ui.TextSegment
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

    private var currentFormat = TextFormat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewModel
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

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

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        arguments?.let {
            noteId = NoteDetailFragmentArgs.fromBundle(it).id
            category = arguments?.getString("categoryName")
        }

        noteViewModel.getNoteById(noteId).observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.edtTitle.setText(it.title)
                val defautBackgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
                val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)
                val n = note.note

                if (!n.isNullOrEmpty()) {
                    val segments = FormatConverter().convertJsonToSegments(n)

                    val spannable = SpannableStringBuilder()
                    for (segment in segments) {
                        val start = spannable.length
                        spannable.append(segment.text)
                        val end = spannable.length

                        // Áp dụng định dạng
                        if (segment.isBold == true) spannable.setSpan(
                            StyleSpan(Typeface.BOLD),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (segment.isItalic == true) spannable.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (segment.isUnderline == true) spannable.setSpan(
                            UnderlineSpan(),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (segment.isStrikethrough == true) spannable.setSpan(
                            StrikethroughSpan(),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (segment.backgroundColor != defautBackgroundColor) spannable.setSpan(
                            BackgroundColorSpan(
                                currentFormat.backgroundColor
                            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (segment.textColor != defautTextColor) spannable.setSpan(
                            ForegroundColorSpan(
                                segment.textColor?: defautTextColor
                            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (segment.textSize != 18f) spannable.setSpan(
                            AbsoluteSizeSpan(currentFormat.textSize.toInt()),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // Gán nội dung với định dạng vào EditText
                    binding.edtNote.text = spannable
                }

            }
        }

        val isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
        binding.constraint.visibility = if (isShowFormattingBar) View.VISIBLE else View.GONE
        if (isShowFormattingBar) {
            showFormattingBar()
        }

        applyFormatting(binding.edtNote)
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
        val isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
//        menu.findItem(R.id.show_formatting_bar).isEnabled = !isShowFormattingBar
        //TODO
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
        val textSegments =
            FormatConverter().extractTextSegments()  // Hàm này sẽ chuyển formattedTextSegments thành List<TextSegment>
        val json = FormatConverter().convertSegmentsToJson(textSegments)

        val updateNote = Note().copy(
            noteId = noteId,
            title = binding.edtTitle.text.toString(),
            note = json
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

        btnRemoveColor.setOnClickListener {
            tvColor.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.backgroundPickColorDialog
                )
            )
        }
        "Opacity (${sbPercentOpacity.progress}%)".also { tvOpacity.text = it }

        sbPercentOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "Opacity (${progress}%)".also { tvOpacity.text = it }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        var colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
        var colorFillTextColor = ContextCompat.getColor(requireContext(), R.color.text)
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
                        textSize = 18f
                    }
                    colorView.setOnClickListener {
                        if (isBackground) {
                            tvColor.setBackgroundColor(Color.parseColor(color))

                            colorFillBackground = Color.parseColor(color)

                            for (i in 0 until gridlayoutColor.childCount) {
                                (gridlayoutColor.getChildAt(i) as? TextView)?.text = null
                            }

                            colorView.text = "+"

                            colorView.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.backgroundPickColorDialog
                                )
                            )
                        } else {
                            tvColor.setTextColor(Color.parseColor(color))

                            colorFillTextColor = Color.parseColor(color)
                            for (i in 0 until gridlayoutColor.childCount) {
                                (gridlayoutColor.getChildAt(i) as? TextView)?.text = null
                            }

                            colorView.text = "+"

                            colorView.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.backgroundPickColorDialog
                                )
                            )
                        }
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
                binding.fillColorText.setBackgroundColor(colorFillTextColor)
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

    private fun dialogPickTextSize(size: Float, colorChecked: Int, colorUncheck: Int) {
        val dialogView = layoutInflater.inflate(R.layout.pick_size, null)
        val tvTextSize = dialogView.findViewById<TextView>(R.id.tvTextSize)
        val sbTextSize = dialogView.findViewById<SeekBar>(R.id.sbTextSize)
        val btnSetDefault = dialogView.findViewById<Button>(R.id.btnSetDefault)
        val tvOK = dialogView.findViewById<TextView>(R.id.tvOK)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        var s = size
        sbTextSize.progress = currentFormat.textSize.toInt()
        "Text size ${currentFormat.textSize.toInt()}".also { tvTextSize.text = it }
        tvTextSize.textSize = currentFormat.textSize

        sbTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "Text size $progress".also { tvTextSize.text = it }
                tvTextSize.textSize = progress.toFloat()
                s = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        })
        btnSetDefault.setOnClickListener {
            tvTextSize.textSize = 18f
            tvTextSize.text = getString(R.string.text_size_18)
            sbTextSize.setProgress(18, true)
            currentFormat.textSize = 18f
        }
        tvOK.setOnClickListener {
            binding.fontSize.setBackgroundColor(if (s == 18f) colorUncheck else colorChecked)
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                startPos = editText.selectionStart
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val endPos = editText.selectionEnd
                    if (startPos < endPos) {
                    val newText = it.subSequence(startPos, endPos)
                    // Áp dụng định dạng hiện tại cho đoạn văn bản mới
                    formattedTextSegments.append(newText)
                    Log.d("textchangedlistener", "$s-$newText-$formattedTextSegments")
                    applyCurrentFormat(formattedTextSegments, startPos, endPos)

                    // Cập nhật lại EditText
                    editText.removeTextChangedListener(this)  // Tạm ngừng TextWatcher
                    editText.text =
                        formattedTextSegments     // Cập nhật lại EditText với định dạng đã áp dụng
                    editText.setSelection(formattedTextSegments.length) // Đặt con trỏ ở cuối văn bản
                    editText.addTextChangedListener(this)     // Kích hoạt lại TextWatcher

                    Log.d("textchangedlistener_later", "$s-$newText-$formattedTextSegments")
                }}
            }
        })
    }

    // Hàm mở rộng cho TextSegment để áp dụng định dạng vào Editable
    private fun applyCurrentFormat(text: SpannableStringBuilder, start: Int, end: Int) {
        val defautBackgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)

        if (start >= end) return

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
        if (currentFormat.textSize != 18f) text.setSpan(
            AbsoluteSizeSpan(currentFormat.textSize.toInt()),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun extractTextSegmentsFromSpannable(spannable: SpannableStringBuilder): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var start = 0

        while (start < spannable.length) {
            val end = spannable.nextSpanTransition(start, spannable.length, Any::class.java)
            val text = spannable.subSequence(start, end).toString()

            // Lấy các định dạng hiện có
            val isBold = spannable.getSpans(start, end, StyleSpan::class.java).any { it.style == Typeface.BOLD }
            val isItalic = spannable.getSpans(start, end, StyleSpan::class.java).any { it.style == Typeface.ITALIC }
            val isUnderline = spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
            val isStrikethrough = spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()

            val backgroundColor = spannable.getSpans(start, end, BackgroundColorSpan::class.java).firstOrNull()?.backgroundColor
            val textColor = spannable.getSpans(start, end, ForegroundColorSpan::class.java).firstOrNull()?.foregroundColor
            val textSize = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java).firstOrNull()?.size?.toFloat()

            // Thêm vào danh sách TextSegment
            segments.add(TextSegment(text, isBold, isItalic, isUnderline, isStrikethrough, backgroundColor, textColor, textSize))
            start = end
        }

        return segments
    }

    fun applySegmentsToSpannable(spannable: SpannableStringBuilder) {
        val segments = mutableListOf<TextSegment>()
        var start = 0
        for (segment in segments) {
            val end = start + segment.text.toString().length

            if (segment.isBold == true) spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (segment.isItalic == true) spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (segment.isUnderline == true) spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (segment.isStrikethrough == true) spannable.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            segment.backgroundColor?.let { spannable.setSpan(BackgroundColorSpan(it), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
            segment.textColor?.let { spannable.setSpan(ForegroundColorSpan(it), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
            segment.textSize?.let { spannable.setSpan(AbsoluteSizeSpan(it.toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }

            start = end // Cập nhật vị trí cho đoạn tiếp theo
        }
    }
}