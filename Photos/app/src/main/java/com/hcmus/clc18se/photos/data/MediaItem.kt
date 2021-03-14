package com.hcmus.clc18se.photos.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import java.util.*

data class MediaItem(
        val id: Long,
        val name: String,
        val uri: Uri,
        val dateCreated: Date,
        val mimeType: String,
        val orientation: Int
) {
    companion object {
        val DiffCallBack = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}