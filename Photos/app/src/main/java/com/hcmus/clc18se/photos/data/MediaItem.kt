package com.hcmus.clc18se.photos.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.recyclerview.widget.DiffUtil
import com.caverock.androidsvg.SVG
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.adapters.bindScaleImage
import com.hcmus.clc18se.photos.utils.*
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.*


@Parcelize
data class MediaItem(
        val id: Long,
        val name: String,
        private var uri: Uri?,
        private var dateTaken: Date?,
        val mimeType: String?,
        private var orientation: Int?,
        private var path: String?,
) : Parcelable {

    fun isSVG(): Boolean {
        return mimeType in svgMimeTypes
    }

    fun isSupportedStaticImage(): Boolean {
        return mimeType in imageMimeTypes
    }

    fun isGif(): Boolean {
        return (mimeType in gifMimeTypes)
    }

    fun isVideo(): Boolean {
        return (mimeType in videoMimeTypes)
    }

    fun isEditable() = isSupportedStaticImage()

    fun requireUri(): Uri {
        uri?.let {
            return it
        }
        val itemUri = ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"),
                id)
        uri = itemUri
        return itemUri
    }

    fun requirePath(context: Context): String? {
        path?.let {
            try {
                path = getRealPathFromURI(context, requireUri())
            } catch (ex: Exception) {
                Timber.e("$ex")
            }
        }
        return path
    }

    fun requireDateTaken(): Date? {
        return dateTaken
    }

    fun toFavouriteItem(): FavouriteItem {
        return FavouriteItem(id)
    }

//    fun isFavourite(): Boolean {
//        return MediaProvider.favouriteMediaItems?.let {
//            this in it
//        } ?: false
//    }

    companion object {

        fun getInstance(id: Long, name: String, uri: Uri?, mimeType: String, path: String? = null): MediaItem {
            return MediaItem(id, name, uri, null, mimeType, null, path)
        }

        val DiffCallBack = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem == newItem
            }
        }

        /**
         * Bind a media item to one of give image view
         * @param subScaleImageView: set the image to this object when isSupportedImage() is true
         * @param imageView: set the image using glide in other cases
         */
        fun bindMediaItemToImageDrawable(
                context: Context,
                subScaleImageView: SubsamplingScaleImageView,
                imageView: ImageView,
                mediaItem: MediaItem?,
                videoView: VideoView,
                mediaController: MediaController,
                debug: Boolean
        ) {
            // TODO: refactor kudasai, onii-chan :<
            mediaItem?.let {
                if (it.isSupportedStaticImage()) {
                    bindScaleImage(subScaleImageView, it.uri, debug)
                    imageView.visibility = View.INVISIBLE
                    subScaleImageView.visibility = View.VISIBLE
                    videoView.visibility = View.INVISIBLE
                } else if (it.isGif()) {
                    bindImage(imageView, mediaItem)
                    imageView.visibility = View.VISIBLE
                    subScaleImageView.visibility = View.INVISIBLE
                    videoView.visibility = View.INVISIBLE
                } else if (it.isSVG()) {
                    try {
                        val inputStream = it.uri?.let { it1 -> context.contentResolver.openInputStream(it1) }
                        inputStream.use {
                            val svg = SVG.getFromInputStream(it)
                            val picture = svg.renderToPicture()

                            val drawable = PictureDrawable(picture)
                            val bitmap = Bitmap.createBitmap(
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight,
                                    Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            canvas.drawPicture(drawable.picture)

                            subScaleImageView.setImage(ImageSource.bitmap(bitmap))

                        }
                    } catch (ex: FileNotFoundException) {
                        Timber.d("$ex")
                    }

                    imageView.visibility = View.INVISIBLE
                    subScaleImageView.visibility = View.VISIBLE
                    videoView.visibility = View.INVISIBLE
                } else if (it.isVideo()){
                    imageView.visibility = View.INVISIBLE
                    subScaleImageView.visibility = View.INVISIBLE
                    videoView.setVideoURI(it.uri)
                    mediaController.setAnchorView(videoView)
                    videoView.setMediaController(mediaController)
                }
            }
        }
    }

}

