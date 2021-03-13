package com.hcmus.clc18se.photos.viewModels

import android.content.res.Resources
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.SampleAlbum
import com.hcmus.clc18se.photos.data.SampleMediaItem
import kotlinx.coroutines.launch

class AlbumViewModel(private val resources: Resources) : ViewModel() {
    private var _albumList = MutableLiveData<List<SampleAlbum>>()
    val albumList: LiveData<List<SampleAlbum>>
        get() = _albumList

    private var _navigateToPhotoList = MutableLiveData<SampleAlbum?>(null)
    val navigateToPhotoList: LiveData<SampleAlbum?> = _navigateToPhotoList

    init {
        initList()
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

    private fun initList() {
        viewModelScope.launch {

            val albumList = mutableListOf<SampleAlbum>()

            for (i in 0..5) {
                val name = "${resources.getString(R.string.album_title)} #${i + 1}"
                val resId = photoList[(photoList.indices).random()].resId
                albumList.add(SampleAlbum(resId, name))
            }
            _albumList.value = albumList
        }
    }

    fun startNavigatingToPhotoList(sampleAlbum: SampleAlbum) {
        _navigateToPhotoList.value = sampleAlbum
    }

    fun doneNavigatingToPhotoList() {
        _navigateToPhotoList.value = null
    }

}

class AlbumViewModelFractory(private val resources: Resources) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
            return AlbumViewModel(resources) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
