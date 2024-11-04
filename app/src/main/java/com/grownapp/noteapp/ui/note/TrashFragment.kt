package com.grownapp.noteapp.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentTrashBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter

class TrashFragment : Fragment(), MenuProvider {

    private var _binding: FragmentTrashBinding? = null

    private val binding get() = _binding!!
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        noteAdapter = NoteAdapter(
            onClickNote = {
                val action = NoteFragmentDirections.actionNavNoteToNoteDetailFragment(it.noteId)
                findNavController().navigate(action)
            },
            onLongClickNote = { note ->

            },
            hideCreated = true

        )

        binding.rcvNoteTrash.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteTrash.adapter = noteAdapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.trash_more, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_more -> {}
        }
        return false
    }


}