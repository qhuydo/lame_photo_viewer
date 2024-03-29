package com.hcmus.clc18se.photos

import android.app.Application
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.utils.ui.ColorResource
//import com.squareup.leakcanary.LeakCanary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Override application to setup Timber log instance
 */
class PhotosApplication : Application() {
    companion object{
        val newList = ArrayList<MediaItem>()
        var list:List<MediaItem>? = null
    }
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    internal val colorResource by lazy { ColorResource(this, preferences) }

    override fun onCreate() {
        super.onCreate()
        delayInit()
    }

    private fun delayInit() {
        // Init Timber instance
        applicationScope.launch {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}