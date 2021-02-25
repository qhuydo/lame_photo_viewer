package com.hcmus.clc18se.photos

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Override application to setup Timber log instance
 */
@Suppress("unused")
class PhotosApplication: Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

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