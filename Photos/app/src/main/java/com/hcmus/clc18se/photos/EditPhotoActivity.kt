package com.hcmus.clc18se.photos

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.databinding.ActivityPhotosBinding
import timber.log.Timber

class EditPhotoActivity : AppCompatActivity() {
    var choose = 0
    private val binding by lazy { ActivityPhotosBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_edit_photo)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_edit)
    }
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.filter_editor -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.bright_editor -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.color_editor -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.add_icon_editor -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.crop_editor -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }
}