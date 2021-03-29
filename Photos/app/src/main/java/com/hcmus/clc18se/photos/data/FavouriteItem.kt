package com.hcmus.clc18se.photos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite")
data class FavouriteItem(
        // Id of the MediaItem
        // retrieved from BaseColumns._ID column in MediaStore
        @PrimaryKey
        @ColumnInfo(name="id")
        val id: Long
)