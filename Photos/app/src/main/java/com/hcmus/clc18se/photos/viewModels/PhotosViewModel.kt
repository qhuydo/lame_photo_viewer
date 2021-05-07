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
import com.hcmus.clc18se.photos.data.*
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.IndexOutOfBoundsException

class PhotosViewModel(
        application: Application,
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

    private var _navigateToImageView = MutableLiveData<MediaItem?>(null)
    val navigateToImageView: LiveData<MediaItem?>
        get() = _navigateToImageView

    init {
        // _mediaItemList.value = mutableListOf()
        // loadImages()
    }

    private var contentObserver: ContentObserver? = null

    private var _customAlbum = MutableLiveData<Album>(null)
    val customAlbum: LiveData<Album>
        get() = _customAlbum

    fun setCustomAlbum(album: Album) {
        if (album.customAlbumId != null) {
            _customAlbum.postValue(album)
        }
    }

    fun setCurrentItemView(newIdx: Int) {
        _mediaItemList.value?.let {
            if (newIdx in it.indices) {
                _idx.postValue(newIdx)
            }
        }
    }

    // wtf is this?
    fun loadDataFromOtherViewModel(other: AndroidViewModel) {
        when (other) {
            is PhotosViewModel -> _mediaItemList.value = other._mediaItemList.value
            is FavouriteAlbumViewModel -> _mediaItemList.value = other.mediaItems.value
            // is CustomAlbumViewModel -> _mediaItemList.value = other.selectedAlbum.value?.mediaItems
        }
    }

    fun loadImages() {
        viewModelScope.launch {
            val images = queryMediaItems()
            withContext(Dispatchers.Main) {
                _mediaItemList.value = images
            }

            registerObserverWhenNull()
        }
    }

    fun loadSelectedPhotoList() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _customAlbum.value?.let { album ->
                val customAlbumItems = database.getCustomAlbum(album.customAlbumId!!).albumItems
                val mediaItems = getMediaItemListFromCustomAlbumItem(customAlbumItems)
                _mediaItemList.postValue(mediaItems)
            }
        }
    }

    private suspend fun getMediaItemListFromCustomAlbumItem(items: List<CustomAlbumItem>?): List<MediaItem> {
        if (items == null) {
            return emptyList()
        }
        return getApplication<Application>().applicationContext.contentResolver.loadMediaItemFromIds(
                items.map { it.id }
        )

    }

    private fun registerObserverWhenNull() {
        if (contentObserver == null) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    loadImages()
                }
            }
            val contentResolver = getApplication<Application>().contentResolver
            contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
            )
            contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer
            )

            contentObserver = observer
        }
    }

    fun loadImages(bucketId: Long, bucketName: String) {
        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            val images = contentResolver.queryMediaItemsFromBucketName(bucketId, bucketName)
            withContext(Dispatchers.Main) {
                _mediaItemList.value = images
            }
            registerObserverWhenNull()
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
                        null,
                        null)

                Timber.d("Delete result - $result columns affected")

                if (result > 0) {
                    withContext(Dispatchers.Main) {
                        _mediaItemList.value = _mediaItemList.value?.toMutableList()?.apply {
                            remove(item)
                        }
                        _deleteSucceed.value = true
                    }
                }

            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                            securityException as? RecoverableSecurityException
                                    ?: throw securityException

                    pendingDeleteImage = item

                    _permissionNeededForDelete.postValue(recoverableSecurityException.userAction.actionIntent.intentSender)

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
        val mediaItems = getApplication<Application>().contentResolver.queryAllMediaItems()
        Timber.i("Found ${mediaItems.size} images")
        return mediaItems
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

    fun startNavigatingToImageView(item: MediaItem) {
        _navigateToImageView.postValue(item)
    }

    fun doneNavigatingToImageView() {
        _navigateToImageView.value = null
    }

    fun insertPhotosIntoSelectedAlbum(mediaItems: List<MediaItem>) {
        if (customAlbum.value == null
                || (customAlbum.value != null
                        && customAlbum.value!!.customAlbumId == null)
        ) {
            return
        }
        viewModelScope.launch {
            val albumId = customAlbum.value!!.customAlbumId!!
            val customAlbumItems = mediaItems.map {
                CustomAlbumItem(id = it.id, albumId = albumId)
            }
            database.addMediaItemToCustomAlbum(*customAlbumItems.toTypedArray())
            // requestReloadingData()
            loadSelectedPhotoList()
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