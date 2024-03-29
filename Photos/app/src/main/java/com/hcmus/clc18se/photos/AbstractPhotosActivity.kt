package com.hcmus.clc18se.photos

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.utils.OnBackPressed
import com.hcmus.clc18se.photos.utils.OnDirectionKeyDown
import com.hcmus.clc18se.photos.utils.ui.IconColour
import com.hcmus.clc18se.photos.viewModels.*
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

    protected val albumViewModel: AlbumViewModel by viewModels()

    protected val photosViewModel: PhotosViewModel by viewModels {
        PhotosViewModelFactory(application, PhotosDatabase.getInstance(this).photosDatabaseDao)
    }

    private val customAlbumViewModel: CustomAlbumViewModel by viewModels {
        CustomAlbumViewModelFactory(
            application,
            PhotosDatabase.getInstance(this).photosDatabaseDao
        )
    }

    // internal val mediaProvider: MediaProvider by lazy { MediaProvider(application.applicationContext) }

    internal val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private val colorResource by lazy { (application as PhotosApplication).colorResource }

    protected var bottomAppBarVisibility: Boolean = true

    protected var defaultSystemUiVisibility: Int = -1

    abstract val appBarConfiguration: AppBarConfiguration

    abstract val navHostFragment: NavHostFragment

    protected fun displayBottomBarPreference(): Boolean {
        return preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0") == "0"
    }

    protected abstract fun addOnDestinationChangedListener()

    protected abstract fun closeFabBeforeNavigating()

    protected abstract fun setUpNavigationBar()

    protected abstract fun setAppbarVisibility(visibility: Boolean)

    internal fun jumpToStartActivity() {
        val intent = Intent(this@AbstractPhotosActivity, StartActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photosViewModel.loadImages()
        albumViewModel.loadAlbums()
        colorResource.configColor(this)
        colorResource.configTheme()
        configLanguage()
        customAlbumViewModel.loadData()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)

        colorResource.configTheme(newConfig.uiMode)
        configLanguage()

        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_BOTTOM_APPBAR_VISIBILITY, bottomAppBarVisibility)
        outState.putInt(BUNDLE_DEFAULT_SYSTEM_UI_VISIBILITY, defaultSystemUiVisibility)
    }

    private fun setAppLocale(localeCode: String) {

        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val configuration: Configuration = resources.configuration

        configuration.setLocale(Locale(localeCode.toLowerCase(Locale.ROOT)))

        resources.updateConfiguration(configuration, displayMetrics)
    }

    private fun configLanguage() {
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
                startActivity(Intent(this, StartActivity::class.java))
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

                startActivity(Intent(this, StartActivity::class.java))
                finish()
            }
            getString(R.string.app_language_key) -> {
                val defaultIdx = 0

                val languageOptions = preferences.getString("app_language", "default")
                val options = resources.getStringArray(R.array.language_values)

                when (languageOptions) {
                    options[defaultIdx] -> {
                        val config = Configuration().apply { setToDefaults() }
                        baseContext.resources.updateConfiguration(
                            config,
                            baseContext.resources.displayMetrics
                        )
                    }
                    else -> {
                        configLanguage()
                    }
                }
                finish()
                overridePendingTransition(0, 0)
                startActivity(Intent(this, StartActivity::class.java))
                overridePendingTransition(0, 0)

            }
            getString(R.string.app_bottom_bar_navigation_key) -> {
                startActivity(Intent(this, StartActivity::class.java))
                finish()
            }
            getString(R.string.adaptive_icon_color_key) -> {
                val adaptiveIconColor =
                    preferences.getBoolean(getString(R.string.adaptive_icon_color_key), false)

                if (adaptiveIconColor) {
                    val currentIconColor = colorResource.getCurrentThemeColor()
                    if (colorResource.colorResourceMapper[currentIconColor] != IconColour.INDIGO) {
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

    abstract fun getNavGraphResId(): Int

    abstract val drawerLayout: DrawerLayout

    override fun onBackPressed() {
        // get the current fragment
        val currentFragment = navHostFragment.childFragmentManager.fragments[0] as? OnBackPressed
        val defaultBackPress = currentFragment?.onBackPress()?.not() ?: true

        if (defaultBackPress) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val currentFragment =
                    navHostFragment.childFragmentManager.fragments[0] as? OnDirectionKeyDown
                val defaultKeyDown = currentFragment?.onKeyDown(keyCode, event)?.not() ?: true
                if (defaultKeyDown) {
                    return super.onKeyDown(keyCode, event)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    internal fun haveWriteStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Detects and toggles immersive mode.
     */
    fun toggleImmersiveMode() {

        // BEGIN_INCLUDE (get_current_ui_flags)
        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        val uiOptions: Int = window.decorView.systemUiVisibility
        var newUiOptions = uiOptions
        // END_INCLUDE (get_current_ui_flags)
        // BEGIN_INCLUDE (toggle_ui_flags)
        val isImmersiveModeEnabled = (uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions
        if (isImmersiveModeEnabled) {
            Timber.i("Turning immersive mode mode off. ")
        } else {
            Timber.i("Turning immersive mode mode on.")
        }

        // Immersive mode: Backward compatible to KitKat (API 19).
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // This sample uses the "sticky" form of immersive mode, which will let the user swipe
        // the bars back in again, but will automatically make them disappear a few seconds later.
        newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_FULLSCREEN
        newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = newUiOptions
        //END_INCLUDE (set_ui_flags)
    }
}