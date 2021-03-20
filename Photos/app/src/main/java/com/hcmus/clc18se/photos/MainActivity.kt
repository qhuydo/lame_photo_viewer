package com.hcmus.clc18se.photos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.databinding.ActivityMainBinding

/**
 * This activity is used for checking permission & redirect to other activity
 * based on app bar configuration
 *
 * When the app is not granted read storage permission - a screen will appear
 * to prompt the user to grant that permission.
 *
 * When the app has necessary permissions, it will redirect to [PhotosActivity]
 * if the bottom bar navigation is used, or it will redirect to [PhotosPagerActivity].
 * That configuration can be found in the app settings.

 */
class MainActivity : AppCompatActivity() {

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        const val TAB_LAYOUT_OPTION = "2"
        const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045
    }

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        if (haveStoragePermission()) {
            jumpToAnotherActivity()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.grantPermissionButton.setOnClickListener { requestPermission() }
        setContentView(binding.root)
    }

    private fun jumpToAnotherActivity() {
        val bottomAppBarPref = preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0")
        val usingTabLayout = bottomAppBarPref == TAB_LAYOUT_OPTION
        if (usingTabLayout) {
            startActivity(Intent(this, PhotosPagerActivity::class.java))
        } else {
            startActivity(Intent(this, PhotosActivity::class.java))
        }
        finish()
    }

    private fun haveStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        } else {
            jumpToAnotherActivity()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    jumpToAnotherActivity()
                } else {
                    goToSettings()
                    return
                }
            }
        }
    }

    /**
     * Go to the application settings to grant permissions.
     */
    private fun goToSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }
}