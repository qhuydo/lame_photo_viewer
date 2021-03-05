package com.hcmus.clc18se.photos.viewModels

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.SamplePhoto

class PhotosViewModel : ViewModel() {

    private var _photoList = MutableLiveData<List<SamplePhoto>>()
    val photoList: LiveData<List<SamplePhoto>>
        get() = _photoList

    init {
        _photoList.value = listOf(
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png"),
                SamplePhoto(R.mipmap.ic_launcher, "Blue.png")
        )
    }
}