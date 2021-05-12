package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import com.hcmus.clc18se.photos.databinding.FragmentPhotoListBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class PhotoListFragment : AbstractPhotoListFragment(R.menu.photo_list_menu) {

    private lateinit var binding: FragmentPhotoListBinding

    private val args: PhotoListFragmentArgs by navArgs()

    private val viewModel: PhotosViewModel by viewModels {
        PhotosViewModelFactory(requireActivity().application, database)
    }

    private lateinit var navGraphViewModel: PhotosViewModel

    override val actionCallbacks = object : MediaItemListAdapter.ActionCallbacks {
        override fun onClick(mediaItem: MediaItem) {
            viewModel.startNavigatingToImageView(mediaItem)
        }

        override fun onSelectionChange() {
            invalidateCab()
        }
    }

    override fun getCurrentViewModel(): PhotosViewModel = viewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val navGraphViewModel: PhotosViewModel by navGraphViewModels(
            (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                requireActivity().application,
                PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        val bucketId = args.bucketId

        viewModel.loadImages(bucketId)
        this.navGraphViewModel = navGraphViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 300L }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_photo_list, container, false
        )

        photoListBinding = binding.photoListLayout

        adapter = MediaItemListAdapter(
            actionCallbacks,
            currentListItemView,
            currentListItemSize
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.apply {
            lifecycleOwner = this@PhotoListFragment
            initRecyclerView()
        }

        initObserver()
        // viewModel.loadImages()
        return binding.root
    }

    private fun initObserver() {
        viewModel.mediaItemList.observe(viewLifecycleOwner) { images ->
            adapter.filterAndSubmitList(images)
        }

        viewModel.navigateToImageView.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem != null) {
                navGraphViewModel.loadDataFromOtherViewModel(viewModel)
                val idx = navGraphViewModel.mediaItemList.value?.indexOf(mediaItem) ?: -1
                navGraphViewModel.setCurrentItemView(idx)

                findNavController().navigate(
                        PhotoListFragmentDirections.actionPhotoListFragmentToPhotoViewFragment()
                )
                viewModel.doneNavigatingToImageView()
            }
        }

        navGraphViewModel.navigateToImageView.observe(viewLifecycleOwner) {
            if (it != null) {
                val idx = navGraphViewModel.mediaItemList.value?.indexOf(it) ?: -1
                navGraphViewModel.setCurrentItemView(idx)
                findNavController().navigate(
                        PhotoListFragmentDirections.actionPhotoListFragmentToPhotoViewFragment()
                )
                navGraphViewModel.doneNavigatingToImageView()
            }
        }
    }

    private fun FragmentPhotoListBinding.initRecyclerView() {
        photoListLayout.apply {
            photoListRecyclerView.adapter = adapter

            (photoListRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.apply {
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

            // topAppBar2.thumbnail = viewModel.mediaItemList.value?.random()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AbstractPhotosActivity).supportActionBar?.title = args.albumName
        setHasOptionsMenu(true)
    }

    override fun refreshRecyclerView() {
        binding.apply {
            adapter = MediaItemListAdapter(
                actionCallbacks,
                currentListItemView,
                currentListItemSize
            ).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = viewModel.mediaItemList.value

            recyclerView.adapter = adapter
            (recyclerView.layoutManager as? StaggeredGridLayoutManager)?.spanCount =
                getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)

            bindMediaListRecyclerView(recyclerView, photoList ?: listOf())
            adapter.notifyDataSetChanged()
        }
    }

    override fun getCabId(): Int {
        return R.id.cab_stub2
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun isSwipeLayoutEnabled() = true
}