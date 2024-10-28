package com.grownapp.noteapp.ui.note

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grownapp.noteapp.databinding.FragmentNoteListBinding
import com.grownapp.noteapp.ui.note.adapter.NoteAdapter
import com.grownapp.noteapp.ui.note.dao.Note
import com.grownapp.noteapp.ui.note_category.NoteCategoryCrossRef
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null

    private val binding get() = _binding!!

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NoteAdapter(onClickNote = {
            val action =
                NoteListFragmentDirections.actionNoteListFragmentToNoteDetailFragment(it.noteId)
            findNavController().navigate(action)
        }, onDelete = {
            viewModel.delete(it)
        })

        binding.rcvNoteList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvNoteList.adapter = adapter

        val categoryName = arguments?.getString("name")
        val categoryId = arguments?.getString("id")?.toInt()
        (activity as AppCompatActivity).supportActionBar?.subtitle = categoryName ?: "Uncategorized"

        Toast.makeText(requireContext(), "$categoryId - $categoryName", Toast.LENGTH_SHORT).show()
        if (categoryId != null) {
            viewModel.getNotesByCategory(categoryId).observe(viewLifecycleOwner) { notes ->
                val note = notes.map { it.note }
                adapter.updateListNote(note)
            }
        }
        else{
            viewModel.allNoteWithoutCategory.observe(viewLifecycleOwner){ notes ->
                adapter.updateListNote(notes)
            }
        }

        binding.fab.setOnClickListener {
            addNote(categoryId)
            sharedPreferences.edit().putBoolean("isVisible", false).apply()
        }
    }

    fun addNote(categoryId: Int?) {
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        val time = dateFormat.format(currentDateTime)

        val note = Note(
            time = time
        )
        viewModel.insert(note){ noteId ->
            if (categoryId != null){
                val noteCategoryCrossRef = NoteCategoryCrossRef(noteId = noteId.toInt(), categoryId = categoryId)
                viewModel.insertNoteCategoryCrossRef(noteCategoryCrossRef)
            }
        }


    }
}