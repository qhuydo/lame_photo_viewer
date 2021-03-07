package com.hcmus.clc18se.photos

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.databinding.ActivityPhotosBinding
import com.hcmus.clc18se.photos.utils.*
import de.psdev.licensesdialog.LicensesDialogFragment
import timber.log.Timber


class PhotosActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPhotosBinding.inflate(layoutInflater) }

    private val navHostFragment by lazy { supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment }

    private val navController by lazy { navHostFragment.navController }

    private val drawerLayout by lazy { binding.drawerLayout }

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private var isFabRotate = false

    private lateinit var appBarConfiguration: AppBarConfiguration

    private val colorThemeMapper by lazy {
        listOf(
                R.color.red_500 to R.style.Theme_Photos_Red_NoActionBar,
                R.color.deep_orange_500 to R.style.Theme_Photos_Orange_NoActionBar,
                R.color.amber_500 to R.style.Theme_Photos_Yellow_NoActionBar,
                R.color.green_500 to R.style.Theme_Photos_Green_NoActionBar,
                R.color.blue_500 to R.style.Theme_Photos_Blue_NoActionBar,
                R.color.indigo_500 to R.style.Theme_Photos_Indigo_NoActionBar,
                R.color.dark_purple_500 to R.style.Theme_Photos_Purple_NoActionBar,
                R.color.pink_500 to R.style.Theme_Photos_Pink_NoActionBar,
                R.color.brown_500 to R.style.Theme_Photos_Brown_NoActionBar,
                R.color.grey_500 to R.style.Theme_Photos_Grey_NoActionBar
        ).map { resources.getInteger(it.first) to it.second }.toMap()
    }

    private fun getCurrentThemeColor(): Int {
        val currentColor = preferences.getInt(getString(R.string.app_color_key), R.color.indigo_500)
        return colorThemeMapper[currentColor] ?: R.style.Theme_Photos_Indigo_NoActionBar
    }

    private fun configColor() {
        Timber.d("Config color")
        val theme = getCurrentThemeColor()
        Timber.d("Theme ${resources?.getResourceEntryName(theme)}")
        setTheme(theme)
    }

    private var bottomAppBarVisibility: Boolean = true
    private val bottomAppBarVisibilityKey: String = "bottomAppBarVisibilityKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configColor()
        configTheme()

        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.page_photo, R.id.page_album, R.id.page_people), drawerLayout
        )
        Timber.d("On Create called")
        Timber.d("----------------")

        regsiterOnChangedPreferenceListener()

        setContentView(binding.root)

        setUpNavigationBar()
        savedInstanceState?.let {
            bottomAppBarVisibility = it.getBoolean(bottomAppBarVisibilityKey)
            setAppbarVisibility(bottomAppBarVisibility)
        }
    }


    private fun addOnDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val newState = destination.id in arrayOf(
                    R.id.page_photo,
                    R.id.page_people,
                    R.id.page_album
            )
            if (bottomAppBarVisibility != newState) {
                bottomAppBarVisibility = newState
                setAppbarVisibility(bottomAppBarVisibility)
            }
        }
    }

    private fun setUpNavigationBar() {
        addOnDestinationChangedListener()

        val toolbar2 = binding.topAppBar2.fragmentToolBar
        setSupportActionBar(toolbar2)
        toolbar2.setupWithNavController(navController, appBarConfiguration)

        val toolbar = binding.topAppBar.searchActionBar
        setSupportActionBar(toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)


        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        ViewAnimation.init(binding.fabAddPicture)
        ViewAnimation.init(binding.fabAddVideo)

        binding.fab.setOnClickListener {
            isFabRotate = ViewAnimation.rotateFab(it, !isFabRotate)
            if (isFabRotate) {
                ViewAnimation.showIn(binding.fabAddPicture)
                ViewAnimation.showIn(binding.fabAddVideo)
            } else {
                ViewAnimation.showOut(binding.fabAddPicture)
                ViewAnimation.showOut(binding.fabAddVideo)
            }
        }
        binding.topAppBar.appBarLayout.bringToFront()
    }


    private fun setAppbarVisibility(visibility: Boolean) {
        Timber.d("setAppbarVisibility(visibility: $visibility)")

        val layoutParams = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams

        if (visibility) {
            binding.apply {
                topAppBar.appBarLayout.visibility = View.VISIBLE
                topAppBar2.fragmentAppBarLayout.visibility = View.INVISIBLE
                bottomAppBar.visibility = View.VISIBLE

                fab.visibility = View.VISIBLE

                layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.search_bar_height)

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.apply {
                topAppBar.appBarLayout.visibility = View.GONE
                topAppBar2.fragmentAppBarLayout.visibility = View.VISIBLE

                bottomAppBar.visibility = View.GONE

                fab.visibility = View.GONE
                fabAddPicture.visibility = View.GONE
                fabAddVideo.visibility = View.GONE

                setAppBarHeight<CoordinatorLayout.LayoutParams>(
                        binding.topAppBar2.fragmentAppBarLayout,
                        getAppBarSizeAttr(this@PhotosActivity) ?: DEFAULT_APP_BAR_HEIGHT)

                layoutParams.topMargin = getAppBarSizeAttr(this@PhotosActivity)
                        ?: DEFAULT_APP_BAR_HEIGHT

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(
                item,
                navController
        ) || super.onOptionsItemSelected(
                item
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        configTheme(newConfig.uiMode)
        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(bottomAppBarVisibilityKey, bottomAppBarVisibility)
    }

    /**
     * Config the system theme
     * @param uiMode: new configuration mode, use null when no the fun did not called in onConfigurationChanged
     */
    private fun configTheme(uiMode: Int? = null) {
        val USE_DEFAULT = 0
        val WHITE = 1
        val DARK = 2

        val themeOptions = preferences.getString(getString(R.string.app_theme_key), "")
        val options = resources.getStringArray(R.array.theme_values)

        Timber.d("configTheme(uiMode: $uiMode)")
        Timber.d("themeOptions $themeOptions")
        when (themeOptions) {
            options[USE_DEFAULT] -> {
                Timber.d("Config default theme: ${uiMode ?: resources.configuration.uiMode}")
                configDefaultTheme(uiMode ?: resources.configuration.uiMode)
            }
            options[WHITE] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            options[DARK] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> Timber.w("No theme has been set")
        }

    }

    private fun configLanguage(uiMode: Int? = null) {
        val DEFAULT = 0
        val ENGLISH = 1
        val VIETNAM = 2
        val languageOptions = preferences.getString("app_language", "")
        val options = resources.getStringArray(R.array.language_values)

        when (languageOptions) {
            options[DEFAULT] -> {

            }
            options[ENGLISH] -> {

            }
            options[VIETNAM] -> {

            }
            else -> Timber.w("No language has been set")
        }
        recreate()

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

    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            getString(R.string.app_theme_key) -> {
                configTheme(null)
                //navController.navigateUp()
                //recreate()
                finish()
                overridePendingTransition(0, 0)
                startActivity(getIntent())
                overridePendingTransition(0, 0)
            }
            getString(R.string.app_color_key) -> {

                val COLORS_RESOURCES = listOf(
                        R.color.red_500 to ICON_COLOR.RED,
                        R.color.deep_orange_500 to ICON_COLOR.ORANGE,
                        R.color.amber_500 to ICON_COLOR.YELLOW,
                        R.color.green_500 to ICON_COLOR.GREEN,
                        R.color.blue_500 to ICON_COLOR.BLUE,
                        R.color.indigo_500 to ICON_COLOR.INDIGO,
                        R.color.dark_purple_500 to ICON_COLOR.PURPLE,
                        R.color.pink_500 to ICON_COLOR.PINK,
                        R.color.brown_500 to ICON_COLOR.BROWN,
                        R.color.grey_500 to ICON_COLOR.GREY,
                ).map { resources.getInteger(it.first) to it.second }

                val RESOURCE_MAPPER = COLORS_RESOURCES.toMap()

                Timber.d("Color config change")
                val newColor = preferences.getInt(getString(R.string.app_color_key), R.color.indigo_500)
                Timber.d("Color ${RESOURCE_MAPPER[newColor] ?: ICON_COLOR.INDIGO}")
                setIcon(packageManager, RESOURCE_MAPPER[newColor] ?: ICON_COLOR.INDIGO)
            }

        }
    }

    private fun regsiterOnChangedPreferenceListener() {
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    fun onLicenseButtonClick(view: View) {
        val fragment = LicensesDialogFragment.Builder(this)
                .setNotices(R.raw.licenses)
                .setThemeResourceId(getCurrentThemeColor())
                .build()

        fragment.show(supportFragmentManager, null)
    }

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_VIDEO_CAPTURE = 1

    fun fab_add_video(view: View) {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }
    }

    fun fab_add_picture(view: View) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }
}