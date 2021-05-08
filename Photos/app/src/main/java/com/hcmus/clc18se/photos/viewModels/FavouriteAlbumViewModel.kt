package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.FavouriteItem
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.data.loadMediaItemFromIds
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    init {
        loadData()
    }

    fun loadData() = viewModelScope.launch {
        withContext(Dispatchers.IO) {

            Timber.d("Start loading FavouriteMediaItems")
            val startTime = System.currentTimeMillis()

            val favourites = database.getAllFavouriteItems()
            val favouriteMediaItems = loadMediaItemFromIds(favourites)

            _mediaItems.postValue(favouriteMediaItems)
            Timber.d("loadFavouriteMediaItems(): ${(System.currentTimeMillis() - startTime)} ms")
        }
    }


    suspend fun addToFavouriteAlbum(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val favouriteItems = items.map { it.toFavouriteItem() }
        database.addFavouriteItems(*favouriteItems.toTypedArray())
        requestReloadingData()
    }

    suspend fun removeFromFavourite(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val favouriteItems = items.map { it.toFavouriteItem() }
        database.removeFavouriteItems(*favouriteItems.toTypedArray())
        requestReloadingData()
    }

    private suspend fun loadMediaItemFromIds(favourites: List<FavouriteItem>) =
            getApplication<Application>().applicationContext.contentResolver.loadMediaItemFromIds(
                    favourites.map { it.id }
            )

    fun requestReloadingData() {
        _reloadDataRequest.postValue(true)
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