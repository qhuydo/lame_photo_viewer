package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.adapters.visibleWhenEmpty
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.FragmentCustomPhotosBinding
import com.hcmus.clc18se.photos.databinding.PhotoListDialogBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import kotlinx.coroutines.launch


class CustomPhotosFragment : AbstractPhotoListFragment(R.menu.custom_photo_list) {

    private lateinit var binding: FragmentCustomPhotosBinding

    private lateinit var viewModel: PhotosViewModel

    private val albumViewModel: CustomAlbumViewModel by activityViewModels {
        CustomAlbumViewModelFactory(requireActivity().application, database)
    }

    // the view model to get all photos in device
    private val photosViewModel: PhotosViewModel by activityViewModels {
        PhotosViewModelFactory(requireActivity().application, database)
    }

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
        val viewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                    requireActivity().application,
                    database
            )
        }
        this.viewModel = viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 300L }
        viewModel.loadSelectedPhotoList()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomPhotosBinding.inflate(layoutInflater, container, false)

        binding.lifecycleOwner = this
        photoListBinding = binding.photoListLayout

        binding.fabAddPhoto.setOnClickListener {
            addPhotosBottomSheet()
        }
        initRecyclerViews()
        initObservers()

        // viewModel.loadImages()
        return binding.root
    }

    private fun initRecyclerViews() {
        adapter = MediaItemListAdapter(
                actionCallbacks,
                currentListItemView,
                currentListItemSize
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.photoListLayout.photoListRecyclerView.apply {
            adapter = this@CustomPhotosFragment.adapter

            (layoutManager as? StaggeredGridLayoutManager)?.apply {
                spanCount = getSpanCountForPhotoList(
                        resources,
                        currentListItemView,
                        currentListItemSize
                )
            }
        }
    }

    private fun initObservers() {

        viewModel.mediaItemList.observe(viewLifecycleOwner) { images ->
            if (images != null) {
                adapter.filterAndSubmitList(images)
                binding.placeholder.root.visibleWhenEmpty(images)
            }
        }

        albumViewModel.reloadDataRequest.observe(viewLifecycleOwner) {
            if (it) {
                albumViewModel.loadData()
                viewModel.loadSelectedPhotoList()
                albumViewModel.doneRequestingLoadData()
            }
        }

        viewModel.customAlbum.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.loadSelectedPhotoList()
            }
        }

        viewModel.navigateToImageView.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem != null) {
                val idx = viewModel.mediaItemList.value?.indexOf(mediaItem) ?: -1
                viewModel.setCurrentItemView(idx)
                findNavController().navigate(
                        CustomPhotosFragmentDirections.actionPageCustomPhotoToPhotoViewFragment()
                )
                viewModel.doneNavigatingToImageView()
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.customAlbum.value?.let { album ->
            (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = album.getName()
        }
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

    private fun addPhotos(mediaItems: List<MediaItem>) {
        viewModel.insertPhotosIntoSelectedAlbum(mediaItems)
    }

    private fun addPhotosBottomSheet() = MaterialDialog(requireContext(), BottomSheet()).show {

        lifecycleOwner(this@CustomPhotosFragment)
        title(R.string.add_photo_dialog_title)

        val dialogBinding = PhotoListDialogBinding.inflate(this@CustomPhotosFragment.layoutInflater)
                .apply {
                    lifecycleOwner = this@CustomPhotosFragment
                    this.viewmodel = photosViewModel
                    initRecyclerViewForBottomSheet(photoListRecyclerView)
                }


        val observer = Observer<List<MediaItem>> {
            if (it != null) {
                bindMediaListRecyclerView(dialogBinding.photoListRecyclerView, it)
            }
        }
        photosViewModel.mediaItemList.observe(viewLifecycleOwner, observer)

        customView(scrollable = true, view = dialogBinding.root)

        positiveButton {
            photosViewModel.mediaItemList.removeObserver(observer)

            dialogBinding.photoListRecyclerView.adapter?.let { adapter ->
                if (adapter is MediaItemListAdapter) {
                    addPhotos(adapter.getSelectedItems())
                }
            }
        }
    }

    private fun initRecyclerViewForBottomSheet(recyclerView: RecyclerView) {
        val adapterCallback = object : MediaItemListAdapter.ActionCallbacks {
            override fun onClick(mediaItem: MediaItem) {}
            override fun onSelectionChange() {}
        }

        recyclerView.adapter = MediaItemListAdapter(
                adapterCallback,
                adapterViewType = currentListItemView,
                itemViewSize = currentListItemSize,
                enforceMultiSelection = true
        )

        (recyclerView.layoutManager as? StaggeredGridLayoutManager)?.apply {
            spanCount = getSpanCountForPhotoList(
                    resources,
                    currentListItemView,
                    currentListItemSize
            )
        }

    }

    override fun onCabItemSelected(item: MenuItem): Boolean {
        val result = super.onCabItemSelected(item)
        if (!result) {
            return when (item.itemId) {
                R.id.action_remove_from_custom_album -> {
                    onActionRemoveFromCustomAlbum()
                    true
                }
                else -> false
            }
        }
        return result
    }

    private fun onActionRemoveFromCustomAlbum() {

        viewModel.viewModelScope.launch {
            val customAlbum = viewModel.customAlbum.value
            customAlbum?.let {
                albumViewModel.removePhotosFromAlbum(adapter.getSelectedItems(), it.customAlbumId!!)
                mainCab?.destroy()
            }
        }
    }

    override fun onPrepareCabMenu(menu: Menu) {
        super.onPrepareCabMenu(menu)

        val actionAddToCustomAlbum = menu.findItem(R.id.action_add_to_custom_album)
        val actionRemoveFromCustomAlbum = menu.findItem(R.id.action_remove_from_custom_album)

        actionAddToCustomAlbum?.isVisible = false
        actionRemoveFromCustomAlbum?.isVisible = true

        binding.fabAddPhoto.hide()
    }

    override fun onCabDestroy(): Boolean {
        binding.fabAddPhoto.show()
        return super.onCabDestroy()
    }

}