package com.hcmus.clc18se.photos.fragments

import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.databinding.PhotoListBinding
import com.hcmus.clc18se.photos.utils.setPhotoListIcon
import com.hcmus.clc18se.photos.utils.setPhotoListItemSizeOption

/**
 * Inherited by [PhotoListFragment] & [PhotosFragment]
 *
 * [PhotoListFragment] & [PhotosFragment] both have very similar implementation & layout structure.
 * The main different between them is [PhotosFragment] is the top destination but [PhotoListFragment]
 * is not.
 *
 * [AbstractPhotoListFragment] implements the option menu initialization, menu item action,
 * actionMode.
 * ViewModels, binding & recycler view behaviour are implemented in inherit classes.
 *
 * @param menuRes resource ID of the toolbar menu
 * - Parameter value provided by [PhotoListFragment] is [R.menu.photo_list_menu]
 * - And provided by [PhotosFragment] is [R.menu.photos_menu]
 *
 * @see [PhotosFragment]
 * @see [PhotoListFragment]
 */
abstract class AbstractPhotoListFragment(private val menuRes: Int): Fragment() {

    protected val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    // Current list item view type, get from preference
    protected var currentListItemView: Int = MediaItemListAdapter.ITEM_TYPE_LIST
    // Current list iten size, get from preference
    protected var currentListItemSize: Int = MediaItemListAdapter.ITEM_SIZE_MEDIUM

    // On click listener object used by MediaItemListAdapter
    abstract val onClickListener: MediaItemListAdapter.OnClickListener

    // Used in refreshRecyclerView()
    // Must be init in onCreateView() from the inherit class
    protected lateinit var photoListBinding: PhotoListBinding

    protected lateinit var adapter: MediaItemListAdapter

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(menuRes, menu)
    }

    /**
     * Set the icon in the toolbar to match with the current list item type in photo_list
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val photoListImageItem = menu.findItem(R.id.photo_list_item_view_type)
        setPhotoListIcon(photoListImageItem, currentListItemView)

        val currentPreference = preferences.getString(getString(
                R.string.photo_list_item_size_key), "0") ?: "0"
        setPhotoListItemSizeOption(resources, menu, currentPreference)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.photo_list_item_view_type -> {
                onItemTypeButtonClicked()
                setPhotoListIcon(item, currentListItemView)
                true
            }
            R.id.item_view_size_big -> onItemSizeOptionClicked(item)
            R.id.item_view_size_medium -> onItemSizeOptionClicked(item)
            R.id.item_view_size_small -> onItemSizeOptionClicked(item)
            R.id.menu_refresh -> onRefreshPhotoList()
            else -> false
        }
    }

    private fun onRefreshPhotoList(): Boolean {
        photoListBinding.apply {
            swipeRefreshLayout.isRefreshing = true
            adapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }
        return true
    }

    private fun onItemTypeButtonClicked() {
        currentListItemView = if (currentListItemView == MediaItemListAdapter.ITEM_TYPE_LIST)
            MediaItemListAdapter.ITEM_TYPE_GRID else MediaItemListAdapter.ITEM_TYPE_LIST

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.photo_list_view_type_key), currentListItemView.toString())
                .apply()

        refreshRecyclerView()
    }

    protected fun onItemSizeOptionClicked(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        val options = resources.getStringArray(R.array.photo_list_item_size_value)

        val option = when (menuItem.itemId) {
            R.id.item_view_size_big -> options[0]
            R.id.item_view_size_medium -> options[1]
            else -> options[2]
        }

        currentListItemSize = option.toInt()

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.photo_list_item_size_key), option)
                .apply()

        refreshRecyclerView()

        return true
    }

    override fun onStop() {
        super.onStop()
        actionMode?.finish()
    }

    abstract fun refreshRecyclerView()

    internal var actionMode: ActionMode? = null

    protected val actionModeCallBack = object: ActionMode.Callback {

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.action_multiple_delete) {
                // Delete button is clicked, handle the deletion and finish the multi select process
                Toast.makeText(activity, "Selected images deleted", Toast.LENGTH_SHORT).show()
                mode?.finish()
            }
            return true
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            actionMode = mode
            mode?.menuInflater?.inflate(R.menu.photo_list_long_click_menu, menu)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            // finished multi selection
            adapter.finishSelection()
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }
    }

}