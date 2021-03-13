package com.hcmus.clc18se.photos.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.SampleMediaItem

class PhotosViewModel : ViewModel() {

    private var _mediaItemList = MutableLiveData<List<SampleMediaItem>>()
    val mediaItemList: LiveData<List<SampleMediaItem>>
        get() = _mediaItemList

    private var _idx = MutableLiveData(0)
    val idx: LiveData<Int>
        get() = _idx

    companion object {
        private val samplePhoto = listOf(
                SampleMediaItem(R.drawable.ic_launcher_sample, "ic_launcher_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_red_sample, "ic_launcher_red_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_orange_sample, "ic_launcher_orange_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_yellow_sample, "ic_launcher_yellow_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_green_sample, "ic_launcher_green_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_blue_sample, "ic_launcher_blue_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_indigo_sample, "ic_launcher_indigo_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_purple_sample, "ic_launcher_purple_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_pink_sample, "ic_launcher_pink_sample.png"),
                SampleMediaItem(R.drawable.ic_launcher_brown_sample, "ic_launcher_brown_sample.png"),
        )
    }

    init {
        _mediaItemList.value = samplePhoto
    }

    fun setCurrentItemView(newIdx: Int) {
        _mediaItemList.value?.let {
            if (newIdx in it.indices) {
                _idx.value = newIdx
            }
        }
    }
}