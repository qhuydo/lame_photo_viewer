package com.hcmus.clc18se.photos.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.ItemPhotoListBinding
import com.hcmus.clc18se.photos.databinding.ItemPhotoListGridBinding

class MediaItemListAdapter(private val activity: AppCompatActivity,
                           private val actionMode: ActionMode.Callback,
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


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val photo = getItem(position) as MediaItem
                holder.bind(photo)

                holder.itemView.setOnClickListener {
                    if (multiSelect) {
                        selectItem(holder, photo)
                    } else {
                        onClickListener.onClick(photo)
                    }
                }

                if (selectedItems.contains(photo)) {
                    // if the item is selected, let the user know by adding a dark layer above it
                    holder.itemView.alpha = 0.3f
                } else {
                    // else, keep it as it is
                    holder.itemView.alpha = 1.0f
                }

                holder.itemView.setOnLongClickListener {
                    if (!multiSelect) {
                        multiSelect = true
                        activity.startSupportActionMode(actionMode)
                        selectItem(holder, photo)
                    }
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder.from(parent, viewType, activity.resources, itemViewSize)
    }

    override fun getItemViewType(position: Int): Int {
        return adapterViewType
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

    // helper function that adds/removes an item to the list depending on the app's state
    private fun selectItem(holder: ViewHolder, item: MediaItem) {
        // If the "selectedItems" list contains the item, remove it and set it's state to normal
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            holder.itemView.alpha = 1.0f
        } else {
            // Else, add it to the list and add a darker shade over the image, letting the user know that it was selected
            selectedItems.add(item)
            holder.itemView.alpha = 0.3f
        }
    }

    fun finishSelection() {
        multiSelect = false
        selectedItems.clear()
        notifyDataSetChanged()
    }
}