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


//    private val defaultExcludedPaths = arrayOf(
//            context.getExternalFilesDir(Environment.DIRECTORY_ALARMS)?.path,
//            context.getExternalFilesDir(null)?.path + "/Android",
//            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path,
//            context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)?.path)

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    interface OnMediaLoadedCallback {
        fun onMediaLoaded(albums: ArrayList<Album>?)
    }

    fun loadAlbum(onMediaLoadedCallback: OnMediaLoadedCallback) {
        // TODO: check permission
        val albums = ArrayList<Album>()

        val columnUri = MediaStore.Files.getContentUri("external")

        // Return only video and image metadata.
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
        " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"

        val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                //MediaStore.Files.FileColumns.MIME_TYPE,
                //MediaStore.Images.ImageColumns.DATE_TAKEN,
                //MediaStore.Video.VideoColumns.DATE_TAKEN,
                BaseColumns._ID,)

        val selectionArgs: Array<String>? = null

        val sortOrder = MediaStore.Files.FileColumns.DATE_ADDED

        // TODO: checking hidden folders
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
                var dateTaken: Long
                var id: Long
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val idColumn = cursor.getColumnIndex(BaseColumns._ID)

                while (cursor.moveToNext()) {
                    path = cursor.getString(pathColumn)
                    id = cursor.getLong(idColumn)

//                    val itemUri = ContentUris.withAppendedId(
//                            MediaStore.Files.getContentUri("external"), id)

                    val mediaItem = MediaItem.getInstance(path, id)

                    //set dateTaken
//                    val dateTakenColumn = cursor.getColumnIndex(
//                            if (mediaItem?.isSupportedStaticImage() == true) MediaStore.Images.ImageColumns.DATE_TAKEN else MediaStore.Video.VideoColumns.DATE_TAKEN)
//                    dateTaken = cursor.getLong(dateTakenColumn)

//                    mediaItem?.let {
//                        it.path = path
//                        //it.dateCreated = Date(dateTaken)
//                        //it.mimeType = MediaItem.getMimeType(context, itemUri)
//                    }

                    //search bucket
                    var foundBucket = false
                    val bucketPath = File(path).parent
                    bucketPath?.let {
                        if (folderMap.containsKey(bucketPath)) {
                            mediaItem?.let {
                                folderMap[bucketPath]?.mediaItems?.add(0, it)
                            }
                        }
                        else {
                            albums.add(Album(
                                    bucketPath,
                                    mutableListOf()
                            ))

                            mediaItem?.let { albums[albums.size - 1].mediaItems.add(0, it) }
                            folderMap[bucketPath] = albums[albums.size - 1]
                        }
                    }
//                    for (album in albums) {
//                        if (album.path == File(path).parent) {
//                            if (mediaItem != null) {
//                                album.mediaItems.add(0, mediaItem)
//                                foundBucket = true
//                                break
//                            }
//                        }
//                    }

//                    if (!foundBucket) {
//                        //no bucket found
//                        val bucketPath: String? = File(path).parent
//                        if (bucketPath != null) {
//                            albums.add(Album(
//                                    bucketPath,
//                                    mutableListOf()
//                            ))
//                            mediaItem?.let { albums[albums.size - 1].mediaItems.add(0, it) }
//                        }
//                    }
                }

                onAlbumLoaded = true
                _albums = albums
                withContext(Dispatchers.Main) {
                    onMediaLoadedCallback.onMediaLoaded(albums)
                }

                Timber.d("onMediaLoaded(): ${(System.currentTimeMillis() - startTime)} ms")

            } ?: return@launch
        }

    }


}