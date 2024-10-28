package com.grownapp.noteapp.ui.privacy_policy

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.grownapp.noteapp.R

class PrivacyPolicyFragment : Fragment() {

    companion object {
        fun newInstance() = PrivacyPolicyFragment()
    }

    private val viewModel: PrivacyPolicyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false)
    }
}