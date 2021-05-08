package com.hcmus.clc18se.photos.database

import androidx.room.*
import com.hcmus.clc18se.photos.data.CustomAlbum
import com.hcmus.clc18se.photos.data.CustomAlbumInfo
import com.hcmus.clc18se.photos.data.CustomAlbumItem
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

    @Transaction
    @Query("select * from custom_album order by name asc, id desc")
    suspend fun getAllCustomAlbums(): List<CustomAlbum>

    @Transaction
    @Query("select * from custom_album where id=:id")
    suspend fun getCustomAlbum(id: Long): CustomAlbum

    @Delete
    suspend fun removeCustomAlbumItem(item: CustomAlbumItem)

    @Insert
    suspend fun addNewCustomAlbum(customAlbumInfo: CustomAlbumInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMediaItemToCustomAlbum(vararg customAlbumItem: CustomAlbumItem)

    @Query("select count(*) from custom_album where name=:name")
    suspend fun containsCustomAlbumName(name: String): Boolean

    @Query("delete from custom_album_item where album_id=:albumId and id in (:id)")
    suspend fun deleteCustomAlbumItemById(albumId: Long, vararg id: Long)
}