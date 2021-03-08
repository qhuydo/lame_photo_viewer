package com.hcmus.clc18se.photos.viewModels

import android.content.res.Resources
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.SampleAlbum
import com.hcmus.clc18se.photos.data.SamplePhoto
import kotlinx.coroutines.launch

class AlbumViewModel(private val resources: Resources) : ViewModel() {
    private var _album_list = MutableLiveData<List<SampleAlbum>>()
    val albumList: LiveData<List<SampleAlbum>>
        get() = _album_list

    init {
        initList()
    }

    private fun initList() {
        viewModelScope.launch {
            val default = SamplePhoto(R.drawable.ic_launcher_sample, "ic_launcher_sample.png")
            val red = SamplePhoto(R.drawable.ic_launcher_red_sample, "ic_launcher_red_sample.png")
            val orange = SamplePhoto(R.drawable.ic_launcher_orange_sample, "ic_launcher_orange_sample.png")
            val yellow = SamplePhoto(R.drawable.ic_launcher_yellow_sample, "ic_launcher_yellow_sample.png")
            val green = SamplePhoto(R.drawable.ic_launcher_green_sample, "ic_launcher_green_sample.png")
            val blue = SamplePhoto(R.drawable.ic_launcher_blue_sample, "ic_launcher_blue_sample.png")
            val indigo = SamplePhoto(R.drawable.ic_launcher_indigo_sample, "ic_launcher_indigo_sample.png")
            val purple = SamplePhoto(R.drawable.ic_launcher_purple_sample, "ic_launcher_purple_sample.png")
            val pink = SamplePhoto(R.drawable.ic_launcher_pink_sample, "ic_launcher_pink_sample.png")
            val brown = SamplePhoto(R.drawable.ic_launcher_brown_sample, "ic_launcher_brown_sample.png")

            val photoList = listOf(default,
                    red,
                    orange,
                    yellow,
                    green,
                    blue,
                    indigo,
                    purple,
                    pink,
                    brown)
            val albumList = mutableListOf<SampleAlbum>()

            for (i in 0..5) {
                val name = "${resources.getString(R.string.album_title)} #${i + 1}"
                val resId = photoList[(photoList.indices).random()].resId
                albumList.add(SampleAlbum(resId, name, listOf<SamplePhoto>()))
            }
            _album_list.value = albumList
        }
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
