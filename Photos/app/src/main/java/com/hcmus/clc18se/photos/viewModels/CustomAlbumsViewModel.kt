package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.*
import com.hcmus.clc18se.photos.database.PhotosDatabaseDao
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

//    private var _selectedAlbum = MutableLiveData<Album?>(null)
//    val selectedAlbum: LiveData<Album?>
//        get() = _selectedAlbum

//    private var _selectedPhotoList = MutableLiveData<List<MediaItem>?>(null)
//    val selectedPhotoList: LiveData<List<MediaItem>?>
//        get() = _selectedPhotoList

    internal fun loadData() = viewModelScope.launch {
        loadAlbumsFromDatabase()
    }

    private suspend fun loadAlbumsFromDatabase() {
        Timber.d("Start loading CustomAlbums from the database")
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            val customAlbums = database.getAllCustomAlbums().map {
                val name = it.albumInfo.name
                val id = it.albumInfo.id
                return@map Album(path = "",
                        name = name,
                        customAlbumId = id,
                        thumbnailUri = getFirstMediaItem(it.albumItems.randomOrNull())?.requireUri()
                )
            }

            withContext(Dispatchers.Main) {
                _albums.value = customAlbums

//                if (selectedAlbum.value != null) {
//                    _selectedAlbum.value = _albums.value?.first {
//                        it.customAlbumId == selectedAlbum.value?.customAlbumId
//                    }
//                }

                Timber.d("loadData(): ${(System.currentTimeMillis() - startTime)} ms")
            }
        }
    }

    private suspend fun getFirstMediaItem(item: CustomAlbumItem?): MediaItem? {
        if (item == null) {
            return null
        }
        return getApplication<Application>().applicationContext.contentResolver.loadMediaItemFromId(
                item.id
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
//        _selectedAlbum.value = album
        _navigateToPhotoList.value = album
//        loadSelectedPhotoList()
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