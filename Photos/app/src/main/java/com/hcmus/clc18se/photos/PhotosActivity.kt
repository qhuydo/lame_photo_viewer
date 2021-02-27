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

    private val binding by lazy { ActivityPhotosBinding.inflate(layoutInflater) }

    private val navHostFragment by lazy { supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment }

    private val navController by lazy { navHostFragment.navController }

    private val drawerLayout by lazy { binding.drawerLayout }

    val appBarConfiguration by lazy {
        AppBarConfiguration(setOf(
                R.id.page_photo,
                R.id.page_album,
                R.id.page_people), drawerLayout
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setUpBottomAppbar()
    }

    private fun setUpBottomAppbar() {
        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in arrayOf(
                            R.id.page_photo,
                            R.id.page_people,
                            R.id.page_album
                    )
            ) {
                binding.bottomAppBar.performShow()
                binding.fab.show()
            } else {
                binding.bottomAppBar.performHide()
                binding.fab.hide()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }
}