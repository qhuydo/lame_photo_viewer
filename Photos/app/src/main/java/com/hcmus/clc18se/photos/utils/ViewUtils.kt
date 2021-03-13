package com.hcmus.clc18se.photos.utils

import android.content.res.Resources
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter.Companion.ITEM_TYPE_LIST
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter.Companion.ITEM_TYPE_GRID

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

fun setAlbumListIcon(menuItem: MenuItem, itemType: Int) = setPhotoListIcon(menuItem, itemType)

fun setPhotoListIcon(menuItem: MenuItem, itemType: Int) {

    when (itemType) {
        ITEM_TYPE_GRID -> {
            menuItem.setIcon(R.drawable.ic_outline_grid_view_24)
        }
        else -> {
            menuItem.setIcon(R.drawable.ic_outline_list_alt_24)
        }
    }
}

fun setAlbumListItemSizeOption(resources: Resources, menu: Menu, currentPreferences: String) {
    val big = 0
    val medium = 1
    val small = 2

    val options = resources.getStringArray(R.array.photo_list_item_size_value)
    when (currentPreferences) {
        options[big] -> menu.findItem(R.id.album_item_view_size_big).isChecked = true
        options[medium] -> menu.findItem(R.id.album_item_view_size_medium).isChecked = true
        options[small] -> menu.findItem(R.id.album_item_view_size_small).isChecked = true
    }
}

fun setPhotoListItemSizeOption(resources: Resources, menu: Menu, currentPreferences: String) {
    val big = 0
    val medium = 1
    val small = 2

    val options = resources.getStringArray(R.array.photo_list_item_size_value)
    when (currentPreferences) {
        options[big] -> menu.findItem(R.id.item_view_size_big).isChecked = true
        options[medium] -> menu.findItem(R.id.item_view_size_medium).isChecked = true
        options[small] -> menu.findItem(R.id.item_view_size_small).isChecked = true
    }
}

fun getSpanCountForPhotoList(resources: Resources, viewType: Int, iconSize: Int): Int {
    return when (viewType) {
        ITEM_TYPE_LIST -> resources.getInteger(R.integer.span_count_photo_list_big)
        else -> {
            return when (iconSize) {
                0 -> resources.getInteger(R.integer.span_count_photo_grid_big)
                1 -> resources.getInteger(R.integer.span_count_photo_grid_medium)
                else -> resources.getInteger(R.integer.span_count_photo_grid_small)
            }
        }
    }
}

fun getSpanCountForAlbumList(resources: Resources, viewType: Int, iconSize: Int) =
        getSpanCountForPhotoList(resources, viewType, iconSize)