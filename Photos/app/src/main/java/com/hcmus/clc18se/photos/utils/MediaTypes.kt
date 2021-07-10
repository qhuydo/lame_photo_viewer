package com.hcmus.clc18se.photos.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.util.*

val svgMimeTypes = arrayOf("image/svg+xml")

val imageMimeTypes = arrayOf(
    "image/*",
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/bmp",
    "image/webp",
)

val videoMimeTypes = arrayOf("video/*",
    "video/mp4",
    "video/x-matroska",
    "video/webm",
    "video/avi"
)

val gifMimeTypes = arrayOf("image/gif")

val exifMimeTypes = arrayOf("image/jpeg", "image/jpg")

val exifWritingMimeTypes = arrayOf("image/jpeg", "image/jpg")

// private val wallpaperMimeTypes = arrayOf("image/jpeg", "image/png")

fun isMedia(mimeType: String): Boolean {
    return checkGifMimeType(mimeType) ||
            checkImageMimeType(mimeType) ||
            checkVideoMimeType(mimeType) ||
            checkSvgMimeType(mimeType)
}

fun getMimeType(path: String?): String? {
    if (path == null) {
        return null
    }
    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(path)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
}

fun getMimeType(context: Context, uri: Uri?): String? {
    return uri?.let {
        return context.contentResolver.getType(it)
    }
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

fun checkSvgMimeType(mimeType: String?): Boolean {
    return checkExtension(mimeType, svgMimeTypes)
}

fun checkGifMimeType(mimeType: String?): Boolean {
    return checkExtension(mimeType, gifMimeTypes)
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

fun getFileName(context: Context, uri: Uri): String? {
    //retrieve file name
    try {
        val cursor = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null
        )
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

// https://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
fun getRealPathFromURI(context: Context, contentUri: Uri?): String? {
    val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
    val cursor = context.contentResolver.query(contentUri!!,
            projection,
            null,
            null,
            null
    )

    return cursor?.use {
        val columnIndex: Int = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        it.moveToFirst()
        it.getString(columnIndex)
    }

}

fun isSVG(mimeType: String?): Boolean {
    return mimeType in svgMimeTypes
}

fun isSupportedStaticImage(mimeType: String): Boolean {
    return mimeType in imageMimeTypes
}

fun isGif(mimeType: String): Boolean {
    return (mimeType in gifMimeTypes)
}

fun isVideo(mimeType: String?): Boolean {
    return (mimeType in videoMimeTypes)
}