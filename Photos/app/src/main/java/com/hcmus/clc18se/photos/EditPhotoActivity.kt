package com.hcmus.clc18se.photos

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import timber.log.Timber

class EditPhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_edit_photo)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.getItemId()) {
            R.id.bright -> {
            }
            R.id.add_icon -> {
            }
            R.id.draw -> {
            }
            R.id.crop -> {
            }
            R.id.filter -> {
            }
            R.id.changeColor -> {
            }
            else -> Timber.w("No edit option has been set")
        }
    }
}