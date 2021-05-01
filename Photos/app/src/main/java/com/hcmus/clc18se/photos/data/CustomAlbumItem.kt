package com.hcmus.clc18se.photos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_album_item")
data class CustomAlbumItem(
        // Id of the MediaItem
        // retrieved from BaseColumns._ID column in MediaStore
        @PrimaryKey
        @ColumnInfo(name="id")
        val id: Long,

        @ColumnInfo(name = "album_id")
        val albumId: Long
)