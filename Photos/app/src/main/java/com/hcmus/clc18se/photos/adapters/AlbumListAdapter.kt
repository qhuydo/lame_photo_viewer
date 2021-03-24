package com.hcmus.clc18se.photos.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.databinding.*

class AlbumListAdapter(
        private val onClickListener: OnClickListener,
        private val resources: Resources,
        private val adapterItemType: Int = ITEM_TYPE_GRID,
        private val adapterItemSize: Int = ITEM_SIZE_MEDIUM) :
        ListAdapter<Album, AlbumListAdapter.ViewHolder>(DiffCallBack()) {

    companion object {
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_GRID = 1

        const val ITEM_SIZE_BIG = 0
        const val ITEM_SIZE_MEDIUM = 1
        const val ITEM_SIZE_SMALL = 2
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(album)
        }
        holder.bind(album)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.from(parent, resources, viewType, adapterItemSize)

    override fun getItemViewType(position: Int): Int {
        return adapterItemType
    }

    class ViewHolder(private val binding: ViewDataBinding,
                     private val resources: Resources,
                     private val itemSize: Int = ITEM_SIZE_MEDIUM
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: Album) {
            when (binding) {
                is ItemAlbumListBinding -> {
                    setItemListSize(resources, binding.albumListItemImage, itemSize)
                    binding.album = album
                }
                is ItemAlbumListGridBinding -> binding.album = album
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup,
                     resources: Resources,
                     viewType: Int,
                     itemSize: Int = ITEM_SIZE_MEDIUM): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = when (viewType) {
                    ITEM_TYPE_LIST -> ItemAlbumListBinding.inflate(layoutInflater)
                    else -> ItemAlbumListGridBinding.inflate(layoutInflater)
                }
                return ViewHolder(binding, resources, itemSize)
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

    class OnClickListener(val clickListener: (album: Album) -> Unit) {
        fun onClick(album: Album) = clickListener(album)
    }

    class DiffCallBack : DiffUtil.ItemCallback<Album>() {
        override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
            return oldItem.path == newItem.path
        }
    }
}

