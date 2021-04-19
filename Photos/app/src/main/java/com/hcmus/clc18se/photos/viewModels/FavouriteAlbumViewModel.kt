package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.launch
import timber.log.Timber

class FavouriteAlbumViewModel(
        application: Application,
        private val database: PhotosDatabaseDao
) : AndroidViewModel(application) {

    private var _mediaItems = MutableLiveData<List<MediaItem>>()
    val mediaItems: LiveData<List<MediaItem>>
        get() = _mediaItems

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

    private var _reloadDataRequest = MutableLiveData(false)
    val reloadDataRequest: LiveData<Boolean>
        get() = _reloadDataRequest

    fun setCurrentItemView(newIdx: Int) {
        _mediaItems.value?.let {
            if (newIdx in it.indices) {
                _idx.value = newIdx
            }
        }
    }

    init {
        loadData()
    }

    private fun loadMediaItemFromId(itemId: Long): MediaItem? {
        val columnUri = MediaStore.Files.getContentUri("external")

        // Return only video and image metadata.
        val selection =
                "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                        " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})" +
                        " AND ${BaseColumns._ID}=${itemId}"

        val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                BaseColumns._ID,
        )

        val selectionArgs: Array<String>? = null

        getApplication<Application>().applicationContext.contentResolver.query(
                columnUri,
                projection,
                selection,
                selectionArgs,
                null
        )?.use { cursor ->

            val path: String
            val id: Long
            val mimeType: String
            val name: String

            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            if (cursor.moveToNext()) {
                path = cursor.getString(pathColumn)
                id = cursor.getLong(idColumn)
                mimeType = cursor.getString(mimeTypeColumn)
                name = cursor.getString(nameColumn)

                return MediaItem.getInstance(id, name, null, mimeType, path)
            }

        }
        return null
    }

    fun loadData() {
        viewModelScope.launch {
            Timber.d("Start loading FavouriteMediaItems")
            val startTime = System.currentTimeMillis()

            val favourites = database.getAllFavouriteItems()

            val favouriteMediaItems = ArrayList<MediaItem>()

            for (favouriteItem in favourites) {
                val mediaItem = loadMediaItemFromId(favouriteItem.id)
                mediaItem?.let { favouriteMediaItems.add(it) }
                        ?: database.removeFavouriteItems(favouriteItem)
            }

            _mediaItems.value = favouriteMediaItems
            Timber.d("loadFavouriteMediaItems(): ${(System.currentTimeMillis() - startTime)} ms")
        }
    }

    fun requestReloadingData() {
        _reloadDataRequest.value = true
    }

    fun doneRequestingLoadData() {
        _reloadDataRequest.value = false
    }
}


@Suppress("UNCHECKED_CAST")
class FavouriteAlbumViewModelFactory(
        private val application: Application,
        private val database: PhotosDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavouriteAlbumViewModel::class.java)) {
            return FavouriteAlbumViewModel(application, database) as T
        }
        throw Exception()
    }
}