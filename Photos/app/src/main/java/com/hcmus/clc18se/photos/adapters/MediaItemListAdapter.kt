package com.hcmus.clc18se.photos.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.card.MaterialCardView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.ItemPhotoHeaderBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListGridBinding
import com.l4digital.fastscroll.FastScroller
import kotlinx.coroutines.*

class MediaItemListAdapter(
        private val actionCallbacks: ActionCallbacks,
        private val adapterViewType: Int = ITEM_TYPE_GRID,
        private val itemViewSize: Int = ITEM_SIZE_MEDIUM,
        private val enforceMultiSelection: Boolean = false
) : ListAdapter<AdapterItem, RecyclerView.ViewHolder>(AdapterItem.DiffCallBack),
        FastScroller.SectionIndexer {

    companion object {
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_GRID = 1

        const val ITEM_SIZE_BIG = 0
        const val ITEM_SIZE_MEDIUM = 1
        const val ITEM_SIZE_SMALL = 2

        const val GROUP_BY_DATE = 0
        const val GROUP_BY_MONTH = 1
        const val GROUP_BY_YEAR = 2
        const val DEFAULT_GROUP_BY = GROUP_BY_DATE
    }

    private val adapterScope = CoroutineScope(Dispatchers.Default + Job())

    private var multiSelect = false
    private val selectedItems = mutableListOf<MediaItem>()

    private var headerIndices = mutableListOf<Int>()
    private var headers = arrayOf<AdapterItem.AdapterItemHeader>()

    fun numberOfSelectedItems(): Int {
        return selectedItems.size
    }

    fun getSelectedItems() = selectedItems

    fun filterAndSubmitList(items: List<MediaItem>) = adapterScope.launch {
        val list = actionCallbacks.onGroupListItem(items)
        val headerList = mutableListOf<AdapterItem.AdapterItemHeader>()

        headerIndices.clear()
        list.forEachIndexed { index, adapterItem ->
            if (adapterItem is AdapterItem.AdapterItemHeader) {
                headerIndices.add(index)
                headerList.add(adapterItem)
            }
        }
        headers = headerList.toTypedArray()

        withContext(Dispatchers.Main) {
            submitList(list)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = getItem(position)
        when (holder) {
            is MediaItemViewHolder -> {
                bindMediaItemViewHolder(
                        holder,
                        (adapterItem as AdapterItem.AdapterMediaItem).mediaItem
                )
            }
            is MediaHeaderViewHolder -> {
                holder.bind(adapterItem as AdapterItem.AdapterItemHeader)
            }
        }
    }

//    private fun bindHeaderViewHolder(holder: MediaHeaderViewHolder, adapterItem: AdapterItem?) {
//        holder.bind(adapterItem as AdapterItem.AdapterItemHeader)
//
//        holder.itemView.setOnLongClickListener {
//            if (enforceMultiSelection) {
//                return@setOnLongClickListener false
//            }
//
//            if (!multiSelect) {
//                multiSelect = true
//                // selectItem(holderMediaItem, item)
//
//                val pos = holder.bindingAdapterPosition
//
//                for (i in pos until currentList.size) {
//
//                    val item = getItem(i)
//                    if (item is AdapterItem.AdapterItemHeader) {
//                        break
//                    }
//
//                    if (item is AdapterItem.AdapterMediaItem) {
//                        if (!selectedItems.contains(item.mediaItem)) {
//                            selectedItems.add(item.mediaItem)
//                        }
//                    }
//                }
//
//                if (holder.itemView is Checkable) {
//                    holder.itemView.isChecked = true
//                }
//
//                actionCallbacks.onSelectionChange()
//            }
//
//            true
//        }
//
//    }

    private fun bindMediaItemViewHolder(holderMediaItem: MediaItemViewHolder, item: MediaItem) {
        if (enforceMultiSelection && !multiSelect) {
            multiSelect = true
        }

        holderMediaItem.bind(item)

        setListeners(holderMediaItem, item)

        if (holderMediaItem.itemView is Checkable) {
            (holderMediaItem.itemView as Checkable).isChecked = selectedItems.contains(item)
        }
    }

    private fun setListeners(
            holderMediaItem: MediaItemViewHolder,
            item: MediaItem
    ) {

        holderMediaItem.itemView.setOnLongClickListener {
            if (enforceMultiSelection) {
                return@setOnLongClickListener false
            }

            if (!multiSelect) {
                multiSelect = true
                selectItem(holderMediaItem, item)
                actionCallbacks.onSelectionChange()
            }
            true
        }

        holderMediaItem.itemView.setOnClickListener {
            if (multiSelect) {
                selectItem(holderMediaItem, item)
                actionCallbacks.onSelectionChange()
            } else {
                actionCallbacks.onClick(item)
            }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        if (holder is MediaHeaderViewHolder) {
            (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let {
                it.isFullSpan = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            adapterViewType -> {
                MediaItemViewHolder.from(parent, viewType, itemViewSize)
            }
            else -> MediaHeaderViewHolder.from(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AdapterItem.AdapterMediaItem -> {
                adapterViewType
            }
            else -> R.layout.item_photo_header
        }
    }

    // helper function that adds/removes an item to the list depending on the app's state
    private fun selectItem(holderMediaItem: MediaItemViewHolder, item: MediaItem) {

        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            if (holderMediaItem.itemView is MaterialCardView) {
                (holderMediaItem.itemView as MaterialCardView).isChecked = false
            }
        } else {
            selectedItems.add(item)
            if (holderMediaItem.itemView is MaterialCardView) {
                (holderMediaItem.itemView as MaterialCardView).isChecked = true
            }
        }
    }

    fun finishSelection() {
        multiSelect = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    suspend fun selectAll() {
        withContext(adapterScope.coroutineContext) {

            multiSelect = true
            selectedItems.clear()
            selectedItems.addAll(currentList
                    .filterIsInstance<AdapterItem.AdapterMediaItem>()
                    .map { it.mediaItem }
            )

            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    interface ActionCallbacks {

        fun onClick(mediaItem: MediaItem)
        fun onSelectionChange()

        suspend fun onGroupListItem(items: List<MediaItem>): List<AdapterItem> {
            return items.map { AdapterItem.AdapterMediaItem(it) }
        }
    }

    override fun getSectionText(position: Int): CharSequence {
        // 0 3 18 21
        // 1 -> 0
        // 4 -> 3
        // 2 -> 0
        val lastIdx = headerIndices.indexOfFirst { index -> position <= index }
        // Timber.d("pos = $position, lastIdx = $lastIdx")
        val idx = if (lastIdx == -1) {
            headerIndices.last()
        }
        else if (lastIdx - 1 > 0) {
            headerIndices[lastIdx - 1]
        }
        else {
            headerIndices[0]
        }

        return (getItem(idx) as? AdapterItem.AdapterItemHeader)?.title ?: ""
    }
}

class MediaItemViewHolder(
        private val listBinding: ViewDataBinding,
        private val itemViewSize: Int = MediaItemListAdapter.ITEM_SIZE_MEDIUM
) :
        RecyclerView.ViewHolder(listBinding.root) {

    fun bind(item: MediaItem) {
        when (listBinding) {
            is ItemPhotoListBinding -> listBinding.apply {
                setItemListSize(itemView.context.resources, imageFrame, itemViewSize)
                photo = item
            }
            is ItemPhotoListGridBinding -> listBinding.apply {
                photo = item
            }
        }

        listBinding.executePendingBindings()
    }

    companion object {
        fun from(
                parent: ViewGroup,
                viewType: Int,
                itemViewSize: Int = MediaItemListAdapter.ITEM_SIZE_MEDIUM
        ): MediaItemViewHolder {
            return when (viewType) {
                MediaItemListAdapter.ITEM_TYPE_LIST -> MediaItemViewHolder(
                        ItemPhotoListBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        ),
                        itemViewSize
                )
                else -> MediaItemViewHolder(
                        ItemPhotoListGridBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                        ),
                        itemViewSize
                )
            }
        }

        fun setItemListSize(resources: Resources, item: View, itemSize: Int) {
            val layoutParams = item.layoutParams
            layoutParams.width = when (itemSize) {
                MediaItemListAdapter.ITEM_SIZE_BIG -> resources.getDimensionPixelSize(R.dimen.photo_list_item_size_big)
                MediaItemListAdapter.ITEM_SIZE_MEDIUM -> resources.getDimensionPixelSize(R.dimen.photo_list_item_size_medium)
                MediaItemListAdapter.ITEM_SIZE_SMALL -> resources.getDimensionPixelSize(R.dimen.photo_list_item_size_small)
                else -> layoutParams.width
            }
        }
    }
}

class MediaHeaderViewHolder(val binding: ItemPhotoHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

    fun bind(itemHeader: AdapterItem.AdapterItemHeader) {
        binding.header = itemHeader
    }

    companion object {
        fun from(parent: ViewGroup): MediaHeaderViewHolder {
            return MediaHeaderViewHolder(
                    ItemPhotoHeaderBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                    )
            )
        }
    }
}

sealed class AdapterItem {
    abstract val id: Long

    data class AdapterMediaItem(val mediaItem: MediaItem) : AdapterItem() {
        override val id: Long = mediaItem.id
    }

    data class AdapterItemHeader(
            val timeStamp: Long,
            val title: String
    ) : AdapterItem() {
        override val id: Long = timeStamp * -1
    }

    companion object {
        val DiffCallBack = object : DiffUtil.ItemCallback<AdapterItem>() {
            override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}
