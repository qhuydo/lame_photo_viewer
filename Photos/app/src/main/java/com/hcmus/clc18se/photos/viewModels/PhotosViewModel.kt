package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.IndexOutOfBoundsException
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

    private var pendingDeleteImage: MediaItem? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    private var _deleteSucceed = MutableLiveData<Boolean?>(null)
    val deleteSucceed: LiveData<Boolean?>
        get() = _deleteSucceed

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

    private var _itemFavouriteStateChanged = MutableLiveData<Boolean?>(null)
    val itemFavouriteStateChanged: LiveData<Boolean?>
        get() = _itemFavouriteStateChanged

    /**
     * Change the state of is_favourite attribute in a MediaItem (true -> false or false -> true)
     * @param itemPosition: index of the item in the mediaItemList
     */
    fun changeFavouriteState(itemPosition: Int) {
        try {
            val mediaItem = _mediaItemList.value?.get(itemPosition)
            mediaItem?.let {
                viewModelScope.launch {
                    val isFavourite = database.hasFavouriteItem(mediaItem.id)
                    if (isFavourite) {
                        Timber.d("MediaItem ID{${mediaItem.id}} was removed from favourites")
                        database.removeFavouriteItems(mediaItem.toFavouriteItem())
                    } else {
                        Timber.d("MediaItem ID{${mediaItem.id}} was added to favourites")
                        database.addFavouriteItems(mediaItem.toFavouriteItem())
                    }
                    _itemFavouriteStateChanged.value = !isFavourite
                }
            } ?: Timber.e("media Item not found")
        } catch (ex: IndexOutOfBoundsException) {
            Timber.e("$ex")
        }
    }

    fun finishChangingFavouriteItemState() {
        _itemFavouriteStateChanged.value = null
    }

    fun deleteImage(image: MediaItem) {
        viewModelScope.launch {
            performDeletingImage(image)
        }
    }

    private suspend fun performDeletingImage(item: MediaItem) {
        withContext(Dispatchers.IO) {
            try {
                val result = getApplication<Application>().contentResolver.delete(
                        item.requireUri(),
                        "${MediaStore.Images.Media._ID} = ?",
                        arrayOf(item.id.toString())
                )
                Timber.d("Delete result - $result columns affected")

                _deleteSucceed.postValue(result > 0)

            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                            securityException as? RecoverableSecurityException
                                    ?: throw securityException

                    pendingDeleteImage = item
                    withContext(Dispatchers.Main) {
                        _permissionNeededForDelete.value = recoverableSecurityException.userAction.actionIntent.intentSender
                    }
                } else {
                    throw securityException
                }
            }
        }
    }

    fun deletePendingImage() {
        pendingDeleteImage?.let {
            pendingDeleteImage = null
            deleteImage(it)
        }
    }

    // TODO: get me a better name
    fun finishPerformingDelete() {
        _deleteSucceed.value = null
    }

    private suspend fun queryMediaItems(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        withContext(Dispatchers.IO) {

            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(MediaStore.MediaColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.Images.ImageColumns.ORIENTATION
                )
            } else {
                arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                )
            }
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                    " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
            // val selection = MediaStore.Images.Media.DATE_ADDED
            val selectionArgs: Array<String>? = null
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            val columnUri = MediaStore.Files.getContentUri("external")
            //val columnUri = MediaStore.Images.Media.INTERNAL_CONTENT_URI

            getApplication<Application>().contentResolver.query(
                    columnUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateAdded = Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val orientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION))
                    } else {
                        0
                    }

                    val path = cursor.getString(pathColumn)
                    // val uri = MediaItem.getMediaUriFromMimeType(mimeType, id)

                    val image = MediaItem(id, displayName, null, dateAdded, mimeType, orientation, path)
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