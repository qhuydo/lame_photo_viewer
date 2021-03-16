package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotosFragment : AbstractPhotoListFragment(R.menu.photo_list_menu) {

    private lateinit var binding: FragmentPhotosBinding
    private val viewModel: PhotosViewModel by activityViewModels()

    override val onClickListener = MediaItemListAdapter.OnClickListener {
        val idx = viewModel.mediaItemList.value?.indexOf(it) ?: -1
        viewModel.setCurrentItemView(idx)
        this.findNavController().navigate(
                PhotosFragmentDirections.actionPagePhotoToPhotoViewFragment()
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photos, container, false
        )

        photoListBinding = binding.photoListLayout

        currentListItemView = preferences.getString(getString(R.string.photo_list_view_type_key),
                MediaItemListAdapter.ITEM_TYPE_LIST.toString())!!.toInt()

        currentListItemSize = preferences.getString(getString(R.string.photo_list_item_size_key),
                "0")!!.toInt()

        setHasOptionsMenu(true)

        adapter = MediaItemListAdapter((requireActivity() as AppCompatActivity),
                actionModeCallBack,
                onClickListener,
                currentListItemView,
                currentListItemSize)

        binding.apply {
            lifecycleOwner = this@PhotosFragment

            photosViewModel = viewModel

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

        return binding.root
    }

    override fun refreshRecyclerView() {
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

}