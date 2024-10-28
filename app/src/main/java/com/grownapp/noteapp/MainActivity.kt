package com.grownapp.noteapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.grownapp.noteapp.databinding.ActivityMainBinding
import com.grownapp.noteapp.ui.categories.CategoriesViewModel
import com.grownapp.noteapp.ui.categories.dao.Category

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var categoriesViewModel: CategoriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_note, R.id.nav_edit_categories, R.id.nav_backup,
                R.id.nav_trash, R.id.nav_settings, R.id.nav_rate,
                R.id.nav_help, R.id.nav_policy
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        categoriesViewModel =
            ViewModelProvider(this)[CategoriesViewModel::class.java]

        categoriesViewModel.allCategory.observe(this) { category ->
            loadCategoriesOnNavMenu(drawerLayout, navView, category)
        }
    }

    private fun loadCategoriesOnNavMenu(
        drawerLayout: DrawerLayout,
        navView: NavigationView,
        categories: List<Category>
    ) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val menu = navView.menu
        val categoryGroup = menu.findItem(R.id.nav_categories_placeholder).subMenu

        if (categories.isEmpty()) return

        categoryGroup?.clear()

        categories.forEach { category ->
            val menuItem = categoryGroup?.add(category.name)
            menuItem?.icon = ContextCompat.getDrawable(this, R.drawable.category)
            menuItem?.setOnMenuItemClickListener {
                val action = NoteNavigationDirections.actionGlobalNavNote(category.name)
                navController.navigate(action)
                drawerLayout.closeDrawers()
                true
            }
        }

        val menuItemUncategorized = categoryGroup?.add("Uncategorized")
        menuItemUncategorized?.icon = ContextCompat.getDrawable(this, R.drawable.uncategorized)
        menuItemUncategorized?.setOnMenuItemClickListener {
            val action = NoteNavigationDirections.actionGlobalNavNote("Uncategorized")
            navController.navigate(action)
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val menuItemEditCategories = categoryGroup?.add(R.string.menu_categories)
        menuItemEditCategories?.icon = ContextCompat.getDrawable(this, R.drawable.edit_category)
        menuItemEditCategories?.setOnMenuItemClickListener {
            navController.navigate(R.id.action_nav_note_to_nav_edit_categories)
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_note, R.id.nav_edit_categories, R.id.nav_backup,
                R.id.nav_trash, R.id.nav_settings, R.id.nav_rate,
                R.id.nav_help, R.id.nav_policy -> {
                    binding.appBarMain.toolbar.subtitle = null
                }
            }
        }
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}