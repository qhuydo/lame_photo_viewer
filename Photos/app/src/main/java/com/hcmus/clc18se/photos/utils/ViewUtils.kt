package com.hcmus.clc18se.photos.utils

import android.content.res.Configuration
import android.graphics.Rect
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.PhotoListAdapter.Companion.ITEM_TYPE_THUMBNAIL

fun getAppBarSizeAttr(activity: AppCompatActivity): Int? {
    val tv = TypedValue()
    if (activity.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
        return TypedValue.complexToDimensionPixelSize(tv.data, activity.resources.displayMetrics)
    }
    return null
}

const val SEARCH_BAR_HEIGHT = 88
const val DEFAULT_APP_BAR_HEIGHT = 56

fun <T : ViewGroup.LayoutParams> setAppBarHeight(appBarLayout: AppBarLayout, dp: Int,
                                                 activity: AppCompatActivity? = null) {
    val layoutParams = appBarLayout.layoutParams as T
    if (activity != null) {
        layoutParams.height = TypedValue.complexToDimensionPixelSize(
                dp, activity.resources.displayMetrics)
    } else {
        layoutParams.height = dp
    }
    appBarLayout.requestLayout()
}

fun setPhotoListIcon(menuItem: MenuItem, itemType: Int) {

    when (itemType) {
        ITEM_TYPE_THUMBNAIL -> {
            menuItem.setIcon(R.drawable.ic_outline_grid_view_24)
        }
        else -> {
            menuItem.setIcon(R.drawable.ic_outline_list_alt_24)
        }
    }
}