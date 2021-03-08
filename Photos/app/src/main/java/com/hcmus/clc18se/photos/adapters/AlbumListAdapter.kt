package com.hcmus.clc18se.photos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.data.SampleAlbum
import com.hcmus.clc18se.photos.databinding.*

class AlbumListAdapter(private val adapterItemType: Int = 0) :
        ListAdapter<SampleAlbum, AlbumListAdapter.ViewHolder>(DiffCallBack) {

    companion object {
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_THUMBNAIL = 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = getItem(position)
        holder.bind(album)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.from(parent, viewType)

    override fun getItemViewType(position: Int): Int {
        return adapterItemType
    }

    class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(a: SampleAlbum) {
            when (binding) {
                is ItemAlbumListBinding -> binding.album = a
                is ItemAlbumListGridBinding -> binding.album = a
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup, viewType: Int): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = when (viewType) {
                    ITEM_TYPE_LIST -> ItemAlbumListBinding.inflate(layoutInflater)
                    else -> ItemAlbumListGridBinding.inflate(layoutInflater)
                }
                return ViewHolder(binding)
            }
        }
    }
}

object DiffCallBack : DiffUtil.ItemCallback<SampleAlbum>() {
    override fun areContentsTheSame(oldItem: SampleAlbum, newItem: SampleAlbum): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: SampleAlbum, newItem: SampleAlbum): Boolean {
        return oldItem.name == newItem.name
    }
}