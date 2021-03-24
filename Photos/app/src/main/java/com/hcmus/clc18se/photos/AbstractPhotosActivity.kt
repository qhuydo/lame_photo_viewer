package com.hcmus.clc18se.photos

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.MediaProvider
import com.hcmus.clc18se.photos.utils.ICON_COLOR
import com.hcmus.clc18se.photos.viewModels.AlbumViewModel
import com.hcmus.clc18se.photos.viewModels.AlbumViewModelFactory
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

    val viewModel: AlbumViewModel by viewModels {
        AlbumViewModelFactory(application)
    }

    val mediaProvider: MediaProvider = MediaProvider(this)

    internal val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private val colorResource by lazy { (application as PhotosApplication).colorResource }

    protected var bottomAppBarVisibility: Boolean = true
    protected var defaultSystemUiVisibility: Int = -1

    protected fun displayBottomBarPreference(): Boolean {
        return preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0") == "0"
    }

    protected abstract fun addOnDestinationChangedListener()

    protected abstract fun closeFabBeforeNavigating()

    protected abstract fun setUpNavigationBar()

    protected abstract fun setAppbarVisibility(visibility: Boolean)

    internal abstract fun makeToolbarInvisible(wantToMakeToolbarInvisible: Boolean = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProvider.loadAlbum(object: MediaProvider.OnMediaLoadedCallback {
            override fun onMediaLoaded(albums: ArrayList<Album>?) {
                viewModel.notifyAlbumLoaded()
            }
        })
        colorResource.configColor(this)
        colorResource.configTheme()
        configLanguage()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        colorResource.configTheme(newConfig.uiMode)
        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_BOTTOM_APPBAR_VISIBILITY, bottomAppBarVisibility)
        outState.putInt(BUNDLE_DEFAULT_SYSTEM_UI_VISIBILITY, defaultSystemUiVisibility)
    }

    private fun setAppLocale(localeCode: String) {
        val displayMetrics: DisplayMetrics = resources.getDisplayMetrics()
        val configuration: Configuration = resources.getConfiguration()
        configuration.setLocale(Locale(localeCode.toLowerCase()))
        resources.updateConfiguration(configuration, displayMetrics)
        configuration.locale = Locale(localeCode.toLowerCase())
        resources.updateConfiguration(configuration, displayMetrics)
    }

    private fun configLanguage(locale: Locale? = null) {
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

    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            getString(R.string.app_theme_key) -> {
                colorResource.configTheme(null)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }

            getString(R.string.app_color_key) -> {

                Timber.d("Color config change")
                val adaptiveIconColor =
                        preferences.getBoolean(getString(R.string.adaptive_icon_color_key), false)

                if (adaptiveIconColor) {
                    colorResource.enableSetNewIconFlag()
                    colorResource.updateIcon(packageManager)
                }

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

            getString(R.string.adaptive_icon_color_key) -> {
                val adaptiveIconColor =
                        preferences.getBoolean(getString(R.string.adaptive_icon_color_key), false)

                if (adaptiveIconColor) {
                    val currentIconColor = colorResource.getCurrentThemeColor()
                    if (colorResource.colorResourceMapper[currentIconColor] != ICON_COLOR.INDIGO) {
                        colorResource.enableSetNewIconFlag()
                        colorResource.updateIcon(packageManager)
                    }
                }
            }
        }
    }

    protected fun registerOnChangedPreferenceListener() {
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    private fun onLicenseButtonClick() {
        val fragment = LicensesDialogFragment.Builder(this)
                .setNotices(R.raw.licenses)
                .setThemeResourceId(colorResource.getCurrentThemeColor())
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

    abstract fun setNavHostFragmentTopMargin(pixelValue: Int)
}