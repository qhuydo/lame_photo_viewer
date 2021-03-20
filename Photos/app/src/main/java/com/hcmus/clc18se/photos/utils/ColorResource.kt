package com.hcmus.clc18se.photos.utils

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import timber.log.Timber

class ColorResource(
        private val application: Application,
        private val preferences: SharedPreferences) {

    private val resources by lazy { application.resources }

    private var setNewIcon: Boolean = false
    internal fun enableSetNewIconFlag() {
        setNewIcon = true
    }

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

    internal val colorResourceMapper by lazy { colorResources.toMap() }

    fun getCurrentThemeColor(): Int {
        val currentColor = preferences.getInt(application.getString(R.string.app_color_key), R.color.indigo_500)
        return colorThemeMapper[currentColor] ?: R.style.Theme_Photos_Indigo_NoActionBar
    }

    internal fun configColor(activity: AppCompatActivity) {
        Timber.d("Config color")
        val theme = getCurrentThemeColor()
        Timber.d("Theme ${resources?.getResourceEntryName(theme)}")
        activity.setTheme(theme)
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

    /**
     * Config the system theme
     * @param uiMode: new configuration mode, use null when no the fun did not called in onConfigurationChanged
     */
    internal fun configTheme(uiMode: Int? = null) {

        val themeOptions = preferences.getString(application.getString(R.string.app_theme_key), "")
        val options = resources.getStringArray(R.array.theme_values)

        Timber.d("configTheme(uiMode: $uiMode)")
        Timber.d("themeOptions $themeOptions")

        when (themeOptions) {
            options[AbstractPhotosActivity.THEME_USE_DEFAULT] -> {
                Timber.d("Config default theme: ${uiMode ?: resources.configuration.uiMode}")
                configDefaultTheme(uiMode ?: resources.configuration.uiMode)
            }
            options[AbstractPhotosActivity.THEME_WHITE] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            options[AbstractPhotosActivity.THEME_DARK] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> Timber.w("No theme has been set")
        }
    }

    internal fun updateIcon(packageManager: PackageManager) {
        if (setNewIcon) {
            val newColor = preferences.getInt(application.getString(R.string.app_color_key), R.color.indigo_500)
            Timber.d("Color ${colorResourceMapper[newColor] ?: ICON_COLOR.INDIGO}")
            setIcon(packageManager, colorResourceMapper[newColor]
                    ?: ICON_COLOR.INDIGO)
        }
    }

}