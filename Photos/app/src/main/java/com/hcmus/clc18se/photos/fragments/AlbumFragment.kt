package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.databinding.FragmentAlbumBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForAlbumList
import com.hcmus.clc18se.photos.utils.setAlbumListIcon
import com.hcmus.clc18se.photos.utils.setAlbumListItemSizeOption
import com.hcmus.clc18se.photos.viewModels.AlbumViewModel
import com.hcmus.clc18se.photos.viewModels.AlbumViewModelFractory

class AlbumFragment : Fragment() {
    private lateinit var binding: FragmentAlbumBinding

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    private var currentListItemView: Int = AlbumListAdapter.ITEM_TYPE_LIST
    private var currentListItemSize: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_album, container, false
        )

        currentListItemView = preferences.getString(getString(R.string.album_list_view_type_key),
                AlbumListAdapter.ITEM_TYPE_LIST.toString())!!.toInt()

        currentListItemSize = preferences.getString(getString(R.string.album_list_item_size_key),
                "0")!!.toInt()

        setHasOptionsMenu(true)

        binding.apply {

            lifecycleOwner = this@AlbumFragment

            val viewModelFactory = AlbumViewModelFractory(resources)
            val viewModel = ViewModelProvider(this@AlbumFragment, viewModelFactory)
                    .get(AlbumViewModel::class.java)

            albumViewModel = viewModel
            albumListLayout.albumListRecyclerView.adapter = AlbumListAdapter(currentListItemView)
            albumListLayout.albumList = viewModel.albumList.value

            val layoutManager = albumListLayout.albumListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = getSpanCountForAlbumList(
                    resources, currentListItemView, currentListItemSize)
        }
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val albumListImageItem = menu.findItem(R.id.album_list_item_view_type)
        setAlbumListIcon(albumListImageItem, currentListItemView)

        val currentPreference = preferences.getString(getString(
                R.string.album_list_item_size_key), "0") ?: "0"
        setAlbumListItemSizeOption(resources, menu, currentPreference)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.album_menu, menu)
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
            AlbumListAdapter.ITEM_TYPE_THUMBNAIL else AlbumListAdapter.ITEM_TYPE_LIST

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

    private fun refreshRecyclerView() {
        binding.apply {
            val adapter = AlbumListAdapter(currentListItemView)
            val recyclerView = albumListLayout.albumListRecyclerView
            val albumList = albumListLayout.albumList

            recyclerView.adapter = adapter
            val layoutManager = albumListLayout.albumListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = getSpanCountForAlbumList(
                    resources, currentListItemView, currentListItemSize)

            bindSampleAlbumListRecyclerView(recyclerView, albumList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

}