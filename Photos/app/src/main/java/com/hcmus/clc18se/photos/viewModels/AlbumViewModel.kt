package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.*

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private var _albumList = MutableLiveData<List<Album>>()
    val albumList: LiveData<List<Album>>
        get() = _albumList

    private var _navigateToPhotoList = MutableLiveData<Album?>(null)
    val navigateToPhotoList: LiveData<Album?> = _navigateToPhotoList

    private var _onAlbumLoaded = MutableLiveData<Boolean>(false)
    val onAlbumLoaded: LiveData<Boolean>
        get() = _onAlbumLoaded

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

    fun getSelectedAlbum(): Album? {
        return _albumList.value?.get(idx.value ?: 0)
    }

    fun setCurrentItemView(newIdx: Int) {
        _albumList.value?.let {
            if (newIdx in it.indices) {
                _idx.value = newIdx
            }
        }
    }


    fun notifyAlbumLoaded() {
        _onAlbumLoaded.value = true
        _albumList.value = MediaProvider.albums
    }

    companion object {
        private val default = SampleMediaItem(R.drawable.ic_launcher_sample, "ic_launcher_sample.png")
        private val red = SampleMediaItem(R.drawable.ic_launcher_red_sample, "ic_launcher_red_sample.png")
        private val orange = SampleMediaItem(R.drawable.ic_launcher_orange_sample, "ic_launcher_orange_sample.png")
        private val yellow = SampleMediaItem(R.drawable.ic_launcher_yellow_sample, "ic_launcher_yellow_sample.png")
        private val green = SampleMediaItem(R.drawable.ic_launcher_green_sample, "ic_launcher_green_sample.png")
        private val blue = SampleMediaItem(R.drawable.ic_launcher_blue_sample, "ic_launcher_blue_sample.png")
        private val indigo = SampleMediaItem(R.drawable.ic_launcher_indigo_sample, "ic_launcher_indigo_sample.png")
        private val purple = SampleMediaItem(R.drawable.ic_launcher_purple_sample, "ic_launcher_purple_sample.png")
        private val pink = SampleMediaItem(R.drawable.ic_launcher_pink_sample, "ic_launcher_pink_sample.png")
        private val brown = SampleMediaItem(R.drawable.ic_launcher_brown_sample, "ic_launcher_brown_sample.png")

        private val photoList = listOf(default,
                red,
                orange,
                yellow,
                green,
                blue,
                indigo,
                purple,
                pink,
                brown)
    }

    fun startNavigatingToPhotoList(album: Album) {
        _navigateToPhotoList.value = album
    }

    fun doneNavigatingToPhotoList() {
        _navigateToPhotoList.value = null
    }
}