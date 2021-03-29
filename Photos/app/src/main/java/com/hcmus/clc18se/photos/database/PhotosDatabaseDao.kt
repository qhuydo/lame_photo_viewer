package com.hcmus.clc18se.photos.database

import androidx.room.*
import com.hcmus.clc18se.photos.data.FavouriteItem

@Dao
interface PhotosDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavouriteItems(vararg items: FavouriteItem?)

    @Delete
    suspend fun removeFavouriteItems(vararg items: FavouriteItem?)

    @Query("select count(*) from favourite where id=:id")
    suspend fun hasFavouriteItem(id: Long): Boolean

    @Query("select * from favourite")
    suspend fun getAllFavouriteItems(): List<FavouriteItem>
}