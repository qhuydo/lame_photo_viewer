package com.hcmus.clc18se.photos

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.utils.ICON_COLOR
import com.hcmus.clc18se.photos.utils.setIcon
import de.psdev.licensesdialog.LicensesDialogFragment
import timber.log.Timber
import java.util.*


abstract class AbstractPhotosActivity : AppCompatActivity() {

    companion object {
        const val BUNDLE_BOTTOM_APPBAR_VISIBILITY: String = "bottomAppBarVisibilityKey"
        const val BUNDLE_DEFAULT_SYSTEM_UI_VISIBILITY: String = "BUNDLE_DEFAULT_SYSTEM_UI_VISIBILITY"

        const val THEME_USE_DEFAULT = 0
        const val THEME_WHITE = 1
        const val THEME_DARK = 2

    }

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    protected var bottomAppBarVisibility: Boolean = true
    protected var defaultSystemUiVisibility: Int = -1

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

    private val colorResources by lazy {
        listOf(
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
    }
    private val colorResourceMapper by lazy { colorResources.toMap() }

    private fun getCurrentThemeColor(): Int {
        val currentColor = preferences.getInt(getString(R.string.app_color_key), R.color.indigo_500)
        return colorThemeMapper[currentColor] ?: R.style.Theme_Photos_Indigo_NoActionBar
    }

    protected fun configColor() {
        Timber.d("Config color")
        val theme = getCurrentThemeColor()
        Timber.d("Theme ${resources?.getResourceEntryName(theme)}")
        setTheme(theme)
    }

    protected fun displayBottomBarPreference(): Boolean {
        return preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0") == "0"
    }

    protected abstract fun addOnDestinationChangedListener()

    protected abstract fun closeFabBeforeNavigating()

    protected abstract fun setUpNavigationBar()

    protected abstract fun setAppbarVisibility(visibility: Boolean)

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        configTheme(newConfig.uiMode)
        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_BOTTOM_APPBAR_VISIBILITY, bottomAppBarVisibility)
        outState.putInt(BUNDLE_DEFAULT_SYSTEM_UI_VISIBILITY, defaultSystemUiVisibility ?: -1)
    }

    /**
     * Config the system theme
     * @param uiMode: new configuration mode, use null when no the fun did not called in onConfigurationChanged
     */
    protected fun configTheme(uiMode: Int? = null) {

        val themeOptions = preferences.getString(getString(R.string.app_theme_key), "")
        val options = resources.getStringArray(R.array.theme_values)

        Timber.d("configTheme(uiMode: $uiMode)")
        Timber.d("themeOptions $themeOptions")
        when (themeOptions) {
            options[THEME_USE_DEFAULT] -> {
                Timber.d("Config default theme: ${uiMode ?: resources.configuration.uiMode}")
                configDefaultTheme(uiMode ?: resources.configuration.uiMode)
            }
            options[THEME_WHITE] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            options[THEME_DARK] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> Timber.w("No theme has been set")
        }

    }

    private fun setAppLocale(localeCode: String) {
        val displayMetrics: DisplayMetrics = resources.getDisplayMetrics()
        val configuration: Configuration = resources.getConfiguration()
        configuration.setLocale(Locale(localeCode.toLowerCase()))
        resources.updateConfiguration(configuration, displayMetrics)
        configuration.locale = Locale(localeCode.toLowerCase())
        resources.updateConfiguration(configuration, displayMetrics)
    }

    protected fun configLanguage(locale: Locale? = null) {
        val defaultIdx = 0

        val languageOptions = preferences.getString("app_language", "default")
        val options = resources.getStringArray(R.array.language_values)
        Timber.d("languageOptions $languageOptions")
        when (languageOptions) {
            options[defaultIdx] -> {

            }
            else -> setAppLocale(languageOptions!!)
        }
    }

    protected fun configDefaultTheme(uiMode: Int) {
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
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            getString(R.string.app_color_key) -> {

                Timber.d("Color config change")
                val newColor = preferences.getInt(getString(R.string.app_color_key), R.color.indigo_500)
                Timber.d("Color ${colorResourceMapper[newColor] ?: ICON_COLOR.INDIGO}")
                setIcon(packageManager, colorResourceMapper[newColor] ?: ICON_COLOR.INDIGO)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            getString(R.string.app_language_key) -> {
                configLanguage()
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            getString(R.string.app_bottom_bar_navigation_key) -> {

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    protected fun registerOnChangedPreferenceListener() {
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    fun onLicenseButtonClick() {
        val fragment = LicensesDialogFragment.Builder(this)
                .setNotices(R.raw.licenses)
                .setThemeResourceId(getCurrentThemeColor())
                .build()

        fragment.show(supportFragmentManager, null)
    }

    @Suppress("UNUSED_PARAMETER")
    fun fabAddVideo(view: View) {
        Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivity(takeVideoIntent)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun fabAddPicture(view: View) {
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivity(takePictureIntent)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onLicenseButtonClick(view: View) = onLicenseButtonClick()

    fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    fun showSystemUI() {
        window.decorView.systemUiVisibility = defaultSystemUiVisibility
    }

}