package com.grownapp.noteapp.ui.note

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import kotlinx.coroutines.launch
import kotlin.math.atan2

class NoteFragment : Fragment(), MenuProvider {

    private var _binding: FragmentNoteBinding? = null

    private val binding get() = _binding!!

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var hideCreated = MutableLiveData(true)

    private var onLongClick = MutableLiveData(false)
    private var noteSelected = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel =
            ViewModelProvider(this)[NoteViewModel::class.java]

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        noteViewModel.insertFirstNote(sharedPreferences)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val isVisible = sharedPreferences.getBoolean("isVisible", true)
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
                if (onLongClick.value == true) {
                    noteSelected.add(note)
                    onLongClick.value = true
//                    updateToolbarTitle()
                } else {
                    onLongClick.value = true
                    noteSelected.clear()
                    noteSelected.add(note)
//                    updateToolbarTitle()
                }
            },
            hideCreated = true
        )

        binding.rcvNote.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNote.adapter = noteAdapter

        hideCreated.observe(viewLifecycleOwner) {
            noteAdapter.updateHideCreated(it)
        }
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

    private fun updateToolbarTitle() {
        val toolbar =
            requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.title = "${noteSelected.size}"
        toolbar.setNavigationIcon(R.drawable.back)
        toolbar.setNavigationOnClickListener {
//            exitLongClickMode()
        }
    }

//    //TODO: Xem lại thanh toolbar cập nhật cho đúng
//    private fun exitLongClickMode() {
//        onLongClick.value = false
//        noteSelected.clear()
//        val toolbar =
//            requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
//        toolbar.title = getString(R.string.app_name)
//        toolbar.navigationIcon = Drawable.createFromPath(R.drawable.menu.toString())
//
//        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
////        val drawerToggle = ActionBarDrawerToggle(
////            requireActivity(),
////            requireActivity().findViewById(R.id.drawer_layout),
////            toolbar,
////            R.string.drawer_open,
////            R.string.drawer_close
////        )
////        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)?.addDrawerListener(drawerToggle)
////        drawerToggle.syncState()
//
//        requireActivity().invalidateOptionsMenu()
//    }

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
//        menu.clear()
//        if (onLongClick.value == true) {
//            menuInflater.inflate(R.menu.menu_note_longclick, menu)
//            updateToolbarTitle()
//        } else {
            menuInflater.inflate(R.menu.menu_note, menu)
//            exitLongClickMode()
//        }
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

            R.id.item_more -> {
                showPopupMenuMore()
                return true
            }

            R.id.item_select_all -> {
                return true
            }

            R.id.item_delete -> {
//                noteViewModel.delete(note)
//                exitLongClickMode()
                return true
            }

            R.id.item_more_click -> {
                return true
            }
        }
        return false
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
                // Xử lý tùy chọn "color" nếu cần
            }
        }
    }
}