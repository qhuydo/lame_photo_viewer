package com.hcmus.clc18se.photos.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class Album(
        val path: String,
        val mediaItems: MutableList<MediaItem>,
        private var thumbnailUri: Uri? = null
): Parcelable {
    fun getName(): String? {
        return File(path).name
    }

    fun getRandomMediaItem(): MediaItem? {
        if (mediaItems.isNotEmpty()) {
            return mediaItems[mediaItems.indices.random()]
        }
        return null
    }
}