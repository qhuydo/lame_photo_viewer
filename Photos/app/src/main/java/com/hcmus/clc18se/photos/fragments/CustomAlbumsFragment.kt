package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentCustomAlbumsBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class CustomAlbumsFragment : AbstractAlbumFragment() {

    private lateinit var binding: FragmentCustomAlbumsBinding

    private val viewModel: CustomAlbumViewModel by activityViewModels {
        CustomAlbumViewModelFactory(
                requireActivity().application,
                PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
        )
    }

    private lateinit var photosViewModel: PhotosViewModel

    private lateinit var customAlbumAdapter: AlbumListAdapter

    private val onClickListener = AlbumListAdapter.OnClickListener {
        viewModel.startNavigatingToPhotoList(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val photosViewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                    requireActivity().application,
                    PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        this.photosViewModel = photosViewModel
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentCustomAlbumsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.customAlbumViewModel = viewModel

        customAlbumAdapter = AlbumListAdapter(currentListItemView,
                currentListItemSize,
                onClickListener)

        initObservers()
        return binding.root
    }

    private fun initObservers() {
        viewModel.navigateToPhotoList.observe(viewLifecycleOwner) {
            if (it != null) {
                photosViewModel.setMediaItemFromAlbum(
                        viewModel.getSelectedAlbum()?.mediaItems ?: listOf()
                )

                findNavController().navigate(
                        CustomAlbumsFragmentDirections.actionPageCustomAlbumsToPhotoListFragment(
                                it.getName() ?: "???")
                )

                viewModel.doneNavigatingToPhotoList()
            }
        }

    }

    override fun refreshRecyclerView() {
        binding.apply {
            customAlbumAdapter = AlbumListAdapter(
                    currentListItemView,
                    currentListItemSize,
                    onClickListener
            )
            val recyclerView = albumListLayout.albumListRecyclerView
            val albumList = viewModel.albums

            recyclerView.adapter = customAlbumAdapter
            val layoutManager = albumListLayout.albumListRecyclerView.layoutManager as? GridLayoutManager
            layoutManager?.spanCount = getSpanCountForAlbumList(resources,
                    currentListItemView,
                    currentListItemSize)

            bindSampleAlbumListRecyclerView(recyclerView, albumList.value ?: listOf())

            customAlbumAdapter.notifyDataSetChanged()
        }
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.custom_album_title

    override fun getOptionMenuResId(): Int = R.menu.custom_album_menu

}