package com.grownapp.noteapp.ui.note.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteapp.R
import com.grownapp.noteapp.ui.categories.dao.Category

class CategoryForNoteAdapter(
    private var categoryList: List<Category>,
    private val selectedCategoryList: MutableSet<Int>
): RecyclerView.Adapter<CategoryForNoteAdapter.CategoryForNoteViewHolder>() {
    class CategoryForNoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val chbCategory: CheckBox = itemView.findViewById(R.id.chbCategory)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryForNoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.category_item, parent, false)
        return CategoryForNoteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CategoryForNoteViewHolder,
        position: Int
    ) {
        val category = categoryList[position]
        holder.tvCategoryName.text = category.name
        holder.chbCategory.isChecked = selectedCategoryList.contains(category.categoryId)

        holder.chbCategory.setOnCheckedChangeListener{_, isChecked ->
            if (isChecked){
                selectedCategoryList.add(category.categoryId)
            }else{
                selectedCategoryList.remove(category.categoryId)
            }
        }
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateListCategory(newListCategory: List<Category>) {
        categoryList = newListCategory
        notifyDataSetChanged()
    }
}