package com.hcmus.clc18se.photos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_album_item")
data class CustomAlbumItem(

        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name="col_id")
        var colId: Long = 0L,

        // Id of the MediaItem
        // retrieved from BaseColumns._ID column in MediaStore
        @ColumnInfo(name="id")
        val id: Long,

        @ColumnInfo(name = "album_id")
        val albumId: Long
)