package com.hcmus.clc18se.photos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.hcmus.clc18se.photos.databinding.ActivityPhotosBinding

class PhotosActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityPhotosBinding = DataBindingUtil.setContentView(
                this,
                R.layout.activity_photos
        )

        val searchActionBar = binding.topAppBar.searchActionBar
        searchActionBar.setOnClickListener {
            Toast.makeText(this, "Hello world", Toast.LENGTH_SHORT).show()
        }

        drawerLayout = binding.drawerLayout

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        //NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        NavigationUI.setupWithNavController(binding.navView, navController)
        setSupportActionBar(searchActionBar)

        searchActionBar.apply {
            navigationContentDescription = ""

            setNavigationIcon(R.drawable.ic_outline_menu_24)
            setNavigationOnClickListener {
                onSupportNavigateUp()
            }

        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.navHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }
}