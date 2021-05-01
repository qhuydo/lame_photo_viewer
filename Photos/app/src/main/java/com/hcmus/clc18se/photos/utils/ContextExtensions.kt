package com.hcmus.clc18se.photos.utils

import android.content.Context
import android.content.SharedPreferences
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter

fun Context.currentAlbumListItemView(preferences: SharedPreferences): Int {
    return preferences.getString(getString(R.string.album_list_view_type_key),
            AlbumListAdapter.ITEM_TYPE_LIST.toString())!!.toInt()
}

fun Context.currentPhotoListItemView(preferences: SharedPreferences): Int {

    return preferences.getString(
            getString(R.string.photo_list_view_type_key),
            MediaItemListAdapter.ITEM_TYPE_LIST.toString()
    )!!.toInt()

}

fun Context.currentAlbumListItemSize(preferences: SharedPreferences): Int {
    return preferences.getString(getString(R.string.album_list_item_size_key),
            "0")!!.toInt()
}

fun Context.currentPhotoListItemSize(preferences: SharedPreferences): Int {
    return preferences.getString(
            getString(R.string.photo_list_item_size_key),
            "0"
    )!!.toInt()
}