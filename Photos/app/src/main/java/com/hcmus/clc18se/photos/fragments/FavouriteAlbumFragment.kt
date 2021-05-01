package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentFavouriteAlbumBinding
import com.hcmus.clc18se.photos.utils.*
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

    override val actionCallbacks = object : MediaItemListAdapter.ActionCallbacks {
        override fun onClick(mediaItem: MediaItem) {
            photosViewModel.startNavigatingToImageView(mediaItem)
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

        currentListItemView = requireContext().currentPhotoListItemView(preferences)
        currentListItemSize = requireContext().currentPhotoListItemSize(preferences)

        adapter = MediaItemListAdapter(
                actionCallbacks,
                currentListItemView,
                currentListItemSize
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.apply {
            lifecycleOwner = this@FavouriteAlbumFragment

            photoListRecyclerView.adapter = adapter

            (photoListRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.apply {
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
            adapter.filterAndSubmitList(it)
        }

        viewModel.reloadDataRequest.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.loadData()
                viewModel.doneRequestingLoadData()
            }
        }

        photosViewModel.navigateToImageView.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem != null) {
                photosViewModel.loadDataFromOtherViewModel(viewModel)

                val idx = viewModel.mediaItems.value?.indexOf(mediaItem) ?: -1

                photosViewModel.setCurrentItemView(idx)
                this@FavouriteAlbumFragment.findNavController().navigate(
                        FavouriteAlbumFragmentDirections.actionFavouriteAlbumFragmentToPhotoViewFragment()
                )

                photosViewModel.doneNavigatingToImageView()
            }

        }
    }

    override fun getCadSubId(): Int = R.id.cab_stub2

    override fun refreshRecyclerView() {
        binding.apply {

            val recyclerView = photoListRecyclerView
            val photoList = viewModel.mediaItems.value

            recyclerView.adapter = MediaItemListAdapter(
                    actionCallbacks,
                    currentListItemView,
                    currentListItemSize
            ).apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            (photoListRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.spanCount =
                    getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)

            bindMediaListRecyclerView(recyclerView, photoList)
            // adapter.notifyDataSetChanged()
        }
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.favorites_title
}