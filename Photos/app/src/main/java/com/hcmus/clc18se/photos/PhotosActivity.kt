package com.hcmus.clc18se.photos

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.hcmus.clc18se.photos.databinding.ActivityPhotosBinding


class PhotosActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var binding: ActivityPhotosBinding

    private lateinit var navHostFragment: NavHostFragment

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPhotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        setUpActionBar()
        setUpNavigation()
        binding.fab.setOnClickListener {
            Snackbar.make(it, "Hello", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setUpActionBar() {
        val searchActionBar = binding.topAppBar.searchActionBar
        searchActionBar.setOnClickListener {
            Toast.makeText(this, "Hello world", Toast.LENGTH_SHORT).show()
        }
        setSupportActionBar(searchActionBar)

        searchActionBar.apply {
            navigationContentDescription = ""

            setNavigationIcon(R.drawable.ic_outline_menu_24)
            setNavigationOnClickListener {
                onSupportNavigateUp()
            }
        }
    }

    private fun setUpNavigation() {
        val searchActionBar = binding.topAppBar.searchActionBar

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bottomBar = binding.bottomNav
            if (destination.id in arrayOf(
                            R.id.page_photo,
                            R.id.page_people,
                            R.id.page_album
                    )
            ) {
                supportActionBar?.show()
                binding.bottomNav.visibility = View.VISIBLE
                binding.bottomAppBar.performShow()
                binding.fab.show()
            } else {
                supportActionBar?.hide()
                binding.bottomNav.visibility = View.GONE
                binding.bottomAppBar.performHide()
                binding.fab.hide()

            }
        }

        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.page_photo,
                R.id.page_album,
                R.id.page_people), drawerLayout)

        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        NavigationUI.setupWithNavController(searchActionBar, navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)
        //binding.bottomAppBar.setupWithNavController(navController)
//
//        binding.bottomAppBar.setOnMenuItemClickListener {  menuItem ->
//            menuItem.onNavDestinationSelected(navController)
//        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.navHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }
}