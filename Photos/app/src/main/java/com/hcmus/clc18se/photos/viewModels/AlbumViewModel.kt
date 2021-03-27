package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.*

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private var _albumList = MutableLiveData<List<Album>>()
    val albumList: LiveData<List<Album>>
        get() = _albumList

    private var _navigateToPhotoList = MutableLiveData<Album?>(null)
    val navigateToPhotoList: LiveData<Album?> = _navigateToPhotoList

    private var _onAlbumLoaded = MutableLiveData(false)
    val onAlbumLoaded: LiveData<Boolean>
        get() = _onAlbumLoaded

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

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

    fun notifyAlbumLoaded() {
        _albumList.value = MediaProvider.albums
        _onAlbumLoaded.value = true
    }

    fun startNavigatingToPhotoList(album: Album) {
        _navigateToPhotoList.value = album
    }

    fun doneNavigatingToPhotoList() {
        _navigateToPhotoList.value = null
    }
}