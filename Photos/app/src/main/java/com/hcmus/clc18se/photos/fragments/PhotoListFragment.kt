package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.FragmentPhotoListBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotoListFragment : AbstractPhotoListFragment(
        R.menu.photo_list_menu
) {

    private lateinit var binding: FragmentPhotoListBinding

    private val args: PhotoListFragmentArgs by navArgs()

    private lateinit var viewModel: PhotosViewModel

    override val onClickListener = object : MediaItemListAdapter.OnClickListener {
        override fun onClick(mediaItem: MediaItem) {
            val idx = viewModel.mediaItemList.value?.indexOf(mediaItem) ?: -1
            viewModel.setCurrentItemView(idx)
            this@PhotoListFragment.findNavController().navigate(
                    PhotoListFragmentDirections.actionPhotoListFragmentToPhotoViewFragment()
            )
        }

        override fun onSelectionChange() {
            invalidateCab()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        )
        this.viewModel = viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photo_list, container, false
        )

        photoListBinding = binding.photoListLayout

        currentListItemView = preferences.getString(
                getString(R.string.photo_list_view_type_key),
                MediaItemListAdapter.ITEM_TYPE_LIST.toString()
        )!!.toInt()

        currentListItemSize = preferences.getString(
                getString(R.string.photo_list_item_size_key),
                "0"
        )!!.toInt()

        adapter = MediaItemListAdapter(
                (requireActivity() as AppCompatActivity),
                onClickListener,
                currentListItemView,
                currentListItemSize
        )

        binding.apply {
            lifecycleOwner = this@PhotoListFragment

            photoListLayout.apply {
                photoList = viewModel.mediaItemList.value
                photoListRecyclerView.adapter = adapter

                (photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                    spanCount = getSpanCountForPhotoList(
                            resources,
                            currentListItemView,
                            currentListItemSize
                    )
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

        // viewModel.loadImages()

        (activity as AbstractPhotosActivity).supportActionBar?.title = args.albumName
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun refreshRecyclerView() {
        binding.apply {
            adapter = MediaItemListAdapter(
                    (requireActivity() as AppCompatActivity),
                    onClickListener,
                    currentListItemView,
                    currentListItemSize
            )

            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = viewModel.mediaItemList.value

            recyclerView.adapter = adapter
            (photoListLayout.photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                spanCount =
                        getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)
            }

            bindMediaListRecyclerView(recyclerView, photoList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }


    override fun getCadSubId(): Int {
        return R.id.cab_stub2
    }
}