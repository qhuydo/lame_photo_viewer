package com.hcmus.clc18se.photos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import timber.log.Timber

class EditPhotoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditPhotoBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("On create")
        setContentView(binding.root)
        Timber.d("On created")
        val bottomNavigation = binding.bottomEdit
        bottomNavigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        setBarVisibility(R.id.bright)
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (item.itemId in listOf(R.id.bright,
                        R.id.filter,
                        R.id.add_icon,
                        R.id.crop,
                        R.id.change_color)) {
            setBarVisibility(item.itemId)
            return@OnNavigationItemSelectedListener true
        }
        false
    }

    private fun setBarVisibility(itemId: Int) {
        binding.apply {
            brightEditor.visibility = View.GONE
            filterEditor.visibility = View.GONE
            addIconEditor.visibility = View.GONE
            cropEditor.visibility = View.GONE
            colorEditor.visibility = View.GONE
        }
        when (itemId) {
            R.id.bright -> binding.brightEditor.visibility = View.VISIBLE
            R.id.filter -> binding.filterEditor.visibility = View.VISIBLE
            R.id.add_icon -> binding.addIconEditor.visibility = View.VISIBLE
            R.id.crop -> binding.cropEditor.visibility = View.VISIBLE
            R.id.change_color -> binding.colorEditor.visibility = View.VISIBLE
        }
    }

}