package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
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
import androidx.core.text.buildSpannedString
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.ColorPicker
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

    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrikethrough = false
    private var backgroundColor: Int? = null
    private var textColor: Int? = null
    private var fontSize = 18f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewModel
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        backgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        textColor = ContextCompat.getColor(requireContext(), R.color.text)
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

                if (!it.note?.toList().isNullOrEmpty()) {
                    val textSegments: List<TextSegment> =
                        Gson().fromJson(it.note, Array<TextSegment>::class.java).toList()

                    val spannableString = buildSpannedString {
                        textSegments.forEach { segment ->
                            append(segment.text)
                            segment.applyFormattingToSpan(
                                this,
                                length - (segment.text?.length ?: 0),
                                length
                            )
                        }
                    }
                    binding.edtNote.text =
                        Editable.Factory.getInstance().newEditable(spannableString)
                } else {
                    binding.edtNote.text = Editable.Factory.getInstance().newEditable("")
                }

            }
        }

        val isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
        binding.constraint.visibility = if (isShowFormattingBar) View.VISIBLE else View.GONE
        if (isShowFormattingBar) {
            showFormattingBar()
        }

        binding.edtNote.applyFormatting()
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
        val updateNote = Note().copy(
            noteId = noteId,
            title = binding.edtTitle.text.toString(),
            note = saveFormattedText(binding.edtNote)
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
            isBold = !isBold
            binding.bold.setBackgroundColor(if (!isBold) colorUncheck else colorChecked)
            Log.d("isBold", isBold.toString())
        }

        binding.italic.setOnClickListener {
            isItalic = !isItalic
            binding.italic.setBackgroundColor(if (!isItalic) colorUncheck else colorChecked)
            Log.d("isItalic", isItalic.toString())
        }

        binding.underline.setOnClickListener {
            isUnderline = !isUnderline
            binding.underline.setBackgroundColor(if (!isUnderline) colorUncheck else colorChecked)
            Log.d("isUnderline", isUnderline.toString())
        }

        binding.strikethrough.setOnClickListener {
            isStrikethrough = !isStrikethrough
            binding.strikethrough.setBackgroundColor(if (!isStrikethrough) colorUncheck else colorChecked)
            Log.d("isStrikethrough", isStrikethrough.toString())
        }

        binding.fillColorBackground.setOnClickListener {
            dialogPickColor(true)
        }

        binding.fillColorText.setOnClickListener {
            dialogPickColor(false)
        }

        binding.fontSize.setOnClickListener {
            dialogPickTextSize(fontSize, colorChecked, colorUncheck)
        }
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
        sbTextSize.progress = fontSize.toInt()
        "Text size ${fontSize.toInt()}".also { tvTextSize.text = it }
        tvTextSize.textSize = fontSize

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
            fontSize = 18f
        }
        tvOK.setOnClickListener {
            binding.fontSize.setBackgroundColor(if (s == 18f) colorUncheck else colorChecked)
            dialog.dismiss()
            fontSize = s
            Log.d("fontSize", fontSize.toString())
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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
                backgroundColor = colorFillBackground
                Log.d("backgroundColor", colorFillBackground.toString())
            } else {
                binding.fillColorText.setBackgroundColor(colorFillTextColor)
                textColor = colorFillTextColor
                Log.d("textColor", colorFillTextColor.toString())
            }
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun EditText.applyFormatting() {
        var startFormat = 0
        var endFormat: Int
        var currentTextFormat = getCurrentTextFormat()
        val listTextFormat = mutableListOf<TextSegment>()

        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.isEmpty()) return

                endFormat = editable.length

                val newTextFormat = getCurrentTextFormat()

                // Nếu định dạng thay đổi hoặc thêm chữ mới, lưu đoạn văn bản cũ với định dạng cũ
                if (newTextFormat != currentTextFormat || startFormat == endFormat) {
                    // Đặt định dạng cho đoạn văn bản cũ
                    if (startFormat < endFormat) {
                        applyTextFormatting(editable, currentTextFormat, startFormat, endFormat)
                        listTextFormat.add(
                            TextSegment(
                                text = editable.subSequence(startFormat, endFormat).toString(),
                                isBold = currentTextFormat.isBold,
                                isItalic = currentTextFormat.isItalic,
                                isUnderline = currentTextFormat.isUnderline,
                                isStrikethrough = currentTextFormat.isStrikethrough,
                                backgroundColor = currentTextFormat.backgroundColor,
                                textColor = currentTextFormat.textColor,
                                fontSize = currentTextFormat.fontSize
                            )
                        )
                    }

                    // Cập nhật `startFormat` và `currentTextFormat` cho đoạn mới
                    startFormat = endFormat
                    currentTextFormat = newTextFormat
                }

                if (startFormat <= endFormat) {
                    // Áp dụng định dạng cho đoạn văn bản mới được thêm
                    applyTextFormatting(editable, currentTextFormat, startFormat, endFormat)
                }
            }

        })
    }

    fun applyTextFormatting(editable: Editable, textFormat: TextSegment, start: Int, end: Int) {
        if (start >= end) return
        val defautBackgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)
        if (textFormat.isBold == true) {
            editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (textFormat.isItalic == true) {
            editable.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (textFormat.isUnderline == true) {
            editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (textFormat.isStrikethrough == true) {
            editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (textFormat.backgroundColor != defautBackgroundColor) {
            editable.setSpan(
                BackgroundColorSpan(textFormat.backgroundColor!!),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (textFormat.textColor != defautTextColor) {
            editable.setSpan(
                ForegroundColorSpan(textFormat.textColor!!),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (textFormat.fontSize != 18f) {
            editable.setSpan(
                AbsoluteSizeSpan(textFormat.fontSize.toInt()),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // Hàm mở rộng cho TextSegment để áp dụng định dạng vào Editable
    private fun TextSegment.applyFormattingToSpan(editable: Editable, start: Int, end: Int) {
        val defautBackgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        val defautTextColor = ContextCompat.getColor(requireContext(), R.color.text)

        if (start >= end) return

        if (isBold == true) editable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (isItalic == true) editable.setSpan(
            StyleSpan(Typeface.ITALIC),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (isUnderline == true) editable.setSpan(
            UnderlineSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (isStrikethrough == true) editable.setSpan(
            StrikethroughSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (backgroundColor != defautBackgroundColor) editable.setSpan(
            BackgroundColorSpan(
                backgroundColor ?: defautBackgroundColor
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (textColor != defautTextColor) editable.setSpan(
            ForegroundColorSpan(
                textColor ?: defautTextColor
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (fontSize != 18f) editable.setSpan(
            AbsoluteSizeSpan(fontSize.toInt()),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun saveFormattedText(editText: EditText): String? {
        val textSegments = mutableListOf<TextSegment>()
        val editable = editText.text

        // Phân tích văn bản trong EditText và lấy ra các đoạn với định dạng tương ứng
        var start = 0
        while (start < editable.length) {
            val end =
                editable.nextSpanTransition(start, editable.length, CharacterStyle::class.java)
            val segmentText = editable.subSequence(start, end).toString()

            // Kiểm tra từng kiểu định dạng
            val bold = editable.getSpans(start, end, StyleSpan::class.java)
                .any { it.style == Typeface.BOLD }
            val italic = editable.getSpans(start, end, StyleSpan::class.java)
                .any { it.style == Typeface.ITALIC }
            val underline = editable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
            val strikethrough =
                editable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()

            // Lấy màu nền và màu chữ (nếu có)
            val background = editable.getSpans(start, end, BackgroundColorSpan::class.java)
                .firstOrNull()?.backgroundColor
            val textcolor = editable.getSpans(start, end, ForegroundColorSpan::class.java)
                .firstOrNull()?.foregroundColor

            // Giả sử fontSize là một span đã được áp dụng (nếu có)
            val size =
                editable.getSpans(start, end, AbsoluteSizeSpan::class.java).firstOrNull()?.size

            textSegments.add(
                TextSegment(
                    text = segmentText,
                    isBold = bold,
                    isItalic = italic,
                    isUnderline = underline,
                    isStrikethrough = strikethrough,
                    backgroundColor = background ?: backgroundColor,
                    textColor = textcolor ?: textColor,
                    fontSize = size?.toFloat() ?: 18f
                )
            )

            start = end
        }

        // Chuyển danh sách `TextSegment` thành chuỗi JSON và lưu
        val json = Gson().toJson(textSegments)
        return json
    }

    private fun getCurrentTextFormat(): TextSegment {
        return TextSegment().copy(
            text = null,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            isStrikethrough = isStrikethrough,
            backgroundColor = backgroundColor,
            textColor = textColor,
            fontSize = fontSize
        )
    }
}