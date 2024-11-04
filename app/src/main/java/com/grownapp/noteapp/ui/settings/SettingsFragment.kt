package com.grownapp.noteapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.constraintTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.tvPasswordSetting.setOnClickListener {
            val action = SettingsFragmentDirections.actionNavSettingsToPasswordSettingsFragment()
            findNavController().navigate(action)
        }

        return root
    }

    private fun showThemeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.menu_theme, null)

        val rdgTheme = dialogView.findViewById<RadioGroup>(R.id.rdgTheme)
        val buttonCancel = dialogView.findViewById<TextView>(R.id.buttonCancel)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        rdgTheme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rdbLight -> {
                }

                R.id.rdbSolarized -> {
                }

                R.id.rdbWhite -> {
                }

                R.id.rdbSolarizeDark -> {
                }

                R.id.rdbDark -> {
                }

                R.id.rdbSystem -> {
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}