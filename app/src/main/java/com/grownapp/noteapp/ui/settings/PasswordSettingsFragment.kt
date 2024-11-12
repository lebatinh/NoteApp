package com.grownapp.noteapp.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentPasswordSettingsBinding

class PasswordSettingsFragment : Fragment() {

    private var _binding: FragmentPasswordSettingsBinding? = null

    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val password = sharedPreferences.getString("password", null)

        val thumbColorStateList =
            ContextCompat.getColorStateList(requireContext(), R.color.switch_thumb_color)
        val thumbColorStateListDisable =
            ContextCompat.getColorStateList(requireContext(), R.color.switch_thumb_color_disable)

        val textColor = ContextCompat.getColorStateList(requireContext(), R.color.text_color)
        val textDesColor = ContextCompat.getColorStateList(requireContext(), R.color.text_des_color)

        binding.swShowLock.thumbTintList = if (!password.isNullOrEmpty()) thumbColorStateList else thumbColorStateListDisable
        binding.swBiometricsUnlock.thumbTintList = if (!password.isNullOrEmpty()) thumbColorStateList else thumbColorStateListDisable

        binding.tvRemovePass.isEnabled = !password.isNullOrEmpty()
        binding.tvRemovePass.setTextColor(textColor)
        binding.tvUnlockTime.isEnabled = !password.isNullOrEmpty()
        binding.tvUnlockTime.setTextColor(textColor)
        binding.tvShowLock.isEnabled = !password.isNullOrEmpty()
        binding.tvShowLock.setTextColor(textColor)
        binding.tvBiometricsUnlock.isEnabled = !password.isNullOrEmpty()
        binding.tvBiometricsUnlock.setTextColor(textColor)

        binding.tvRemovePassDes.isEnabled = !password.isNullOrEmpty()
        binding.tvRemovePassDes.setTextColor(textDesColor)
        binding.tvUnlockTimeDes.isEnabled = !password.isNullOrEmpty()
        binding.tvUnlockTimeDes.setTextColor(textDesColor)
        binding.tvShowLockDes.isEnabled = !password.isNullOrEmpty()
        binding.tvShowLockDes.setTextColor(textDesColor)
        binding.tvBiometricsUnlockDes.isEnabled = !password.isNullOrEmpty()
        binding.tvBiometricsUnlockDes.setTextColor(textDesColor)

        return root
    }
}