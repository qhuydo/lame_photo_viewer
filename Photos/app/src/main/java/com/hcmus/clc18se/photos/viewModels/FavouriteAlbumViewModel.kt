package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.data.loadMediaItemFromId
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
        return getApplication<Application>().applicationContext.contentResolver.loadMediaItemFromId(itemId)
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