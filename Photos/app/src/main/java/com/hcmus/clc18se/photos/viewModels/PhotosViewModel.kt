package com.hcmus.clc18se.photos.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.SamplePhoto

class PhotosViewModel : ViewModel() {

    private var _photoList = MutableLiveData<List<SamplePhoto>>()
    val photoList: LiveData<List<SamplePhoto>>
        get() = _photoList

    companion object {
        private val samplePhoto = listOf(
                SamplePhoto(R.drawable.ic_launcher_sample, "ic_launcher_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_red_sample, "ic_launcher_red_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_orange_sample, "ic_launcher_orange_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_yellow_sample, "ic_launcher_yellow_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_green_sample, "ic_launcher_green_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_blue_sample, "ic_launcher_blue_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_indigo_sample, "ic_launcher_indigo_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_purple_sample, "ic_launcher_purple_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_pink_sample, "ic_launcher_pink_sample.png"),
                SamplePhoto(R.drawable.ic_launcher_brown_sample, "ic_launcher_brown_sample.png"),
        )
    }

    init {
        _photoList.value = samplePhoto
    }
}