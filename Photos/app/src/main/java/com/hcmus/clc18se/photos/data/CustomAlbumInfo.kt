package com.hcmus.clc18se.photos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_album")
data class CustomAlbumInfo(
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0L,

        val name: String,
//
//        @ColumnInfo(name="date_created")
//        var dateCreated: Long = System.currentTimeMillis()
)