package com.hcmus.clc18se.photos.adapters

import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.data.SampleAlbum

@BindingAdapter("mediaListItem")
fun bindMediaListRecyclerView(recyclerView: RecyclerView, data: List<MediaItem>) {
    val adapter = recyclerView.adapter as MediaItemListAdapter
    adapter.submitList(data)
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

@BindingAdapter("imageFromUri")
fun bindImage(imgView: ImageView, imgUri: Uri?) {
    imgUri?.let {
        Glide.with(imgView.context)
                .load(imgUri)
                .into(imgView)
    }
}