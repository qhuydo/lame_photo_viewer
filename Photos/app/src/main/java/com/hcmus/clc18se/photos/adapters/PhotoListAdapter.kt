package com.hcmus.clc18se.photos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.data.SamplePhoto
import com.hcmus.clc18se.photos.databinding.ItemPhotoListBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListThumbnailBinding

class PhotoListAdapter(private val adapterViewType: Int = 0) :
        ListAdapter<SamplePhoto, PhotoListAdapter.ViewHolder>(DiffCallback) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = getItem(position)
        holder.bind(photo)
    }

    override fun getItemViewType(position: Int): Int {
        return adapterViewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.from(parent, viewType)

    class ViewHolder(private val listBinding: ViewDataBinding) : RecyclerView.ViewHolder(listBinding.root) {
        fun bind(item: SamplePhoto) {
            when (listBinding) {
                is ItemPhotoListBinding -> listBinding.apply {
                    photo = item
                }
                is ItemPhotoListThumbnailBinding -> listBinding.apply {
                    photo = item
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup, viewType: Int): ViewHolder {
                return when (viewType) {
                    ITEM_TYPE_LIST -> ViewHolder(
                            ItemPhotoListBinding.inflate(LayoutInflater.from(parent.context))
                    )
                    else -> ViewHolder(
                            ItemPhotoListThumbnailBinding.inflate(LayoutInflater.from(parent.context))
                    )
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SamplePhoto>() {
        override fun areContentsTheSame(oldItem: SamplePhoto, newItem: SamplePhoto): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: SamplePhoto, newItem: SamplePhoto): Boolean {
            return oldItem == newItem
        }

    }
}

const val ITEM_TYPE_LIST = 0
const val ITEM_TYPE_THUMBNAIL = 1
