package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private var _albumList = MutableLiveData<List<Album>>()
    val albumList: LiveData<List<Album>>
        get() = _albumList

    private var _navigateToPhotoList = MutableLiveData<Album?>(null)
    val navigateToPhotoList: LiveData<Album?> = _navigateToPhotoList

//    private var _onAlbumLoaded = MutableLiveData(false)
//    val onAlbumLoaded: LiveData<Boolean>
//        get() = _onAlbumLoaded

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

    private var contentObserver: ContentObserver? = null

    fun getSelectedAlbum(): Album? {
        // return _albumList.value?.get(idx.value ?: 0)
        return navigateToPhotoList.value ?: _albumList.value?.get(idx.value ?: 0)
    }

    fun setCurrentItemView(newIdx: Int) {
        _albumList.value?.let {
            if (newIdx in it.indices) {
                _idx.value = newIdx
            }
        }
    }

//    fun notifyAlbumLoaded() {
//        _albumList.value = MediaProvider.albums
//        _onAlbumLoaded.value = true
//    }

    fun startNavigatingToPhotoList(album: Album) {
        _navigateToPhotoList.postValue(album)
    }

    fun doneNavigatingToPhotoList() {
        _navigateToPhotoList.value = null
    }

    fun loadAlbums() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _albumList.postValue(getApplication<Application>().contentResolver.loadAlbums())
            if (contentObserver == null) {
                registerObserverWhenNull()
            }
        }
    }

    private fun registerObserverWhenNull() {
        if (contentObserver == null) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    loadAlbums()
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

    override fun onCleared() {
        super.onCleared()
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

}