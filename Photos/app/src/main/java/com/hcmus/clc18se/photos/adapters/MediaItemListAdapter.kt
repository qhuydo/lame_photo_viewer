package com.hcmus.clc18se.photos.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.ItemPhotoListBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListGridBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MediaItemListAdapter(private val resources: Resources,
                           private val onClickListener: OnClickListener,
                           private val adapterViewType: Int = ITEM_TYPE_GRID,
                           private val itemViewSize: Int = ITEM_SIZE_MEDIUM) :
        ListAdapter<MediaItem, RecyclerView.ViewHolder>(MediaItem.DiffCallBack) {

    companion object {
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_GRID = 1

        const val ITEM_SIZE_BIG = 0
        const val ITEM_SIZE_MEDIUM = 1
        const val ITEM_SIZE_SMALL = 2
    }

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val photo = getItem(position) as MediaItem
                holder.bind(photo)
                holder.itemView.setOnClickListener { onClickListener.onClick(photo) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder.from(parent, viewType, resources, itemViewSize)
    }

    class ViewHolder(private val listBinding: ViewDataBinding,
                     private val resources: Resources,
                     private val itemViewSize: Int = ITEM_SIZE_MEDIUM) :
            RecyclerView.ViewHolder(listBinding.root) {

        fun bind(item: MediaItem) {
            when (listBinding) {
                is ItemPhotoListBinding -> listBinding.apply {
                    setItemListSize(resources, photoListItemImage, itemViewSize)
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
                     resources: Resources,
                     itemViewSize: Int = ITEM_SIZE_MEDIUM): ViewHolder {
                return when (viewType) {
                    ITEM_TYPE_LIST -> ViewHolder(
                            ItemPhotoListBinding.inflate(LayoutInflater.from(parent.context)),
                            resources,
                            itemViewSize
                    )
                    else -> ViewHolder(
                            ItemPhotoListGridBinding.inflate(LayoutInflater.from(parent.context)),
                            resources,
                            itemViewSize
                    )
                }
            }

            fun setItemListSize(resources: Resources, item: ImageView, itemSize: Int) {
                val layoutParams = item.layoutParams
                layoutParams.width = when (itemSize) {
                    ITEM_SIZE_BIG -> resources.getDimensionPixelSize(R.dimen.photo_list_item_size_big)
                    ITEM_SIZE_MEDIUM -> resources.getDimensionPixelSize(R.dimen.photo_list_item_size_medium)
                    ITEM_SIZE_SMALL -> resources.getDimensionPixelSize(R.dimen.photo_list_item_size_small)
                    else -> layoutParams.width
                }
            }
        }
    }

    class OnClickListener(val clickListener: (mediaItem: MediaItem) -> Any) {
        fun onClick(mediaItem: MediaItem) = clickListener(mediaItem)
    }
}