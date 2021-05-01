package com.hcmus.clc18se.photos.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hcmus.clc18se.photos.data.CustomAlbumInfo
import com.hcmus.clc18se.photos.data.CustomAlbumItem
import com.hcmus.clc18se.photos.data.FavouriteItem

@Database(
    entities = [FavouriteItem::class, CustomAlbumItem::class, CustomAlbumInfo::class],
    version = 2,
    exportSchema = true,
)
abstract class PhotosDatabase : RoomDatabase() {

    abstract val photosDatabaseDao: PhotosDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: PhotosDatabase? = null

        fun getInstance(context: Context): PhotosDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PhotosDatabase::class.java,
                        "photos_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}