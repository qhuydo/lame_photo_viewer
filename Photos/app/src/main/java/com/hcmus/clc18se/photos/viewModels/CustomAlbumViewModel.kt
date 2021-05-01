package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.CustomAlbumItem
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.data.loadMediaItemFromId
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.launch
import timber.log.Timber

class CustomAlbumViewModel(
        application: Application,
        private val database: PhotosDatabaseDao
) : AndroidViewModel(application) {

    private var _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>>
        get() = _albums

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            Timber.d("Start loading CustomAlbums from the database")
            val startTime = System.currentTimeMillis()

            val customAlbums = database.getAllCustomAlbums().map {
                val name = it.albumInfo.name
                val mediaItems = getMediaItemListFromCustomAlbumItem(it.albumItems)
                return@map Album(path = "", mediaItems = mediaItems, name = name)
            }

            _albums.value = customAlbums

            Timber.d("loadData(): ${(System.currentTimeMillis() - startTime)} ms")

        }
    }

    private suspend fun getMediaItemListFromCustomAlbumItem(items: List<CustomAlbumItem>): MutableList<MediaItem> {
        val mediaItems: MutableList<MediaItem> = mutableListOf()
        items.forEach { item ->
            val mediaItem = loadMediaItemFromId(item.id)
            mediaItem?.let { mediaItems.add(it) } ?: database.removeCustomAlbumItem(item)
        }
        return mediaItems
    }

    private fun loadMediaItemFromId(itemId: Long): MediaItem? {
        return getApplication<Application>().applicationContext.contentResolver.loadMediaItemFromId(itemId)
    }

    private var _reloadDataRequest = MutableLiveData(false)
    val reloadDataRequest: LiveData<Boolean>
        get() = _reloadDataRequest

    fun requestReloadingData() {
        _reloadDataRequest.value = true
    }

    fun doneRequestingLoadData() {
        _reloadDataRequest.value = false
    }
}

@Suppress("UNCHECKED_CAST")
class CustomAlbumViewModelFactory(
        private val application: Application,
        private val database: PhotosDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomAlbumViewModelFactory::class.java)) {
            return CustomAlbumViewModelFactory(application, database) as T
        }
        throw Exception()
    }
}