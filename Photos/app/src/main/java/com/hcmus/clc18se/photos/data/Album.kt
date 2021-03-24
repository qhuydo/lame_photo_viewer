package com.hcmus.clc18se.photos.data

import android.os.Parcelable
import com.hcmus.clc18se.photos.R
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class Album(
        val path: String,
        val mediaItems: MutableList<MediaItem>,
        val resId: Int = R.drawable.ic_launcher_indigo_sample
): Parcelable {
    fun getName(): String? {
        return File(path).name
    }
}