package com.hcmus.clc18se.photos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        const val TAB_LAYOUT_OPTION = "2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bottomAppBarPref = preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0")
        val usingTabLayout = bottomAppBarPref == TAB_LAYOUT_OPTION
        if (usingTabLayout) {
            startActivity(Intent(this, PhotosPagerActivity::class.java))
        } else {
            startActivity(Intent(this, PhotosActivity::class.java))
        }
        finish()
    }

}