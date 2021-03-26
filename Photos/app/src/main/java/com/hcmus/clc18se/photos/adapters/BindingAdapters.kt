package com.hcmus.clc18se.photos.adapters

import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.samples.svg.SvgSoftwareLayerSetter
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.MediaItem
import timber.log.Timber

@BindingAdapter("mediaListItem")
fun bindMediaListRecyclerView(recyclerView: RecyclerView, data: List<MediaItem>) {
    val adapter = recyclerView.adapter as MediaItemListAdapter
    adapter.submitList(data)
}

@BindingAdapter("albumListItem")
fun bindSampleAlbumListRecyclerView(recyclerView: RecyclerView, data: List<Album>) {
    val adapter = recyclerView.adapter as AlbumListAdapter
    adapter.submitList(data)
}

/**
 * Bind the image binary to the image view from drawable resource id
 */
@BindingAdapter("imageFromResId")
fun bindImage(imgView: ImageView, imgRes: Int?) {
    imgRes?.let {
        val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(imgView.context)
                .load(imgRes)
                .apply(requestOptions)
                .into(imgView)
    }
}

@BindingAdapter("imageFromUri")
fun bindImage(imgView: ImageView, imgUri: Uri?) {

    imgUri?.let {
        val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(imgView.context)
                .load(imgUri)
                .apply(requestOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgView)
    }
}

@BindingAdapter("imageFromBitmap")
fun bindImage(imgView: ImageView, bitmap: Bitmap?) {

    bitmap?.let {
        val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)

        Glide.with(imgView.context)
                .load(bitmap)
                .apply(requestOptions)
                .into(imgView)
    }
}


@BindingAdapter("imageFromMediaItem")
fun bindImage(imgView: ImageView, mediaItem: MediaItem?) {

    // TODO: onii-chan, refactor me!!! UwU
    mediaItem?.let {
        val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)

        if (mediaItem.isSVG()) {
            Glide.with(imgView.context)
                    .`as`(PictureDrawable::class.java)
                    .listener(SvgSoftwareLayerSetter())
                    .load(mediaItem.requireUri())
                    .error(R.drawable.ic_launcher_grey_sample)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgView)

        } else if (mediaItem.isVideo()) {

            // val path = getRealPathFromURI(imgView.context, mediaItem.requireUri())
            // val path = mediaItem.requirePath(imgView.context)
            Timber.d("Load video thumbnail from Glide ")
            Timber.d("Uri ${mediaItem.requireUri()}")
            Timber.d("Path ${mediaItem.requirePath(imgView.context)}")

            Glide.with(imgView.context)
                    .asBitmap()
                    .centerCrop()
                    .load(mediaItem.requireUri())
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .into(imgView)
        } else {
            Glide.with(imgView.context)
                    .load(mediaItem.requireUri())
                    //.apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgView)
        }
    }
}

//@BindingAdapter("subSamplingScaleImageViewFromUri")
fun bindScaleImage(imgView: SubsamplingScaleImageView, imgUri: Uri?, debug: Boolean = false) {
    imgUri?.let {
        imgView.apply {
            setImage(ImageSource.uri(imgUri))
            setDebug(debug)
        }
    }
}

/**
 * Set the video thumbnail visibility based on the type of mediaItem
 */
@BindingAdapter("videoThumbnailVisibility")
fun setVideoVisibility(videoThumbnail: ImageView, mediaItem: MediaItem?) {
    mediaItem?.let {
        videoThumbnail.visibility = if (it.isVideo()) View.VISIBLE else View.INVISIBLE
    }
}

@BindingAdapter("selectThumbnail")
fun selectAlbumThumbnail(image: ImageView, album: Album?) {
    album?.let {
        val mediaItem = album.getRandomMediaItem()
        bindImage(image, mediaItem)
    }
}