@file:Suppress("unused")

package com.hcmus.clc18se.photos.utils.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import com.hcmus.clc18se.photos.BuildConfig
import timber.log.Timber

enum class IconColour {
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE,
    INDIGO,
    PURPLE,
    PINK,
    BROWN,
    GREY
}

fun setIcon(packageManager: PackageManager, targetColor: IconColour) {
    for (value in IconColour.values()) {
        val action = if (value == targetColor) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        Timber.d("Color ${value.name} - Action ${
            if (action == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) "STATE_ENABLE" 
            else "STATE_DISABLE"}")

        packageManager.setComponentEnabledSetting(
                ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.${value.name}"),
                action, PackageManager.DONT_KILL_APP
        )
    }
}