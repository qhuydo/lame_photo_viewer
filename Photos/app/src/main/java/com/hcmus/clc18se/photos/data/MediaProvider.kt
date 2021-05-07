package com.hcmus.clc18se.photos.data

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.hcmus.clc18se.photos.data.MediaItem.Companion.uriFromMimeType
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

val DEFAULT_MEDIA_ITEM_PROJECTION = arrayOf(
        MediaStore.Files.FileColumns.DATA,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        MediaStore.MediaColumns.DATE_ADDED,
        BaseColumns._ID,
)
val DEFAULT_MEDIA_ITEM_SELECTION_ARGS: Array<String>? = null
const val DEFAULT_MEDIA_ITEM_SELECTION = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
        "=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
        " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})" +
        " AND ${BaseColumns._ID}=?"

val DEFAULT_CONTENT_URI: Uri = MediaStore.Files.getContentUri("external")
val DEFAULT_SORT_ORDER: String? = null

suspend fun ContentResolver.loadAlbums(): List<Album> = withContext(Dispatchers.IO) {

    val columnUri = MediaStore.Files.getContentUri("external")

    // Return only video and image metadata.
    val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
            "=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
            "=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"

    val projection = arrayOf(
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
            BaseColumns._ID,
    )
    val selectionArgs: Array<String>? = DEFAULT_MEDIA_ITEM_SELECTION_ARGS
    val sortOrder = MediaStore.Files.FileColumns.DATE_ADDED
    val folderMap = HashMap<Long, Album>()
    val albums = mutableListOf<Album>()

    Timber.d("Start scanning Album")

    query(columnUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
    )?.use { cursor ->

        val startTime = System.currentTimeMillis()

        val idColumn = cursor.getColumnIndex(BaseColumns._ID)
        val pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val bucketPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)

        while (cursor.moveToNext()) {
            val path = cursor.getString(pathColumn)

            // val mediaItem = MediaItem.getInstance(id, name, null, mimeType, path)

            val bucketPath = cursor.getStringOrNull(bucketPathColumn) ?: File(path).parent
            val bucketId = cursor.getLong(bucketIdColumn)

            if (!folderMap.containsKey(bucketId)) {
                val mediumId = cursor.getLong(idColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                albums.add(Album(
                        path = File(path).absolutePath,
                        name = bucketPath,
                        bucketId = bucketId,
                        thumbnailUri = uriFromMimeType(mimeType, mediumId)
                ))
                folderMap[bucketId] = albums[albums.size - 1]
            }
        }

        // TODO: select sort type
        Timber.d("onMediaLoaded(): ${(System.currentTimeMillis() - startTime)} ms")
    }
    return@withContext albums
}

suspend fun ContentResolver.loadMediaItemFromId(itemId: Long): MediaItem? = withContext(Dispatchers.IO) {
    val items = queryMediaItemsWithIdList(selectionArgs = arrayOf("$itemId"))
    return@withContext items?.firstOrNull()
}

suspend fun ContentResolver.loadMediaItemFromIds(itemIds: List<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
    val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
            "=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
    return@withContext queryMediaItemsWithIdList(selection = selection, itemsIds = itemIds)
            ?: listOf()
}

suspend fun ContentResolver.queryAllMediaItems(): MutableList<MediaItem> = withContext(Dispatchers.IO) {
    val mediaItems = mutableListOf<MediaItem>()
    val projection = DEFAULT_MEDIA_ITEM_PROJECTION

    val selection = MediaStore.Files.FileColumns.MEDIA_TYPE +
            "=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
            "=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"

    val selectionArgs: Array<String>? = null
    val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

    val columnUri = MediaStore.Files.getContentUri("external")

    query(columnUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
    )?.use { cursor ->

        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val dateAdded = Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateAddedCol)))
            val displayName = cursor.getString(nameCol)
            val mimeType = cursor.getString(mimeCol)

            val path = cursor.getString(pathCol)
            // val uri = MediaItem.getMediaUriFromMimeType(mimeType, id)

            val image = MediaItem(id,
                    displayName,
                    null,
                    dateAdded,
                    mimeType,
                    null,
                    path)
            mediaItems += image
        }

        Timber.i("Found ${cursor.count} images")
    }
    return@withContext mediaItems
}

suspend fun ContentResolver.queryMediaItemsFromBucketName(
        bucketId: Long,
        bucketName: String): List<MediaItem> = withContext(Dispatchers.IO) {

    val mediaItems = mutableListOf<MediaItem>()
    val projection = DEFAULT_MEDIA_ITEM_PROJECTION

    val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
            " = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
            " = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})" +
            " AND ${MediaStore.Files.FileColumns.BUCKET_ID} = $bucketId"

    val selectionArgs = null
    val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

    val columnUri = MediaStore.Files.getContentUri("external")

    query(columnUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
    )?.use { cursor ->

        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val dateAdded = Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateAddedCol)))
            val displayName = cursor.getString(nameCol)
            val mimeType = cursor.getString(mimeCol)

            val path = cursor.getString(pathCol)
            // val uri = MediaItem.getMediaUriFromMimeType(mimeType, id)

            val image = MediaItem(id,
                    displayName,
                    null,
                    dateAdded,
                    mimeType,
                    null,
                    path)
            mediaItems += image
        }

        Timber.i("Found ${cursor.count} images")
    }

    return@withContext mediaItems
}

// TODO: document me!
private fun ContentResolver.queryMediaItemsWithIdList(
        columnUri: Uri = DEFAULT_CONTENT_URI,
        projection: Array<String>? = DEFAULT_MEDIA_ITEM_PROJECTION,
        selection: String? = DEFAULT_MEDIA_ITEM_SELECTION,
        selectionArgs: Array<String>? = DEFAULT_MEDIA_ITEM_SELECTION_ARGS,
        sortOrder: String? = DEFAULT_SORT_ORDER,
        itemsIds: List<Long>? = null
): List<MediaItem>? {

    query(columnUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
    )?.use { cursor ->

        val idColumn = cursor.getColumnIndex(BaseColumns._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

        val list = mutableListOf<MediaItem>()
        val itemIdSet = itemsIds?.toSet()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)

            if (itemIdSet != null && id !in itemIdSet) {
                continue
            }

            val path = cursor.getString(pathColumn)
            val mimeType = cursor.getString(mimeTypeColumn)
            val name = cursor.getString(nameColumn)

            list.add(MediaItem.getInstance(id, name, null, mimeType, path))
        }
        return list
    }
    return null
}

suspend fun ContentResolver.deleteMultipleMediaItems(list: List<MediaItem>) = withContext(Dispatchers.IO) {

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {

        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}" +
                "=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
                " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"

        val ids = list.map { it.id }

        query(MediaStore.Files.getContentUri("external"),
                listOf(BaseColumns._ID).toTypedArray(),
                selection,
                null,
                null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val idx = ids.indexOf(id)
                if (idx != -1) {
                    delete(list[idx].requireUri(), null, null)
                }
            }

        }
    }
}