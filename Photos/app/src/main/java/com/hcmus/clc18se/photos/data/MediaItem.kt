package com.hcmus.clc18se.photos.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.recyclerview.widget.DiffUtil
import com.caverock.androidsvg.SVG
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.adapters.AdapterItem
import com.hcmus.clc18se.photos.utils.*
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTimeComparator
import org.joda.time.DateTimeFieldType
import timber.log.Timber
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class MediaItem(
    val id: Long,
    val name: String,
    private var uri: Uri?,
    private var dateSorted: Date?,
    val mimeType: String?,
    var orientation: Int?,
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
        return mimeType?.startsWith("video") ?: false
    }

    fun isEditable() = isSupportedStaticImage()

    fun requireUri(): Uri {
        uri?.let {
            return it
        } ?: return uriFromMimeType(mimeType, id)
    }

    fun requirePath(context: Context): String? {
        if (path == null) {
            try {
                path = getRealPathFromURI(context, requireUri())
            } catch (ex: Exception) {
                Timber.e("$ex")
            }
        }
        return path
    }

    fun getDateSorted(): Date? {
        return dateSorted
    }

    fun toFavouriteItem(): FavouriteItem {
        return FavouriteItem(id)
    }

    companion object {

        fun getInstance(
            id: Long,
            name: String,
            uri: Uri?,
            mimeType: String,
            path: String? = null
        ): MediaItem {
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

        fun bindSvgToSubScaleImageView(item: MediaItem, imageView: SubsamplingScaleImageView) {
            try {
                item.requireUri().let { imageView.context.contentResolver.openInputStream(it) }
                    .use {
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

                        imageView.setImage(ImageSource.bitmap(bitmap))

                    }
            } catch (ex: FileNotFoundException) {
                Timber.d("$ex")
            }
        }

        fun uriFromMimeType(mimeType: String?, id: Long): Uri {
            val contentUri = when {
                checkImageMimeType(mimeType) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                checkVideoMimeType(mimeType) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }

            // id = 33
            // MediaStore.Files.getContentUri("external") -> content://media/external/file/33
            // MediaStore.Images.Media.EXTERNAL_CONTENT_URI -> content://media/external/video/media/33
            return ContentUris.withAppendedId(contentUri, id)
        }

        /**
         * Insert `AdapterItem.AdapterItemHeader` object to group the list into sections,
         * The function is used in [MediaItemListAdapter.ActionCallbacks::onGroupListItem]
         * when submit the data to `MediaItemListAdapter`.
         * @param items a list of MediaItems that have their `dateSorted` attribute sorted in a
         *        particular order.
         * @see DateUtils.formatDateTime
         */
        private fun groupBy(
            context: Context,
            items: List<MediaItem>,
            headerLabelMap: (date: Date) -> String,
            dateComparator: Comparator<Date>
        ): List<AdapterItem> {
            val adapterItems = mutableListOf<AdapterItem>()
            if (items.isEmpty()) {
                return emptyList()
            }

            // Construct the first header item from the list
            var headerItem = items.first().getDateSorted()?.let {
                AdapterItem.AdapterItemHeader(it.time, headerLabelMap(it))
            } ?: return items.map { AdapterItem.AdapterMediaItem(it) }

            var headerDate = items.first().getDateSorted()
            items.forEachIndexed { index, mediaItem ->

                if (index == 0) {
                    adapterItems.add(headerItem)
                }

                val date = mediaItem.getDateSorted()
                date?.let {

                    if (dateComparator.compare(it, headerDate) != 0) {
                        headerDate = it
                        headerItem = AdapterItem.AdapterItemHeader(
                            date.time,
                            headerLabelMap(it)
                        )

                        adapterItems += headerItem
                    }

                } ?: return adapterItems + items.subList(index, items.lastIndex)
                    .map { AdapterItem.AdapterMediaItem(it) }

                adapterItems += AdapterItem.AdapterMediaItem(mediaItem)
            }
            return adapterItems
        }

        fun groupByDate(context: Context, items: List<MediaItem>): List<AdapterItem> {
            val flags = DateUtils.FORMAT_SHOW_YEAR or
                    DateUtils.FORMAT_ABBREV_MONTH or
                    DateUtils.FORMAT_SHOW_DATE
            val headerLabelMap = { date: Date ->
                DateUtils.formatDateTime(context, date.time, flags)
            }

            val comparator = kotlin.Comparator<Date> { date1, date2 ->
                DateTimeComparator.getInstance(DateTimeFieldType.dayOfMonth()).compare(date1, date2)
            }

            return groupBy(
                context,
                items,
                headerLabelMap,
                comparator
            )
        }

        fun groupByMonth(context: Context, items: List<MediaItem>): List<AdapterItem> {
            // only shows month & year
            val flags = DateUtils.FORMAT_SHOW_YEAR or
                    DateUtils.FORMAT_ABBREV_MONTH or
                    DateUtils.FORMAT_NO_MONTH_DAY

            val headerLabelMap = { date: Date ->
                DateUtils.formatDateTime(context, date.time, flags)
            }

            val comparator = kotlin.Comparator<Date> { date1, date2 ->
                DateTimeComparator.getInstance(DateTimeFieldType.monthOfYear())
                    .compare(date1, date2)
            }

            return groupBy(
                context,
                items,
                headerLabelMap,
                comparator
            )
        }

        fun groupByYear(context: Context, items: List<MediaItem>): List<AdapterItem> {
            // only shows year in the header
            val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            val headerLabelMap = { date: Date -> simpleDateFormat.format(date) }

            // Compare the date without month & day of month
            val comparator = kotlin.Comparator<Date> { date1, date2 ->
                DateTimeComparator.getInstance(DateTimeFieldType.year()).compare(date1, date2)
            }

            return groupBy(
                context,
                items,
                headerLabelMap,
                comparator
            )
        }

        fun getSecretName(mediaItem: MediaItem): String {
            val nameWithoutExtension = mediaItem.name.substringBeforeLast(".")
            val extension = mediaItem.name.substringAfterLast(".")

            // missing delimiter value
            if (extension == mediaItem.name) {
                return "${nameWithoutExtension}_${mediaItem.id}"
            }

            return "${nameWithoutExtension}_${mediaItem.id}.$extension"
        }
    }
}