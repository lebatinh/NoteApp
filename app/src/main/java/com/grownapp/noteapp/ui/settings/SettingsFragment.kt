package com.grownapp.noteapp.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

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
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.constraintTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.tvPasswordSetting.setOnClickListener {
            val action = SettingsFragmentDirections.actionNavSettingsToPasswordSettingsFragment()
            findNavController().navigate(action)
        }

        val editor = sharedPreferences.edit()

        val isOnTrash = sharedPreferences.getBoolean("isOnTrash", true)
        binding.swTrash.isChecked = isOnTrash

        binding.constraintTrash.setOnClickListener {
            val currentIsOnTrash = sharedPreferences.getBoolean("isOnTrash", true)
            editor.putBoolean("isOnTrash", !currentIsOnTrash).apply()
            binding.swTrash.isChecked = !currentIsOnTrash
        }
        binding.swTrash.setOnCheckedChangeListener { _, checked ->
            editor.putBoolean("isOnTrash", checked).apply()
            binding.swTrash.isChecked = checked
        }

        val isDiagnostic = sharedPreferences.getBoolean("isDiagnostic", false)
        binding.swDiagnostic.isChecked = isDiagnostic

        val thumbColorStateList = ContextCompat.getColorStateList(requireContext(), R.color.switch_thumb_color)
        val thumbColorStateListDisable = ContextCompat.getColorStateList(requireContext(), R.color.switch_thumb_color_disable)

        val textColor = ContextCompat.getColorStateList(requireContext(), R.color.text_color)
        val textDesColor = ContextCompat.getColorStateList(requireContext(), R.color.text_des_color)

        binding.constraintDiagnostic.setOnClickListener {
            val currentIsDiagnostic = sharedPreferences.getBoolean("isDiagnostic", false)
            editor.putBoolean("isDiagnostic", !currentIsDiagnostic).apply()
            binding.swDiagnostic.isChecked = !currentIsDiagnostic
            binding.constraintHideNoteTitleDiagnostic.isEnabled = !currentIsDiagnostic
            binding.swHideNoteTitleDiagnostic.thumbTintList = if (!currentIsDiagnostic) thumbColorStateList else thumbColorStateListDisable

            binding.tvHideNoteTitleDiagnostic.isEnabled = !currentIsDiagnostic
            binding.tvHideNoteTitleDiagnosticDes.isEnabled = !currentIsDiagnostic
            binding.tvHideNoteTitleDiagnostic.setTextColor(textColor)
            binding.tvHideNoteTitleDiagnosticDes.setTextColor(textDesColor)
        }
        binding.swDiagnostic.setOnCheckedChangeListener { _, checked ->
            editor.putBoolean("isDiagnostic", checked).apply()
            binding.swDiagnostic.isChecked = checked
            binding.constraintHideNoteTitleDiagnostic.isEnabled = checked

            binding.swHideNoteTitleDiagnostic.thumbTintList = if (checked) thumbColorStateList else thumbColorStateListDisable

            binding.tvHideNoteTitleDiagnostic.isEnabled = checked
            binding.tvHideNoteTitleDiagnosticDes.isEnabled = checked
            binding.tvHideNoteTitleDiagnostic.setTextColor(textColor)
            binding.tvHideNoteTitleDiagnosticDes.setTextColor(textDesColor)
        }

        val isHideNoteTitleDiagnostic =
            sharedPreferences.getBoolean("isHideNoteTitleDiagnostic", true)
        binding.swHideNoteTitleDiagnostic.isChecked = isHideNoteTitleDiagnostic
        binding.swHideNoteTitleDiagnostic.thumbTintList = if (isDiagnostic) thumbColorStateList else thumbColorStateListDisable

        binding.tvHideNoteTitleDiagnostic.isEnabled = isDiagnostic
        binding.tvHideNoteTitleDiagnosticDes.isEnabled = isDiagnostic
        binding.tvHideNoteTitleDiagnostic.setTextColor(textColor)
        binding.tvHideNoteTitleDiagnosticDes.setTextColor(textDesColor)

        binding.constraintHideNoteTitleDiagnostic.setOnClickListener {
            if (binding.swDiagnostic.isChecked){
                val currentIsHideNoteTitleDiagnostic =
                    sharedPreferences.getBoolean("isHideNoteTitleDiagnostic", true)
                editor.putBoolean("isHideNoteTitleDiagnostic", !currentIsHideNoteTitleDiagnostic)
                    .apply()
                binding.swHideNoteTitleDiagnostic.isChecked = !currentIsHideNoteTitleDiagnostic

            }
        }

        var isBackup =
            sharedPreferences.getBoolean("isBackup", true)
        binding.swBackup.isChecked = isBackup

        binding.constraintBackup.setOnClickListener {
            isBackup = !isBackup
            editor.putBoolean("isBackup", isBackup).apply()
            binding.swBackup.isChecked = isBackup
        }

        binding.swBackup.setOnCheckedChangeListener { _, checked ->
            isBackup = checked
            editor.putBoolean("isBackup", isBackup).apply()
            binding.swBackup.isChecked = isBackup
        }

        binding.tvNoteList.setOnClickListener {
            val action = SettingsFragmentDirections.actionNavSettingsToNoteListSettingFragment()
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