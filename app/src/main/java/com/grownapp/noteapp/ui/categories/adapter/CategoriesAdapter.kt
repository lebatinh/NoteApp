package com.grownapp.noteapp.ui.categories.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.R
import com.grownapp.noteapp.ui.categories.dao.Category

class CategoriesAdapter(
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoriesViewHolder>() {
    private var categories: List<Category> = emptyList()

    class CategoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategories: TextView = itemView.findViewById(R.id.tvCategories)
        val imgEditCategoryItem: ImageView = itemView.findViewById(R.id.imgEditCategoryItem)
        val imgDeleteCategoryItem: ImageView = itemView.findViewById(R.id.imgDeleteCategoryItem)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoriesViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.categories_item, parent, false)
        return CategoriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriesViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategories.text = category.name
        holder.imgEditCategoryItem.setOnClickListener {
            onEdit(category)
        }
        holder.imgDeleteCategoryItem.setOnClickListener {
            onDelete(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateListCategory(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}