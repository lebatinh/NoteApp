package com.grownapp.noteapp.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
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
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteDetailBinding
import com.grownapp.noteapp.ui.Format
import com.grownapp.noteapp.ui.FormattedText
import com.grownapp.noteapp.ui.TextSegment
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import org.json.JSONArray
import org.json.JSONObject

class NoteDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteDetailBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var sharedPreferences: SharedPreferences

    private var noteId: Int = 0
    private var category: String? = null

    private val formattedText = FormattedText()
    private var currentFormat = Format()

    private var isUpdating = false
    private var previousFormat = currentFormat.copy()
    private var startPos = 0
    private var endPos = 0
    private val formattedSegments = mutableListOf<TextSegment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewModel
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        currentFormat =
            currentFormat.copy(
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
                formattedText.fromJSON(it.note ?: "")
                binding.edtNote.text = formattedText.toSpannable()
            }
        }

        val isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
        binding.constraint.visibility = if (isShowFormattingBar) View.VISIBLE else View.GONE
        if (isShowFormattingBar) {
            showFormattingBar()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentLifecycle", "Gán TextWatcher cho edtNote")

        Log.d("startPos", "addTextChangedListener : $startPos")
        Log.d("endPos", "addTextChangedListener : $endPos")

        binding.edtNote.addTextChangedListener(object : TextWatcher {
            var lastPos = 0
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastPos = start + count
                Log.d("start", "beforeTextChanged : $startPos / $start")
                Log.d("end", "beforeTextChanged : $endPos + $count")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (start + count > lastPos) {
                    // Chỉ áp dụng định dạng cho văn bản mới (phần được thêm vào)
                    val spannable = s as SpannableStringBuilder
                    formattedText.applyFormat(spannable, currentFormat, start, start + count)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                lastPos = binding.edtNote.selectionEnd
            }
        })
    }

    private fun saveFormattedTextToJson(): String {
        // Lưu tất cả các segment đã format thành JSON
        val jsonArray = JSONArray()
        formattedSegments.forEach { segment ->
            val jsonObject = JSONObject().apply {
                put("text", segment.text)
                put("format", JSONObject().apply {
                    put("isBold", segment.format.isBold)
                    put("isItalic", segment.format.isItalic)
                    put("isUnderline", segment.format.isUnderline)
                    put("isStrikethrough", segment.format.isStrikethrough)
                    put("backgroundColor", segment.format.backgroundColor)
                    put("textColor", segment.format.textColor)
                    put("fontSize", segment.format.fontSize)
                })
            }
            jsonArray.put(jsonObject)
        }
        val formattedTextJson = jsonArray.toString()
        return formattedTextJson
        Log.d("FormattedText", "Saved JSON: $formattedTextJson")
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_note_detail, menu)
        val isShowFormattingBar = sharedPreferences.getBoolean("isShowFormattingBar", false)
//        menu.findItem(R.id.show_formatting_bar).isEnabled = !isShowFormattingBar
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
            note = saveFormattedTextToJson()
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
            currentFormat = currentFormat.copy(isBold = !currentFormat.isBold!!)
            binding.bold.setBackgroundColor(if (!currentFormat.isBold!!) colorUncheck else colorChecked)
            Log.d("isBold", currentFormat.isBold.toString())
        }

        binding.italic.setOnClickListener {
            currentFormat = currentFormat.copy(isItalic = !currentFormat.isItalic!!)
            binding.italic.setBackgroundColor(if (!currentFormat.isItalic!!) colorUncheck else colorChecked)
            Log.d("isItalic", currentFormat.isItalic.toString())
        }

        binding.underline.setOnClickListener {
            currentFormat = currentFormat.copy(isUnderline = !currentFormat.isUnderline!!)
            binding.underline.setBackgroundColor(if (!currentFormat.isUnderline!!) colorUncheck else colorChecked)
            Log.d("isUnderline", currentFormat.isUnderline.toString())
        }

        binding.strikethrough.setOnClickListener {
            currentFormat = currentFormat.copy(isStrikethrough = !currentFormat.isStrikethrough!!)
            binding.strikethrough.setBackgroundColor(if (!currentFormat.isStrikethrough!!) colorUncheck else colorChecked)
            Log.d("isStrikethrough", currentFormat.isStrikethrough.toString())
        }

        binding.fillColorBackground.setOnClickListener {
            dialogPickColor(true)
        }

        binding.fillColorText.setOnClickListener {
            dialogPickColor(false)
        }

        binding.fontSize.setOnClickListener {
            dialogPickTextSize(currentFormat.fontSize!!, colorChecked, colorUncheck)
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
        sbTextSize.progress = currentFormat.fontSize?.toInt() ?: 18
        tvTextSize.text = "Text size ${currentFormat.fontSize?.toInt()}"
        tvTextSize.textSize = currentFormat.fontSize!!

        sbTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTextSize.text = "Text size $progress"
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
            currentFormat = currentFormat.copy(fontSize = 18f)
        }
        tvOK.setOnClickListener {
            binding.fontSize.setBackgroundColor(if (s == 18f) colorUncheck else colorChecked)
            dialog.dismiss()
            currentFormat = currentFormat.copy(fontSize = s)
            Log.d("fontSize", currentFormat.fontSize.toString())
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
        tvOpacity.text = "Opacity (${sbPercentOpacity.progress}%)"

        sbPercentOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvOpacity.text = "Opacity (${progress}%)"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        val colors = arrayOf(
            "#000000",
            "#444444",
            "#888888",
            "#CCCCCC",
            "#FFFFFF",
            "#FF0000",
            "#00FF00",
            "#0000FF",
            "#FFFF00",
            "#00FFFF",
            "#FF00FF",
            "#FF0000",
            "#FF1D00",
            "#FF3A00",
            "#FF5700",
            "#FF7300",
            "#FF9000",
            "#FFAD00",
            "#FFCA00",
            "#FFE700",
            "#FAFF00",
            "#DDFF00",
            "#C0FF00",
            "#A4FF00",
            "#87FF00",
            "#6AFF00",
            "#4DFF00",
            "#30FF00",
            "#13FF00",
            "#00FF0A",
            "#00FF26",
            "#00FF43",
            "#00FF60",
            "#00FF7D",
            "#00FF9A",
            "#00FFB7",
            "#00FFD4",
            "#00FFF1",
            "#00F1FF",
            "#00D4FF",
            "#00B7FF",
            "#009AFF",
            "#007DFF",
            "#0060FF",
            "#0043FF",
            "#0026FF",
            "#000AFF",
            "#1300FF",
            "#3000FF",
            "#4D00FF",
            "#6A00FF",
            "#8700FF",
            "#A400FF",
            "#C000FF",
            "#DD00FF",
            "#FA00FF",
            "#FF00E7",
            "#FF00CA",
            "#FF00AD",
            "#FF0090",
            "#FF0073",
            "#FF0057",
            "#FF003A",
            "#FF001D"
        )

        var colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
        var colorFillTextColor = ContextCompat.getColor(requireContext(), R.color.text)
        dialogView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val dialogWidth = dialogView.width
                val itemWidth = dialogWidth / 8
                gridlayoutColor.removeAllViews()

                for (color in colors) {
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
}

// TODO: lỗi cập nhật kiểu chữ tiếp theo không đúng, phải ấn cách mới cập nhật đúng