package com.hcmus.clc18se.photos.data

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// TODO: deprecate this?
@Deprecated("Memory leak")
class MediaProvider(private val context: Context) {

    companion object {
        // TODO: get this variable a better name
        private var privateAlbums: ArrayList<Album>? = null
        val albums: ArrayList<Album>?
            get() = privateAlbums
    }

    private var onAlbumLoaded: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Default)

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    interface MediaProviderCallBack {
        fun onMediaLoaded(albums: ArrayList<Album>?)

        fun onHasNoPermission() {}
    }

    fun loadAlbum(callback: MediaProviderCallBack) {

        if (!hasPermission()) {
            Timber.i("Did not have storage permission")
            callback.onHasNoPermission()
        }

        val albums = ArrayList<Album>()

        val columnUri = MediaStore.Files.getContentUri("external")

        // Return only video and image metadata.
        val selection = DEFAULT_MEDIA_ITEM_SELECTION
        val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                BaseColumns._ID,
        )
        val selectionArgs: Array<String>? = DEFAULT_MEDIA_ITEM_SELECTION_ARGS
        val sortOrder = MediaStore.Files.FileColumns.DATE_ADDED
        val folderMap = HashMap<String, Album>()

        scope.launch {
            Timber.d("Start scanning Album")
            context.contentResolver.query(
                    columnUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            )?.use { cursor ->

                val startTime = System.currentTimeMillis()

                var path: String
                var id: Long
                var mimeType: String
                var name: String
                var dateAdded: Date

                val idColumn = cursor.getColumnIndex(BaseColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    path = cursor.getString(pathColumn)
                    id = cursor.getLong(idColumn)
                    mimeType = cursor.getString(mimeTypeColumn)
                    name = cursor.getString(nameColumn)
                    dateAdded = Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateAddedColumn)))

                    val mediaItem = MediaItem.getInstance(id, name, null, mimeType, path)

                    //search bucket
                    val bucketPath = File(path).parent
                    bucketPath?.let {
                        if (folderMap.containsKey(bucketPath)) {
                            folderMap[bucketPath]?.mediaItems?.add(0, mediaItem)
                        } else {
                            albums.add(Album(
                                    bucketPath,
                                    mutableListOf()
                            ))

                            albums[albums.size - 1].mediaItems.add(0, mediaItem)
                            folderMap[bucketPath] = albums[albums.size - 1]
                        }
                    }
                }

                onAlbumLoaded = true
                privateAlbums = albums

                // TODO: select sort type
                albums.sortBy { album -> album.getName()?.toLowerCase(Locale.ROOT) }
                withContext(Dispatchers.Main) {
                    callback.onMediaLoaded(albums)
                }

                Timber.d("onMediaLoaded(): ${(System.currentTimeMillis() - startTime)} ms")

            } ?: return@launch
        }

    }

}

val DEFAULT_MEDIA_ITEM_PROJECTION = arrayOf(
        MediaStore.Files.FileColumns.DATA,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        BaseColumns._ID,
)
val DEFAULT_MEDIA_ITEM_SELECTION_ARGS: Array<String>? = null
const val DEFAULT_MEDIA_ITEM_SELECTION = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
        "=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
        " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})" +
        " AND ${BaseColumns._ID}"

fun ContentResolver.loadMediaItemFromId(itemId: Long): MediaItem? {
    val columnUri = MediaStore.Files.getContentUri("external")

    // Return only video and image metadata.
    val selection = "$DEFAULT_MEDIA_ITEM_SELECTION=$itemId"
    val projection = DEFAULT_MEDIA_ITEM_PROJECTION
    val selectionArgs: Array<String>? = DEFAULT_MEDIA_ITEM_SELECTION_ARGS

    query(columnUri,
            projection,
            selection,
            selectionArgs,
            null
    )?.use { cursor ->

        val path: String
        val id: Long
        val mimeType: String
        val name: String

        val idColumn = cursor.getColumnIndex(BaseColumns._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

        if (cursor.moveToNext()) {
            path = cursor.getString(pathColumn)
            id = cursor.getLong(idColumn)
            mimeType = cursor.getString(mimeTypeColumn)
            name = cursor.getString(nameColumn)

            return MediaItem.getInstance(id, name, null, mimeType, path)
        }

    }
    return null
}
