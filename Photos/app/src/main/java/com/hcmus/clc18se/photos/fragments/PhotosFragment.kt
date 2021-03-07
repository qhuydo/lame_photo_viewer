package com.hcmus.clc18se.photos.fragments

import android.content.Context.MODE_PRIVATE
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
import com.hcmus.clc18se.photos.utils.setPhotoListIcon
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotosFragment : Fragment() {

    private lateinit var binding: FragmentPhotosBinding
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    private var currentListItemView: Int = ITEM_TYPE_LIST

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
        setHasOptionsMenu(true)

        binding.apply {
            lifecycleOwner = this@PhotosFragment

            photosViewModel = viewModel
            photoListLayout.photoList = viewModel.photoList.value
            photoListLayout.photoListRecyclerView.adapter = PhotoListAdapter(currentListItemView)
            val layoutManager = photoListLayout.photoListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = if (currentListItemView == ITEM_TYPE_LIST) 1 else 2

        }
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.photo_list_item_view_type)
        setPhotoListIcon(menuItem, currentListItemView)
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
            else -> false
        }
    }

    private fun onItemTypeButtonClicked() {
        binding.apply {
            currentListItemView = if (currentListItemView == ITEM_TYPE_LIST)
                ITEM_TYPE_THUMBNAIL else ITEM_TYPE_LIST

            // Save the preference
            preferences.edit()
                    .putString(getString(R.string.photo_list_view_type_key), currentListItemView.toString())
                    .apply()

            val adapter = PhotoListAdapter(currentListItemView)
            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = photoListLayout.photoList

            recyclerView.adapter = adapter
            val layoutManager = recyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = if (currentListItemView == ITEM_TYPE_LIST) 1 else 2

            bindSamplePhotoListRecyclerView(recyclerView, photoList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

}