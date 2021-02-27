package com.hcmus.clc18se.photos

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.databinding.ActivityPhotosBinding
import de.psdev.licensesdialog.LicensesDialogFragment
import timber.log.Timber


class PhotosActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPhotosBinding.inflate(layoutInflater) }

    private val navHostFragment by lazy { supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment }

    private val navController by lazy { navHostFragment.navController }

    private val drawerLayout by lazy { binding.drawerLayout }

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(
                R.id.page_photo,
                R.id.page_album,
                R.id.page_people
            ), drawerLayout
        )
    }

    private var bottomAppBarVisibility: Boolean = true
    private val bottomAppBarVisibilityKey: String = "bottomAppBarVisibilityKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        regsiterOnChangedPreferenceListener()
        configTheme(null)
        setUpBottomAppbar()
        setContentView(binding.root)

        savedInstanceState?.let {
            bottomAppBarVisibility = it.getBoolean(bottomAppBarVisibilityKey)
            setAppbarVisibility(bottomAppBarVisibility)
        }
    }

    private fun setUpBottomAppbar() {
        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomAppBarVisibility = destination.id in arrayOf(
                R.id.page_photo,
                R.id.page_people,
                R.id.page_album
            )
            setAppbarVisibility(bottomAppBarVisibility)
        }
    }

    private fun setAppbarVisibility(visibility: Boolean) {
        if (visibility) {
            binding.bottomAppBar.visibility = View.VISIBLE
            binding.bottomAppBar.performShow()
            binding.fab.show()
        } else {
            //binding.bottomAppBar.performHide()
            binding.bottomAppBar.visibility = View.GONE
            binding.fab.hide()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(
            item
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configTheme(newConfig.uiMode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(bottomAppBarVisibilityKey, bottomAppBarVisibility)
    }

    /**
     * Config the system theme
     * @param uiMode: new configuration mode, use null when no the fun did not called in onConfigurationChanged
     */
    private fun configTheme(uiMode: Int?) {
        val USE_DEFAULT = 0
        val WHITE = 1
        val DARK = 2

        val themeOptions = preferences.getString("app_theme", "")
        val options = resources.getStringArray(R.array.theme_values)

        when (themeOptions) {
            options[USE_DEFAULT] -> {
                Timber.d("Config default theme: ${uiMode ?: resources.configuration.uiMode}")
                configDefaultTheme(uiMode ?: resources.configuration.uiMode)
                uiMode?.let { recreate() }
            }
            options[WHITE] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            options[DARK] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> Timber.w("No theme has been set")
        }

    }

    private fun configDefaultTheme(uiMode: Int) {
        when (uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                Timber.d("Config UI_MODE_NIGHT_NO")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            } // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {
                Timber.d("Config MODE_NIGHT_YES")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } // Night mode is active, we're using dark theme

        }
    }


    val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "app_theme" -> {
                configTheme(null)
                recreate()
            }
        }
    }

    private fun regsiterOnChangedPreferenceListener() {
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    fun onLicenseButtonClick(view: View) {
        val fragment = LicensesDialogFragment.Builder(this)
            .setNotices(R.raw.licenses)
            .build()

        fragment.show(supportFragmentManager, null)
    }
}