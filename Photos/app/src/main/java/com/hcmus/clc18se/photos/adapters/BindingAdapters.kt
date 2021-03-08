package com.hcmus.clc18se.photos.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hcmus.clc18se.photos.data.SampleAlbum
import com.hcmus.clc18se.photos.data.SamplePhoto

@BindingAdapter("samplePhotoListItem")
fun bindSamplePhotoListRecyclerView(recyclerView: RecyclerView, data: List<SamplePhoto>) {
    val adapter = recyclerView.adapter as PhotoListAdapter
    adapter.addHeaderAndSubmitList(data)
}

@BindingAdapter("sampleAlbumListItem")
fun bindSampleAlbumListRecyclerView(recyclerView: RecyclerView, data: List<SampleAlbum>) {
    val adapter = recyclerView.adapter as AlbumListAdapter
    adapter.submitList(data)
}

/**
 * Bind the image binary to the image view from drawable resource id
 */
@BindingAdapter("imageFromResId")
fun bindImage(imgView: ImageView, imgRes: Int?) {
    imgRes?.let {
        Glide.with(imgView.context)
            .load(imgRes)
            .into(imgView)
        // imgView.setImageResource(it)
    }
}
