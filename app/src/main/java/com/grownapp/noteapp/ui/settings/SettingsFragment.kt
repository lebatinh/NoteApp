package com.grownapp.noteapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.menu_theme, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val rdgTheme = dialogView.findViewById<RadioGroup>(R.id.rdgTheme)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

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

}