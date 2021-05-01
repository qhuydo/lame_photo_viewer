package com.hcmus.clc18se.photos.data

import androidx.room.Embedded
import androidx.room.Relation

data class CustomAlbum(
        @Embedded val albumInfo: CustomAlbumInfo,

        @Relation(
                parentColumn = "id",
                entityColumn = "album_id",
                entity = CustomAlbumItem::class
        )
        val albumItems: List<CustomAlbumItem>
)