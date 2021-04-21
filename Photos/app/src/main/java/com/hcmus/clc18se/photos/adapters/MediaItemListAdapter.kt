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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaItemListAdapter(private val actionCallbacks: ActionCallbacks,
                           private val adapterViewType: Int = ITEM_TYPE_GRID,
                           private val itemViewSize: Int = ITEM_SIZE_MEDIUM) :
        ListAdapter<AdapterItem, RecyclerView.ViewHolder>(AdapterItem.DiffCallBack) {

    companion object {
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_GRID = 1

        const val ITEM_SIZE_BIG = 0
        const val ITEM_SIZE_MEDIUM = 1
        const val ITEM_SIZE_SMALL = 2
    }

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    private var multiSelect = false
    private val selectedItems = arrayListOf<MediaItem>()

    fun numberOfSelectedItems(): Int {
        return selectedItems.size
    }

    fun filterAndSubmitList(items: List<MediaItem>) {
        adapterScope.launch {
            val list = actionCallbacks.onGroupListItem(items)
            withContext(Dispatchers.Main) {
                submitList(list)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = getItem(position)
        when (holder) {
            is MediaItemViewHolder -> {
                bindMediaItemViewHolder(holder, (adapterItem as AdapterItem.AdapterMediaItem).mediaItem)
            }
            is MediaHeaderViewHolder -> {
                holder.bind(adapterItem as AdapterItem.AdapterItemHeader)
            }
        }
    }

    private fun bindMediaItemViewHolder(holderMediaItem: MediaItemViewHolder, item: MediaItem) {
        holderMediaItem.bind(item)

        holderMediaItem.itemView.setOnLongClickListener {
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


        if (holderMediaItem.itemView is Checkable) {
            (holderMediaItem.itemView as Checkable).isChecked = selectedItems.contains(item)
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
                holderMediaItem.itemView.isChecked = false
            }
        } else {
            selectedItems.add(item)
            if (holderMediaItem.itemView is MaterialCardView) {
                holderMediaItem.itemView.isChecked = true
            }
        }
    }

    fun finishSelection() {
        multiSelect = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    interface ActionCallbacks {

        fun onClick(mediaItem: MediaItem)
        fun onSelectionChange()

        suspend fun onGroupListItem(items: List<MediaItem>): List<AdapterItem> {
            return items.map { AdapterItem.AdapterMediaItem(it) }
        }
    }
}

class MediaItemViewHolder(private val listBinding: ViewDataBinding,
                          private val itemViewSize: Int = MediaItemListAdapter.ITEM_SIZE_MEDIUM) :
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
        fun from(parent: ViewGroup,
                 viewType: Int,
                 itemViewSize: Int = MediaItemListAdapter.ITEM_SIZE_MEDIUM): MediaItemViewHolder {
            return when (viewType) {
                MediaItemListAdapter.ITEM_TYPE_LIST -> MediaItemViewHolder(
                        ItemPhotoListBinding.inflate(LayoutInflater.from(parent.context)),
                        itemViewSize
                )
                else -> MediaItemViewHolder(
                        ItemPhotoListGridBinding.inflate(LayoutInflater.from(parent.context)),
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

class MediaHeaderViewHolder(val binding: ItemPhotoHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(itemHeader: AdapterItem.AdapterItemHeader) {
        binding.header = itemHeader
    }

    companion object {
        fun from(parent: ViewGroup): MediaHeaderViewHolder {
            return MediaHeaderViewHolder(
                    ItemPhotoHeaderBinding.inflate(LayoutInflater.from(parent.context))
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
            val title: String) : AdapterItem() {
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
