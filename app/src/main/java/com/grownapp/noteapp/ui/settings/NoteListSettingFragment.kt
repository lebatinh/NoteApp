package com.grownapp.noteapp.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentNoteListSettingBinding

class NoteListSettingFragment : Fragment() {

    private var _binding: FragmentNoteListSettingBinding? = null

    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListSettingBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }
}