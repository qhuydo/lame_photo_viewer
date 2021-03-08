package com.hcmus.clc18se.photos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.SamplePhoto
import com.hcmus.clc18se.photos.databinding.ItemPhotoListBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListGridBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ClassCastException

// TODO: simplify the adapter when the header is unused
class PhotoListAdapter(private val adapterViewType: Int = 0) :
        ListAdapter<DataItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        const val ITEM_TYPE_HEADER = -1
        const val ITEM_TYPE_LIST = 0
        const val ITEM_TYPE_THUMBNAIL = 1
    }

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val photo = getItem(position) as DataItem.PhotoItem
                holder.bind(photo)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.HeaderItem -> ITEM_TYPE_HEADER
            else -> adapterViewType
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_HEADER -> HeaderViewHolder.from(parent)
            adapterViewType -> ViewHolder.from(parent, viewType)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    fun addHeaderAndSubmitList(list: List<SamplePhoto>?, hasHeader: Boolean = false) {
        adapterScope.launch {
            val items = when (hasHeader) {
                true -> when (list) {
                    null -> listOf(DataItem.HeaderItem)
                    else -> listOf(DataItem.HeaderItem) + list.map { DataItem.PhotoItem(it) }
                }
                false -> list?.map { DataItem.PhotoItem(it) }
            }

            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    class ViewHolder(private val listBinding: ViewDataBinding) :
            RecyclerView.ViewHolder(listBinding.root) {

        fun bind(item: DataItem.PhotoItem) {
            when (listBinding) {
                is ItemPhotoListBinding -> listBinding.apply {
                    photo = item.photo
                }
                is ItemPhotoListGridBinding -> listBinding.apply {
                    photo = item.photo
                }
            }
            listBinding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup, viewType: Int): ViewHolder {
                return when (viewType) {
                    ITEM_TYPE_LIST -> ViewHolder(
                            ItemPhotoListBinding.inflate(LayoutInflater.from(parent.context))
                    )
                    else -> ViewHolder(
                            ItemPhotoListGridBinding.inflate(LayoutInflater.from(parent.context))
                    )
                }
            }
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view =
                        layoutInflater.inflate(R.layout.item_photo_list_view_option, parent, false)

                return HeaderViewHolder(view)
            }
        }
    }

}

object DiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}

sealed class DataItem {
    abstract val id: String

    data class PhotoItem(val photo: SamplePhoto) : DataItem() {
        override val id: String = photo.name
    }

    object HeaderItem : DataItem() {
        override val id: String = ""
    }
}