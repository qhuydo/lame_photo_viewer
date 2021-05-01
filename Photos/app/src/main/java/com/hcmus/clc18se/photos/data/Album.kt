package com.hcmus.clc18se.photos.data

import android.net.Uri
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
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

    class DiffCallBack : DiffUtil.ItemCallback<Album>() {
        override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
            return oldItem.path == newItem.path
        }
    }
}