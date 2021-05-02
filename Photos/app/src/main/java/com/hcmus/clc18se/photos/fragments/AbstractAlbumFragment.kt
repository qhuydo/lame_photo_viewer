package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.utils.currentAlbumListItemSize
import com.hcmus.clc18se.photos.utils.currentAlbumListItemView
import com.hcmus.clc18se.photos.utils.setAlbumListIcon
import com.hcmus.clc18se.photos.utils.setAlbumListItemSizeOption

abstract class AbstractAlbumFragment : BaseFragment() {

    protected val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    protected var currentListItemView: Int = AlbumListAdapter.ITEM_TYPE_LIST

    protected var currentListItemSize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }

        currentListItemView = requireContext().currentAlbumListItemView(preferences)
        currentListItemSize = requireContext().currentAlbumListItemSize(preferences)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.album_list_item_view_type)?.let { albumListImageItem ->
            setAlbumListIcon(albumListImageItem, currentListItemView)

            val currentPreference = preferences.getString(getString(
                    R.string.album_list_item_size_key), "0") ?: "0"
            setAlbumListItemSizeOption(resources, menu, currentPreference)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(getOptionMenuResId(), menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.album_list_item_view_type -> {
                onItemTypeButtonClicked()
                setAlbumListIcon(item, currentListItemView)
                true
            }
            R.id.album_item_view_size_big -> onItemSizeOptionClicked(item)
            R.id.album_item_view_size_medium -> onItemSizeOptionClicked(item)
            R.id.album_item_view_size_small -> onItemSizeOptionClicked(item)
            else -> false
        }
    }

    private fun onItemTypeButtonClicked() {
        currentListItemView = if (currentListItemView == AlbumListAdapter.ITEM_TYPE_LIST)
            AlbumListAdapter.ITEM_TYPE_GRID else AlbumListAdapter.ITEM_TYPE_LIST

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.album_list_view_type_key), currentListItemView.toString())
                .apply()

        refreshRecyclerView()
    }

    private fun onItemSizeOptionClicked(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        val options = resources.getStringArray(R.array.photo_list_item_size_value)

        val option = when (menuItem.itemId) {
            R.id.album_item_view_size_big -> options[0]
            R.id.album_item_view_size_medium -> options[1]
            else -> options[2]
        }

        currentListItemSize = option.toInt()

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.album_list_item_size_key), option)
                .apply()

        refreshRecyclerView()

        return true
    }

    abstract fun refreshRecyclerView()

    abstract fun getOptionMenuResId(): Int

}