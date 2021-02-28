@file:Suppress("unused")

package com.hcmus.clc18se.photos

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity

enum class ICON_COLOR {
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

fun setIcon(packageManager: PackageManager, targetColor: ICON_COLOR) {
    for (value in ICON_COLOR.values()) {
        val action = if (value == targetColor) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(
                ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.${value.name}"),
                action, PackageManager.DONT_KILL_APP
        )
    }
}