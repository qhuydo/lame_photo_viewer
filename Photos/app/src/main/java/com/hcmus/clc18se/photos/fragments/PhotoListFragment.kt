package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.databinding.FragmentPhotoListBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.utils.setPhotoListIcon
import com.hcmus.clc18se.photos.utils.setPhotoListItemSizeOption
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotoListFragment : Fragment() {

    private lateinit var binding: FragmentPhotoListBinding
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    private var currentListItemView: Int = MediaItemListAdapter.ITEM_TYPE_LIST
    private var currentListItemSize: Int = 0

    private val viewModel: PhotosViewModel by activityViewModels()

    private val args: PhotoListFragmentArgs by navArgs()

    private val onClickListener = MediaItemListAdapter.OnClickListener {
        val idx = viewModel.mediaItemList.value?.indexOf(it) ?: -1
        viewModel.setCurrentItemView(idx)
        this.findNavController().navigate(
                PhotoListFragmentDirections.actionPhotoListFragmentToPhotoViewFragment()
        )
    }

    private lateinit var adapter: MediaItemListAdapter


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photo_list, container, false
        )

        currentListItemView = preferences.getString(getString(R.string.photo_list_view_type_key),
                MediaItemListAdapter.ITEM_TYPE_LIST.toString())!!.toInt()

        currentListItemSize = preferences.getString(getString(R.string.photo_list_item_size_key),
                "0")!!.toInt()

        adapter = MediaItemListAdapter((requireActivity() as AppCompatActivity),
                actionModeCallBack,
                onClickListener,
                currentListItemView,
                currentListItemSize)

        binding.apply {
            lifecycleOwner = this@PhotoListFragment

            photoListLayout.apply {
                photoList = viewModel.mediaItemList.value
                photoListRecyclerView.adapter = adapter

                (photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                    spanCount = getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)
                }

                swipeRefreshLayout.setOnRefreshListener {
                    adapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
            }

        }

        viewModel.mediaItemList.observe(requireActivity(), { images ->
            adapter.submitList(images)
        })

        viewModel.loadImages()

        (activity as AbstractPhotosActivity).supportActionBar?.title = args.albumName
        setHasOptionsMenu(true)

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
        inflater.inflate(R.menu.photo_list_menu, menu)
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
            R.id.menu_refresh -> {
                binding.photoListLayout.apply {
                    swipeRefreshLayout.isRefreshing = true
                    adapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
                true
            }
            else -> false
        }
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

    override fun onStop() {
        super.onStop()
        actionMode?.finish()
    }

    private fun refreshRecyclerView() {
        binding.apply {
            adapter = MediaItemListAdapter((requireActivity() as AppCompatActivity),
                    actionModeCallBack,
                    onClickListener,
                    currentListItemView,
                    currentListItemSize)

            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = viewModel.mediaItemList.value

            recyclerView.adapter = adapter
            (photoListLayout.photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                spanCount = getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)
            }

            bindMediaListRecyclerView(recyclerView, photoList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

    private var actionMode: ActionMode? = null

    private val actionModeCallBack = object: ActionMode.Callback {

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