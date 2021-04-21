package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class PhotosViewModel(application: Application,
                      private val database: PhotosDatabaseDao
) : AndroidViewModel(application) {

    private var _mediaItemList = MutableLiveData<List<MediaItem>>()
    val mediaItemList: LiveData<List<MediaItem>>
        get() = _mediaItemList

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

    init {
        loadImages()
    }

    private var contentObserver: ContentObserver? = null

    fun setCurrentItemView(newIdx: Int) {
        _mediaItemList.value?.let {
            if (newIdx in it.indices) {
                _idx.value = newIdx
            }
        }
    }

    // wtf is this?
    fun loadDataFromOtherViewModel(other: AndroidViewModel) {
        when (other) {
            is PhotosViewModel -> _mediaItemList.value = other._mediaItemList.value
            is FavouriteAlbumViewModel -> _mediaItemList.value = other.mediaItems.value
        }
    }

    fun loadImages() {
        viewModelScope.launch {
            val images = queryMediaItems()
            _mediaItemList.postValue(images)

            if (contentObserver == null) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        loadImages()
                    }
                }
                getApplication<Application>().contentResolver.registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
                )
                contentObserver = observer
            }
        }
    }

    fun setMediaItemFromAlbum(mediaItems: List<MediaItem>) {
        _mediaItemList.value = mediaItems
    }

    private suspend fun queryMediaItems(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()

        withContext(Dispatchers.IO) {

            val projection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                arrayOf(MediaStore.MediaColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_TAKEN,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.Images.ImageColumns.ORIENTATION
                )
            } else {
                arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_TAKEN,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                )
            }
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                    " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
            // val selection = MediaStore.Images.Media.DATE_ADDED
            val selectionArgs: Array<String>? = null
            val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

            val columnUri = MediaStore.Files.getContentUri("external")

            getApplication<Application>().contentResolver.query(
                    columnUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateAdded = Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val orientation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION))
                    } else {
                        0
                    }
                    val path = cursor.getString(pathColumn)

                    val uri = ContentUris.withAppendedId(
                            MediaStore.Files.getContentUri("external"),
                            id)

                    val image = MediaItem(id, displayName, uri, dateAdded, mimeType, orientation, path)
                    mediaItems += image

                }

                Timber.i("Found ${cursor.count} images")
            }
        }
        Timber.i("Found ${mediaItems.size} images")
        return mediaItems
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}

@Suppress("UNCHECKED_CAST")
class PhotosViewModelFactory(
        private val application: Application,
        private val database: PhotosDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotosViewModel::class.java)) {
            return PhotosViewModel(application, database) as T
        }
        throw Exception()
    }
}