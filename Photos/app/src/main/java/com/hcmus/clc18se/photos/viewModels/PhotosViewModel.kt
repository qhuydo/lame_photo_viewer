package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hcmus.clc18se.photos.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class PhotosViewModel(application: Application) : AndroidViewModel(application) {

    private var _mediaItemList = MutableLiveData<List<MediaItem>>()
    val mediaItemList: LiveData<List<MediaItem>>
        get() = _mediaItemList

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

    init {
        _mediaItemList.value = mutableListOf<MediaItem>()
    }

    private var contentObserver: ContentObserver? = null

    fun setCurrentItemView(newIdx: Int) {
        _mediaItemList.value?.let {
            if (newIdx in it.indices) {
                _idx.value = newIdx
            }
        }
    }

    fun loadImages() {
        viewModelScope.launch {
            val images = queryMediaItems()
            _mediaItemList.postValue(images)

            if (contentObserver == null) {
                val observer = object : ContentObserver(Handler()) {
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

    private suspend fun queryMediaItems(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()

        withContext(Dispatchers.IO) {

            val projection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                arrayOf(MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.Images.ImageColumns.ORIENTATION
                )
            } else {
                arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                )
            }

            val selection = MediaStore.Images.Media.DATE_ADDED
            val selectionArgs: Array<String>? = null
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

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

                    val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id)

                    val image = MediaItem(id, displayName, uri, dateAdded, mimeType, orientation, null)
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