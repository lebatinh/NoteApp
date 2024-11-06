package com.grownapp.noteapp.ui.note

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
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
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.grownapp.noteapp.MainActivity
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category
import com.grownapp.noteapp.ui.note.adapter.CategoryForNoteAdapter
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
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
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var hideCreated = MutableLiveData(true)

    private var isOnTrash = true
    private var isEditMode = false
    private lateinit var listNoteSelected: MutableList<Note>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel =
            ViewModelProvider(this)[NoteViewModel::class.java]
        categoryViewModel =
            ViewModelProvider(this)[CategoriesViewModel::class.java]

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        listNoteSelected = mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        insertFirstNote(sharedPreferences)
        val isVisible = sharedPreferences.getBoolean("isVisible", true)
        isOnTrash = sharedPreferences.getBoolean("isOnTrash", true)
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
            onLongClickNote = { note ->
                isEditMode = true
                startEditMode(note, true)
            },
            hideCreated = true,
            listNoteSelectedAdapter = listNoteSelected,
            updateCountCallback = { updateCountNoteSeleted() }
        )

        binding.rcvNote.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNote.adapter = noteAdapter

        noteViewModel.allNote.observe(viewLifecycleOwner) { notes ->
            notes.let {
                noteAdapter.updateListNote(notes)
            }
        }

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

        sortBy()

        return root
    }

    private fun updateCountNoteSeleted() {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val tvCountSeleted = toolbar?.findViewById<TextView>(R.id.tvCountSeleted)
        tvCountSeleted?.text = listNoteSelected.size.toString()
        Log.d("listNoteSelectedFragment", listNoteSelected.size.toString())
    }

    private fun startEditMode(note: Note, isVisible: Boolean) {
        isEditMode = isVisible
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
        val btnSearch = toolbar.findViewById<View>(R.id.item_search)
        val itemsort = toolbar.findViewById<View>(R.id.item_sort)
        val itemmore = toolbar.findViewById<View>(R.id.item_more)

        if (isVisible) {
            btnSearch?.visibility = View.GONE
            itemsort?.visibility = View.GONE
            itemmore?.visibility = View.GONE

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
                startEditMode(note, false)
                noteAdapter.exitEditMode()
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
                listNoteSelected.forEach {
                    if (isOnTrash) {
                        noteViewModel.pushInTrash(true, it.noteId)
                    } else {
                        noteViewModel.delete(it.noteId)
                    }
                }
            }
        } else {
            btnSearch?.visibility = View.VISIBLE
            itemsort?.visibility = View.VISIBLE
            itemmore?.visibility = View.VISIBLE

            val noteLayout = toolbar.findViewById<View>(R.id.custom_note_toolbar)
            if (noteLayout != null) {
                toolbar.removeView(noteLayout)
            }
        }
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

    // tạo note đầu tiên mặc định
    private fun insertFirstNote(sharedPreferences: SharedPreferences) {
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        val time = dateFormat.format(currentDateTime)

        if (isFirstRun) {
            val text = """
            Thank you for downloading Notepad Free. This is a welcome message.
            You can delete this message by clicking Delete button in the top right corner.
            You can revert any unwanted changes during note edition with the "Undo" and "Redo" buttons. Try to edit this text, and click the buttons in the top right corner.
            Please check the main menu for additional functions, like Help screen, backup functions, or settings. It can be opened with the button in the top left corner of the main screen.
            Have a nice day.
            ☺️
        """.trimIndent()

            val spannableText = SpannableStringBuilder(text)

            val noteContent = spannableToNoteContent(spannableText)

            val firstNote = Note(
                title = "Hi, how are you? (tap to open)",
                note = Gson().toJson(noteContent),
                timeCreate = time
            )

            // Lưu vào ViewModel
            noteViewModel.insertFirst(firstNote)

            // Cập nhật trạng thái isFirstRun
            with(sharedPreferences.edit()) {
                putBoolean("isFirstRun", false)
                apply()
            }
        }
    }

    private fun createNewNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            noteViewModel.insert(Note()) {}

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
        menu.clear()
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
                                noteAdapter.updateListNote(notes)
                            }
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let {
                            noteViewModel.search(it).observe(viewLifecycleOwner) { notes ->
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

            R.id.item_more_note -> {
                showPopupMenuMore()
                return true
            }
        }
        return false
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
                    startEditMode(Note(), true)
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
                    dialogPickColor()
                    true
                }

                else -> false
            }
        }

        // Hiển thị PopupMenu
        popupMenu.show()
    }

    private fun dialogPickColor() {
        val dialogView = layoutInflater.inflate(R.layout.pick_color, null)
        val tvColor = dialogView.findViewById<TextView>(R.id.tvColor)
        val gridlayoutColor = dialogView.findViewById<GridLayout>(R.id.gridlayoutColor)
        val btnRemoveColor = dialogView.findViewById<Button>(R.id.btnRemoveColor)
        val tvOK = dialogView.findViewById<TextView>(R.id.tvOK)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        var colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
        tvColor.setBackgroundColor(colorFillBackground)

        btnRemoveColor.setOnClickListener {
            colorFillBackground = ContextCompat.getColor(requireContext(), R.color.transparent)
            tvColor.setBackgroundColor(colorFillBackground)
        }

        dialogView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val dialogWidth = dialogView.width
                val itemWidth = dialogWidth / 6
                gridlayoutColor.removeAllViews()

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

                        // Cập nhật `tvColor` với màu được chọn
                        tvColor.setBackgroundColor(colorFillBackground)

                        for (i in 0 until gridlayoutColor.childCount) {
                            (gridlayoutColor.getChildAt(i) as? TextView)?.text = null
                        }

                        "+".also { colorView.text = it }

                        colorView.setBackgroundColor(
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
            listNoteSelected.forEach {
                noteViewModel.updateBackgroundColor(it.noteId, colorFillBackground)
            }
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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
        }
        dialog.show()
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
                    hideCreated.postValue(true)
                }

                R.id.rdbEditOldest -> {
                    editor.putString("sort", "editoldest")
                    hideCreated.postValue(true)
                }

                R.id.rdbA_Z -> {
                    editor.putString("sort", "a_z")
                    hideCreated.postValue(true)
                }

                R.id.rdbZ_A -> {
                    editor.putString("sort", "z_a")
                    hideCreated.postValue(true)
                }

                R.id.rdbCreateNewest -> {
                    editor.putString("sort", "createnewest")
                    hideCreated.postValue(false)
                }

                R.id.rdbCreateOldest -> {
                    editor.putString("sort", "createoldest")
                    hideCreated.postValue(false)
                }

                R.id.rdbColor -> {
                    editor.putString("sort", "color")
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

    private fun sortBy() {
        val sort = sharedPreferences.getString("sort", null)
        val sortObserver = { notes: List<Note> -> noteAdapter.updateListNote(notes) }

        when (sort) {
            "editnewest" -> noteViewModel.sortedByUpdatedTimeDesc()
                .observe(viewLifecycleOwner, sortObserver)

            "editoldest" -> noteViewModel.sortedByUpdatedTimeAsc()
                .observe(viewLifecycleOwner, sortObserver)

            "a_z" -> noteViewModel.sortedByTitleAsc().observe(viewLifecycleOwner, sortObserver)
            "z_a" -> noteViewModel.sortedByTitleDesc().observe(viewLifecycleOwner, sortObserver)
            "createnewest" -> noteViewModel.sortedByCreatedTimeDesc()
                .observe(viewLifecycleOwner, sortObserver)

            "createoldest" -> noteViewModel.sortedByCreatedTimeAsc()
                .observe(viewLifecycleOwner, sortObserver)

            "color" -> {
                // Xử lý tùy chọn "color"
            }
        }
    }

    private fun spannableToNoteContent(spannable: SpannableStringBuilder): NoteContent {
        val defaultBackgroundColor = ContextCompat.getColor(requireContext(), R.color.transparent)
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
            val isUnderline = spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
            val isStrikethrough =
                spannable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()

            // Lấy backgroundColor và textColor
            val backgroundColor = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
                .firstOrNull()?.backgroundColor ?: defaultBackgroundColor
            val textColor = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
                .firstOrNull()?.foregroundColor ?: defaultTextColor
            val textSize = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java)
                .firstOrNull()?.size?.toFloat() ?: 18f

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
}