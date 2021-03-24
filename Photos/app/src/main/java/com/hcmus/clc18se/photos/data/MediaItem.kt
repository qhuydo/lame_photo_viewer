package com.hcmus.clc18se.photos.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
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
        var name: String?,
        private var uri: Uri?,
        private var dateCreated: Date?,
        val mimeType: String?,
        private var orientation: Int?,
        private var type: Int?,
) : Parcelable {

    fun isSVG(): Boolean {
        if (type == null) {
            type = if (mimeType in svgMimeTypes) TYPE_SVG else null
        }
        return type != null
    }

    fun isSupportedStaticImage(): Boolean {
        if (type == null) {
            type = if (mimeType in imageMimeTypes) TYPE_SVG else null
        }
        return type != null
    }

    fun isGif(): Boolean {
        if (type == null) {
            type = if (mimeType in gifMimeTypes) TYPE_SVG else null
        }
        return type != null
    }

    fun isEditable() = isSupportedStaticImage()

    fun requireUri(): Uri {
        uri?.let {
            return it
        }
        val itemUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id)
        uri = itemUri
        return itemUri
    }


    companion object {

        fun getInstance(id: Long, context: Context, uri: Uri?, mimeType: String): MediaItem {
            return MediaItem(id, null, uri, null, mimeType, null, null)
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
                debug: Boolean
        ) {
            mediaItem?.let {
                if (it.isSupportedStaticImage()) {
                    bindScaleImage(subScaleImageView, it.uri, debug)
                    imageView.visibility = View.INVISIBLE
                    subScaleImageView.visibility = View.VISIBLE
                } else if (it.isGif()) {
                    bindImage(imageView, mediaItem)
                    imageView.visibility = View.VISIBLE
                    subScaleImageView.visibility = View.INVISIBLE
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
                }
            }
        }
    }

}

