package com.hcmus.clc18se.photos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.data.SamplePhoto
import com.hcmus.clc18se.photos.databinding.PhotoListItemBinding

class PhotoListAdapter :
        ListAdapter<SamplePhoto, PhotoListAdapter.ViewHolder>(DiffCallback) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = getItem(position)
        holder.bind(photo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder.from(parent)

    class ViewHolder(private val binding: PhotoListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SamplePhoto) {
            binding.apply {
                photo = item
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                return ViewHolder(
                        PhotoListItemBinding.inflate(LayoutInflater.from(parent.context))
                )
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