package com.hcmus.clc18se.photos.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.ItemPhotoListBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListGridBinding

class MediaItemListAdapter(private val activity: AppCompatActivity,
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

    // private val adapterScope = CoroutineScope(Dispatchers.Default)

    private var multiSelect = false
    private val selectedItems = arrayListOf<MediaItem>()

    fun numberOfSelectedItems(): Int {
        return selectedItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val photo = getItem(position) as MediaItem
                holder.bind(photo)

                holder.itemView.setOnLongClickListener {
                    if (!multiSelect) {
                        multiSelect = true
                        selectItem(holder, photo)
                        onClickListener.onSelectionChange()
                    }
                    true
                }

                holder.itemView.setOnClickListener {
                    if (multiSelect) {
                        selectItem(holder, photo)
                        onClickListener.onSelectionChange()
                    } else {
                        onClickListener.onClick(photo)
                    }
                }


                if (holder.itemView is Checkable) {
                    (holder.itemView as Checkable).isChecked = selectedItems.contains(photo)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder.from(parent, viewType, activity, itemViewSize)
    }

    override fun getItemViewType(position: Int): Int {
        return adapterViewType
    }

    class ViewHolder(private val listBinding: ViewDataBinding,
                     private val activity: AppCompatActivity,
                     private val itemViewSize: Int = ITEM_SIZE_MEDIUM) :
            RecyclerView.ViewHolder(listBinding.root) {

        internal var selected: Boolean = false

        fun bind(item: MediaItem) {
            when (listBinding) {
                is ItemPhotoListBinding -> listBinding.apply {
                    setItemListSize(activity.resources, imageFrame, itemViewSize)
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
                     activity: AppCompatActivity,
                     itemViewSize: Int = ITEM_SIZE_MEDIUM): ViewHolder {
                return when (viewType) {
                    ITEM_TYPE_LIST -> ViewHolder(
                            ItemPhotoListBinding.inflate(LayoutInflater.from(parent.context)),
                            activity,
                            itemViewSize
                    )
                    else -> ViewHolder(
                            ItemPhotoListGridBinding.inflate(LayoutInflater.from(parent.context)),
                            activity,
                            itemViewSize
                    )
                }
            }

            fun setItemListSize(resources: Resources, item: View, itemSize: Int) {
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

    interface OnClickListener {
        fun onClick(mediaItem: MediaItem)
        fun onSelectionChange()
    }

    // helper function that adds/removes an item to the list depending on the app's state
    private fun selectItem(holder: ViewHolder, item: MediaItem) {

        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            if (holder.itemView is MaterialCardView) {
                holder.itemView.isChecked = false
            }
        } else {
            selectedItems.add(item)
            if (holder.itemView is MaterialCardView) {
                holder.itemView.isChecked = true
            }
        }
    }

    fun finishSelection() {
        multiSelect = false
        selectedItems.clear()
        notifyDataSetChanged()
    }
}