package com.hcmus.clc18se.photos.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MediaProvider(private val context: Context) {

    companion object {
        private var _albums: ArrayList<Album>? = null
        val albums: ArrayList<Album>?
            get() = _albums
    }

    private var onAlbumLoaded: Boolean = false

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

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
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"

        val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                BaseColumns._ID,
        )

        val selectionArgs: Array<String>? = null

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

                val idColumn = cursor.getColumnIndex(BaseColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    path = cursor.getString(pathColumn)
                    id = cursor.getLong(idColumn)
                    mimeType = cursor.getString(mimeTypeColumn)
                    name = cursor.getString(nameColumn)

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
                _albums = albums

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