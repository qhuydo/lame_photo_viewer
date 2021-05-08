package com.hcmus.clc18se.photos.adapters

import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.use
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
import com.hcmus.clc18se.photos.utils.getMimeType
import com.hcmus.clc18se.photos.utils.isSVG
import com.hcmus.clc18se.photos.utils.isVideo
import com.l4digital.fastscroll.FastScrollView

@BindingAdapter("mediaListItem")
fun bindMediaListRecyclerView(recyclerView: RecyclerView, data: List<MediaItem>?) {
    data?.let {
        val adapter = recyclerView.adapter as? MediaItemListAdapter
        adapter?.filterAndSubmitList(data)
    }
}

@BindingAdapter("mediaListItem")
fun bindMediaListRecyclerView(fastScrollView: FastScrollView, data: List<MediaItem>?) {
    data?.let {
        val adapter = fastScrollView.recyclerView.adapter as? MediaItemListAdapter
        adapter?.filterAndSubmitList(data)
    }
}

@BindingAdapter("albumListItem")
fun bindSampleAlbumListRecyclerView(recyclerView: RecyclerView, data: List<Album>?) {
    data?.let {
        val adapter = recyclerView.adapter as? AlbumListAdapter
        adapter?.submitList(data)
    }
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
fun bindImage(imgView: ImageView, mediaItem: MediaItem?) = mediaItem?.let {
    bindImage(imgView, it.requireUri(), it.mimeType)
}

fun bindImage(imgView: ImageView, uri: Uri, mimeType: String?) {
    when {
        isSVG(mimeType) -> {

            Glide.with(imgView.context)
                    .`as`(PictureDrawable::class.java)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .listener(SvgSoftwareLayerSetter())
                    .load(uri)
                    .error(R.drawable.ic_launcher_grey_sample)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgView)

        }
        isVideo(mimeType) -> {

            // Timber.d("Load video thumbnail from Glide ")
            // Timber.d("Uri ${mediaItem.requireUri()}")
            // Timber.d("Path ${mediaItem.requirePath(imgView.context)}")

            Glide.with(imgView.context)
                    .asBitmap()
                    .load(uri)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .into(imgView)
        }
        else -> {
            Glide.with(imgView.context)
                    .load(uri)
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

@BindingAdapter(value = ["mediaItem", "debug"], requireAll = false)
fun SubsamplingScaleImageView.bindMediaItem(mediaItem: MediaItem?, debug: Boolean?) {
    mediaItem?.let {
        when {
            it.isSupportedStaticImage() -> {
                bindScaleImage(this, it.requireUri(), debug ?: false)
                visibility = View.VISIBLE
            }
            it.isSVG() -> {
                MediaItem.bindSvgToSubScaleImageView(it, this)
                visibility = View.VISIBLE
            }
            else -> visibility = View.GONE
        }
    }
}

@BindingAdapter("mediaItem")
fun ImageView.setGifOrVideoMediaItem(mediaItem: MediaItem?) {
    mediaItem?.let {
        when {
            it.isGif() || it.isVideo() -> {
                bindImage(this, mediaItem)
                visibility = View.VISIBLE
            }
            else -> visibility = View.GONE
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
        val uri = album.thumbnailUri

        if (uri != null) {
            val mime = getMimeType(image.context, uri)
            bindImage(image, uri, mime)
        } else {
            image.resources.obtainTypedArray(R.array.sample_photos).use { samplePhotos ->
                val sampleResId = samplePhotos.getResourceId(
                        (0 until samplePhotos.length()).random(),
                        R.drawable.ic_launcher_indigo_sample)

                Glide.with(image.context)
                        .load(sampleResId)
                        .into(image)
            }
        }
    }
}

@BindingAdapter("visibleWhenNonNull")
fun View.visibleWhenNonNull(obj: Any?) {
    visibility = if (obj != null) View.VISIBLE else View.GONE
}

@BindingAdapter("visibleWhenEmpty")
fun View.visibleWhenEmpty(obj: List<*>?) {
    visibility = if (obj.isNullOrEmpty()) View.VISIBLE else View.GONE
}

@BindingAdapter("placeHolderEmoticon")
fun TextView.setPlaceHolderEmoticon(nothing: Nothing?) {
    text = context.resources.getStringArray(R.array.emoticons).random()
}