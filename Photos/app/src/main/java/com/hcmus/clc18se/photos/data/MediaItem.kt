package com.hcmus.clc18se.photos.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import com.caverock.androidsvg.SVG
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.adapters.bindScaleImage
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.*

@Parcelize
data class MediaItem(
        val id: Long,
        val name: String,
        val uri: Uri,
        val dateCreated: Date,
        val mimeType: String,
        val orientation: Int
) : Parcelable {

    fun isSVG() = mimeType == TYPE_SVG

    fun isSupportedStaticImage() = (mimeType == TYPE_JPEG || mimeType == TYPE_PNG)

    fun isGif() = mimeType == TYPE_GIF

    fun isEditable() = isSupportedStaticImage()

    companion object {

        const val TYPE_JPEG = "image/jpeg"
        const val TYPE_PNG = "image/png"
        const val TYPE_SVG = "image/svg+xml"
        const val TYPE_GIF = "image/gif"

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
                fragment: Fragment,
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
                }
                else if (it.isGif()) {
                    bindImage(imageView, mediaItem)
                    imageView.visibility = View.VISIBLE
                    subScaleImageView.visibility = View.INVISIBLE
                }
                else if (it.isSVG()) {
                    try {
                        val inputStream = fragment.requireContext().contentResolver.openInputStream(it.uri)
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