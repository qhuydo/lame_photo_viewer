package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentFavouriteAlbumBinding
import com.hcmus.clc18se.photos.utils.SpaceItemDecoration
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class FavouriteAlbumFragment : AbstractPhotoListFragment(R.menu.photo_list_menu) {

    private lateinit var binding: FragmentFavouriteAlbumBinding

    private val peach by lazy {
        PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
    }

    private val viewModel: FavouriteAlbumViewModel by activityViewModels {
        FavouriteAlbumViewModelFactory(
                requireActivity().application, peach
        )
    }

    private lateinit var photosViewModel: PhotosViewModel

    override val onClickListener = object : MediaItemListAdapter.OnClickListener {
        override fun onClick(mediaItem: MediaItem) {
            photosViewModel.loadDataFromOtherViewModel(viewModel)

            val idx = viewModel.mediaItems.value?.indexOf(mediaItem) ?: -1

            photosViewModel.setCurrentItemView(idx)
            this@FavouriteAlbumFragment.findNavController().navigate(
                    FavouriteAlbumFragmentDirections.actionFavouriteAlbumFragmentToPhotoViewFragment()
            )
        }

        override fun onSelectionChange() {
            invalidateCab()
        }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFavouriteAlbumBinding.inflate(inflater, container, false)

        val photosViewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                    requireActivity().application,
                    PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        this.photosViewModel = photosViewModel

        currentListItemView = preferences.getString(
                getString(R.string.photo_list_view_type_key),
                MediaItemListAdapter.ITEM_TYPE_LIST.toString()
        )!!.toInt()

        currentListItemSize = preferences.getString(
                getString(R.string.photo_list_item_size_key),
                "0"
        )!!.toInt()

        adapter = MediaItemListAdapter(
                onClickListener,
                currentListItemView,
                currentListItemSize
        )

        binding.apply {
            lifecycleOwner = this@FavouriteAlbumFragment

            photoListRecyclerView.adapter = adapter

            photoListRecyclerView.addItemDecoration(
                    SpaceItemDecoration(resources.getDimension(R.dimen.photo_list_item_margin).toInt())
            )

            (photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                spanCount = getSpanCountForPhotoList(
                        resources,
                        currentListItemView,
                        currentListItemSize
                )
            }
        }

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
    }

    private fun initObservers() {
        viewModel.mediaItems.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.reloadDataRequest.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.loadData()
                viewModel.doneRequestingLoadData()
            }
        }
    }

    override fun getCadSubId(): Int = R.id.cab_stub2

    override fun refreshRecyclerView() {
        binding.apply {

            val recyclerView = photoListRecyclerView
            val photoList = viewModel.mediaItems.value

            recyclerView.adapter = MediaItemListAdapter(
                    onClickListener,
                    currentListItemView,
                    currentListItemSize
            )
            (photoListRecyclerView.layoutManager as? GridLayoutManager)?.spanCount =
                    getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)

            bindMediaListRecyclerView(recyclerView, photoList)
            // adapter.notifyDataSetChanged()
        }
    }
}