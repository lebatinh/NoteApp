package com.grownapp.noteapp.ui.categories

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.grownapp.noteapp.R
import com.grownapp.noteapp.databinding.FragmentCategoriesBinding
import com.grownapp.noteapp.ui.categories.adapter.CategoriesAdapter
import com.grownapp.noteapp.ui.categories.dao.Category

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null

    private val binding get() = _binding!!

    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var categoriesViewModel: CategoriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        categoriesViewModel =
            ViewModelProvider(this)[CategoriesViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        categoriesAdapter = CategoriesAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = {
                showDeleteDialog(it) }
        )

        binding.rcvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvCategories.adapter = categoriesAdapter

        categoriesViewModel.allCategory.observe(viewLifecycleOwner) { category ->
            category.let {
                categoriesAdapter.updateListCategory(it)
            }
        }
        binding.tvAddCategories.setOnClickListener {
            val name = binding.edtCategories.text
            if (name.toString() != "" && name.toString().isNotEmpty() && name.toString()
                    .isNotBlank()
            ) {
                categoriesViewModel.insertCategory(Category(name = name.toString().trim()))
                name.clear()
            }
        }

        return root
    }

    private fun showDeleteDialog(category: Category) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.delete_category, category.name))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                categoriesViewModel.deleteCategory(category)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showEditDialog(category: Category) {
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_category_dialog, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val edtCategoryEdit = dialogView.findViewById<EditText>(R.id.edtCategoryEdit)
        edtCategoryEdit.setText(category.name)

        val btnOK = dialogView.findViewById<Button>(R.id.btnOK)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnOK.setOnClickListener {
            val name = edtCategoryEdit.text.toString().trim()
            if (name != "" && name.isNotBlank() && name.isNotEmpty()) {
                categoriesViewModel.updateCategory(
                    category.copy(name = name)
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}