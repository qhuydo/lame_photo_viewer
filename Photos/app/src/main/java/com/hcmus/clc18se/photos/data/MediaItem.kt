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
import com.hcmus.clc18se.photos.databinding.ItemVideoPagerBinding
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
        } ?: return getMediaUriFromMimeType(mimeType, id)
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
                videoView: ItemVideoPagerBinding,
                debug: Boolean
        ) {
            imageView.visibility = View.INVISIBLE
            subScaleImageView.visibility = View.INVISIBLE
            videoView.imageFrame.visibility = View.INVISIBLE
            videoView.playIcon.visibility = View.INVISIBLE

            mediaItem?.let {
                when {
                    it.isSupportedStaticImage() -> {
                        bindScaleImage(subScaleImageView, it.requireUri(), debug)
                        subScaleImageView.visibility = View.VISIBLE
                    }
                    it.isGif() -> {
                        bindImage(imageView, mediaItem)
                        imageView.visibility = View.VISIBLE
                    }
                    it.isSVG() -> {
                        bindSvgToSubScaleImageView(it, context, subScaleImageView)
                        subScaleImageView.visibility = View.VISIBLE
                    }
                    it.isVideo() -> {
                        videoView.imageFrame.visibility = View.VISIBLE
                        videoView.playIcon.visibility = View.VISIBLE
                        videoView.photo = it
                    }
                }
            }
        }

        private fun bindSvgToSubScaleImageView(it: MediaItem, context: Context, subScaleImageView: SubsamplingScaleImageView) {
            try {
                val inputStream = it.requireUri().let { it1 -> context.contentResolver.openInputStream(it1) }
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
        }

        fun getMediaUriFromMimeType(mimeType: String?, id: Long): Uri {
            val contentUri = when {
                checkImageMimeType(mimeType) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                checkVideoMimeType(mimeType) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }
            return ContentUris.withAppendedId(contentUri, id)
        }
    }
}
