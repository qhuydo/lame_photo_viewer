package com.hcmus.clc18se.photos

import android.app.Application
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.utils.ColorResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Override application to setup Timber log instance
 */
@Suppress("unused")
class PhotosApplication : Application() {

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
            Timber.plant(Timber.DebugTree())
        }
    }
}