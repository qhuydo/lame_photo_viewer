package com.hcmus.clc18se.photos.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.databinding.ItemAlbumListBinding
import com.hcmus.clc18se.photos.databinding.ItemAlbumListGridBinding

class AlbumListAdapter(
    private val adapterItemType: Int = ITEM_TYPE_GRID,
    private val adapterItemSize: Int = ITEM_SIZE_MEDIUM,
    private val allowLongClick: Boolean = false,
    private val callbacks: AlbumListAdapterCallbacks,
) : ListAdapter<Album, AlbumListAdapter.ViewHolder>(Album.DiffCallBack()) {

    companion object {
        // TODO: change constants value to layout id
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_GRID = 1

        const val ITEM_SIZE_BIG = 0
        const val ITEM_SIZE_MEDIUM = 1
        const val ITEM_SIZE_SMALL = 2
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = getItem(position)
        holder.itemView.setOnClickListener {
            callbacks.onClick(album)
        }

        if (allowLongClick) {
            holder.itemView.setOnLongClickListener {
                callbacks.onLongClick(album)
                true
            }
        }
        holder.bind(album)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.from(parent, viewType, adapterItemSize)

    override fun getItemViewType(position: Int): Int {
        return adapterItemType
    }

    class ViewHolder(
        val binding: ViewDataBinding,
        private val itemSize: Int = ITEM_SIZE_MEDIUM
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: Album) {
            when (binding) {
                is ItemAlbumListBinding -> {
                    setItemListSize(itemView.resources, binding.albumListItemImage, itemSize)
                    binding.album = album
                }
                is ItemAlbumListGridBinding -> binding.album = album
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(
                parent: ViewGroup,
                viewType: Int,
                itemSize: Int = ITEM_SIZE_MEDIUM
            ): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = when (viewType) {
                    ITEM_TYPE_LIST -> ItemAlbumListBinding.inflate(layoutInflater)
                    else -> ItemAlbumListGridBinding.inflate(layoutInflater)
                }
                return ViewHolder(binding, itemSize)
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

    interface AlbumListAdapterCallbacks {
        fun onClick(album: Album)
        fun onLongClick(album: Album) {}
    }
}

