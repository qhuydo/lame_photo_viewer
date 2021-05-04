package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.*
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CustomAlbumViewModel(
        application: Application,
        private val database: PhotosDatabaseDao
) : AndroidViewModel(application) {

    private var _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>>
        get() = _albums

//    private var _customAlbums = MutableLiveData<List<CustomAlbum>>()
//    val customAlbum: LiveData<List<CustomAlbum>>
//        get() = _customAlbums

    private var _navigateToPhotoList = MutableLiveData<Album?>(null)
    val navigateToPhotoList: LiveData<Album?> = _navigateToPhotoList

    private var _selectedAlbum = MutableLiveData<Album?>(null)
    val selectedAlbum: LiveData<Album?>
        get() = _selectedAlbum

    internal fun loadData() = CoroutineScope(Dispatchers.IO).launch {
        loadAlbumsFromDatabase()
    }

    private suspend fun loadAlbumsFromDatabase() {
        Timber.d("Start loading CustomAlbums from the database")
        val startTime = System.currentTimeMillis()

        val customAlbums = database.getAllCustomAlbums().map {
            val name = it.albumInfo.name
            val id = it.albumInfo.id
            val mediaItems = getMediaItemListFromCustomAlbumItem(it.albumItems)
            return@map Album(path = "", mediaItems = mediaItems, name = name, customAlbumId = id)
        }

        withContext(Dispatchers.Main) {
            _albums.value = customAlbums

            if (selectedAlbum.value != null) {
                _selectedAlbum.value = _albums.value?.first {
                    it.customAlbumId == selectedAlbum.value?.customAlbumId
                }
            }

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
        return getApplication<Application>().applicationContext.contentResolver.loadMediaItemFromId(
                itemId
        )
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

    fun startNavigatingToPhotoList(album: Album) {
        _selectedAlbum.postValue(album)
        _navigateToPhotoList.postValue(album)
    }

    fun doneNavigatingToPhotoList() {
        _navigateToPhotoList.value = null
    }

    suspend fun insertNewAlbum(name: String): Album {
        val id = database.addNewCustomAlbum(CustomAlbumInfo(name = name))
        loadAlbumsFromDatabase()
        return albums.value!!.first { album -> album.getName() == name }
    }

    suspend fun containsAlbumName(name: String): Boolean {
        return database.containsCustomAlbumName(name)
    }

    // TODO: fix this inefficient
    fun insertPhotosIntoSelectedAlbum(mediaItems: List<MediaItem>) {
        if (selectedAlbum.value == null
                || (selectedAlbum.value != null
                        && selectedAlbum.value!!.customAlbumId == null)
        ) {
            return
        }
        viewModelScope.launch {
            val albumId = selectedAlbum.value!!.customAlbumId!!
            val customAlbumItems = mediaItems.map {
                CustomAlbumItem(id = it.id, albumId = albumId)
            }
            database.addMediaItemToCustomAlbum(*customAlbumItems.toTypedArray())
            // requestReloadingData()
            loadData()
        }
    }
}

@Suppress("UNCHECKED_CAST")
class CustomAlbumViewModelFactory(
        private val application: Application,
        private val database: PhotosDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomAlbumViewModel::class.java)) {
            return CustomAlbumViewModel(application, database) as T
        }
        throw Exception()
    }
}