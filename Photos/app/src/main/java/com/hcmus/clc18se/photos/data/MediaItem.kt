package com.hcmus.clc18se.photos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import com.caverock.androidsvg.SVG
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.adapters.bindScaleImage
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.*

@Parcelize
data class MediaItem(
        val id: Long,
        val name: String,
        var uri: Uri?,
        var dateCreated: Date?,
        var mimeType: String?,
        var orientation: Int?,
        var type: Int? = null,
        var path: String? = null
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

    companion object {

        const val TYPE_SVG = 1
        const val TYPE_IMAGE = 2
        const val TYPE_VIDEO = 4
        const val TYPE_GIF = 16

        val svgExtensions = arrayOf("svg")
        val svgMimeTypes = arrayOf("image/svg+xml")

        val imageExtensions = arrayOf("jpg", "png", "jpe", "jpeg", "bmp")
        val imageMimeTypes = arrayOf("image/*", "image/jpeg", "image/jpg", "image/png", "image/bmp")

        //  arrayOf("mp4", "mkv", "webm", "avi")
        val videoExtensions = arrayOf("mp4", "mkv", "webm", "avi")

        // arrayOf("video/*", "video/mp4", "video/x-matroska", "video/webm", "video/avi")
        val videoMimeTypes = arrayOf("video/*", "video/mp4")

        val gifExtensions = arrayOf("gif")
        val gifMimeTypes = arrayOf("image/gif")

        val exifExtensions = arrayOf("jpg", "jpe", "jpeg", "dng", "cr2")

        // arrayOf("image/jpeg", "image/jpg", "image/x-adobe-dng", "image/x-canon-cr2")
        val exifMimeTypes = arrayOf("image/jpeg", "image/jpg")

        val exifWritingExtensions = arrayOf("jpg", "jpe", "jpeg")
        val exifWritingMimeTypes = arrayOf("image/jpeg", "image/jpg")

        // private val wallpaperMimeTypes = arrayOf("image/jpeg", "image/png")

        fun isMedia(path: String): Boolean {
            return checkImageExtension(path) ||
                    checkGifExtension(path) ||
                    checkVideoExtension(path)
        }

        fun getMimeType(path: String?): String? {
            if (path == null) {
                return null
            }
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(path)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        }

        fun getMimeType(context: Context, uri: Uri?): String? {
            return context.contentResolver.getType(uri!!)
        }

        //trying to check via mimeType
        fun isImage(path: String?): Boolean {
            return path != null && checkImageExtension(path)
        }

        fun isSvg(path: String?): Boolean {
            return path != null && checkSvgExtension(path)
        }

        fun isVideo(path: String?): Boolean {
            return path != null && checkVideoExtension(path)
        }

        fun isGif(path: String?): Boolean {
            return path != null && checkGifExtension(path)
        }

        /*check mimeTypes*/
        fun doesSupportExifMimeType(mimeType: String?): Boolean {
            return checkExtension(mimeType, exifMimeTypes)
        }

        fun doesSupportWritingExifMimeType(mimeType: String?): Boolean {
            return checkExtension(mimeType, exifWritingMimeTypes)
        }

        fun checkImageMimeType(mimeType: String?): Boolean {
            return checkExtension(mimeType, imageMimeTypes)
        }

        fun checkVideoMimeType(mimeType: String?): Boolean {
            return checkExtension(mimeType, videoMimeTypes)
        }

        fun checkGifMimeType(mimeType: String?): Boolean {
            return checkExtension(mimeType, gifMimeTypes)
        }

        /*check fileExtensions*/
        fun doesSupportExifFileExtension(path: String?): Boolean {
            return checkExtension(path, exifExtensions)
        }

        fun doesSupportWritingExifFileExtension(path: String?): Boolean {
            return checkExtension(path, exifWritingExtensions)
        }

        private fun checkImageExtension(path: String): Boolean {
            return checkExtension(path, imageExtensions)
        }

        private fun checkSvgExtension(path: String): Boolean {
            return checkExtension(path, svgExtensions)
        }

        private fun checkVideoExtension(path: String): Boolean {
            return checkExtension(path, videoExtensions)
        }

        private fun checkGifExtension(path: String): Boolean {
            return checkExtension(path, gifExtensions)
        }

        private fun checkExtension(path: String?, extensions: Array<String>): Boolean {
            if (path == null) {
                return false
            }
            for (i in extensions) {
                if (path.toLowerCase(Locale.ROOT).endsWith(i)) {
                    return true
                }
            }
            return false
        }

        fun getInstance(path: String?, id: Long): MediaItem? {
            var mediaItem: MediaItem? = null

            path?.let {
                if (isMedia(path)) {
                    mediaItem = MediaItem(
                            id,
                            File(path).name,
                            null,
                            null,
                            null,
                            null,
                            path = path
                    )
                }
            }

            return mediaItem
        }

        fun getInstance(context: Context?, uri: Uri?, id: Long): MediaItem? {
            if (uri == null) {
                return null
            }
            val mimeType: String? = context?.let { getMimeType(it, uri) }
            return getInstance(context, uri, mimeType, id)
        }

        fun getInstance(context: Context?, uri: Uri?, mimeType: String?, id: Long): MediaItem? {
            if (uri == null) {
                return null
            }
            return context?.let {
                MediaItem(
                        id,
                        getFileName(context, uri) ?: "N/A",
                        uri,
                        null,
                        mimeType,
                        null,
                )
            }
        }

        fun getFileName(context: Context, uri: Uri?): String? {
            //retrieve file name
            try {
                val cursor = context.contentResolver.query(uri!!, arrayOf(OpenableColumns.DISPLAY_NAME),
                        null, null, null)
                if (cursor != null) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (!cursor.isAfterLast) {
                        val filename = cursor.getString(nameIndex)
                        cursor.close()
                        return filename
                    }
                }
            } catch (ignored: SecurityException) {
            }
            return null
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
