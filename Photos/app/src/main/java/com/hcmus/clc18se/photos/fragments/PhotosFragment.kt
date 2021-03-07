package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.PhotoListAdapter
import com.hcmus.clc18se.photos.adapters.PhotoListAdapter.Companion.ITEM_TYPE_LIST
import com.hcmus.clc18se.photos.adapters.PhotoListAdapter.Companion.ITEM_TYPE_THUMBNAIL
import com.hcmus.clc18se.photos.adapters.bindSamplePhotoListRecyclerView
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.utils.setPhotoListIcon
import com.hcmus.clc18se.photos.utils.setPhotoListItemSizeOption
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotosFragment : Fragment() {

    private lateinit var binding: FragmentPhotosBinding
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    private var currentListItemView: Int = ITEM_TYPE_LIST
    private var currentListItemSize: Int = 0

    private val viewModel by lazy {
        ViewModelProvider(this).get(PhotosViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photos, container, false
        )

        currentListItemView = preferences.getString(getString(R.string.photo_list_view_type_key),
                ITEM_TYPE_LIST.toString())!!.toInt()

        currentListItemSize = preferences.getString(getString(R.string.photo_list_item_size_key),
                "0")!!.toInt()


        setHasOptionsMenu(true)

        binding.apply {
            lifecycleOwner = this@PhotosFragment

            photosViewModel = viewModel
            photoListLayout.photoList = viewModel.photoList.value
            photoListLayout.photoListRecyclerView.adapter = PhotoListAdapter(currentListItemView)
            val layoutManager = photoListLayout.photoListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = getSpanCountForPhotoList(
                    resources, currentListItemView, currentListItemSize)
        }
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val photoListImageItem = menu.findItem(R.id.photo_list_item_view_type)
        setPhotoListIcon(photoListImageItem, currentListItemView)

        val currentPreference = preferences.getString(getString(
                R.string.photo_list_item_size_key), "0") ?: "0"
        setPhotoListItemSizeOption(resources, menu, currentPreference)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.photos_menu, menu)
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
            else -> false
        }
    }

    private fun onItemTypeButtonClicked() {
        currentListItemView = if (currentListItemView == ITEM_TYPE_LIST)
            ITEM_TYPE_THUMBNAIL else ITEM_TYPE_LIST

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.photo_list_view_type_key), currentListItemView.toString())
                .apply()

        refreshRecyclerView()
    }

    private fun onItemSizeOptionClicked(menuItem: MenuItem): Boolean {
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

    private fun refreshRecyclerView() {
        binding.apply {
            val adapter = PhotoListAdapter(currentListItemView)
            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = photoListLayout.photoList

            recyclerView.adapter = adapter
            val layoutManager = photoListLayout.photoListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = getSpanCountForPhotoList(
                    resources, currentListItemView, currentListItemSize)

            bindSamplePhotoListRecyclerView(recyclerView, photoList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

}