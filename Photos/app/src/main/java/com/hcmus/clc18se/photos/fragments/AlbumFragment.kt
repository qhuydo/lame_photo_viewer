package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.PhotosPagerActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.MediaProvider
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentAlbumBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.viewModels.AlbumViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class AlbumFragment : AbstractAlbumFragment() {

    private lateinit var binding: FragmentAlbumBinding

    private val albumViewModel: AlbumViewModel by activityViewModels()

    private lateinit var photosViewModel: PhotosViewModel

    private lateinit var adapter: AlbumListAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId(),
        ) {
            PhotosViewModelFactory(
                    requireActivity().application,
                    PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        photosViewModel = viewModel
    }

    private val albumAdapterListener = AlbumListAdapter.OnClickListener {
        // I know something went wrong with this, but it was kinda runnable,
        // so I left this untouched until the obvious bug appears.
        // TODO: investigate bugs in these line, or somewhere else.
        val idx = albumViewModel.albumList.value?.indexOf(it) ?: -1
        albumViewModel.setCurrentItemView(idx)
        albumViewModel.startNavigatingToPhotoList(it)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_album, container, false
        )

        setHasOptionsMenu(true)

        binding.lifecycleOwner = this@AlbumFragment
        binding.albumViewModel = this@AlbumFragment.albumViewModel

        adapter = AlbumListAdapter(
                currentListItemView,
                currentListItemSize,
                albumAdapterListener
        )

        binding.albumListLayout.apply {

            albumListRecyclerView.adapter = adapter

            val layoutManager = albumListRecyclerView.layoutManager as? GridLayoutManager
            layoutManager?.spanCount = getSpanCountForAlbumList(resources,
                    currentListItemView,
                    currentListItemSize)
        }

        initObservers()
        return binding.root
    }

    private fun initObservers() {
        albumViewModel.onAlbumLoaded.observe(viewLifecycleOwner, {
            if (it == true) {
                binding.progressCircular.visibility = View.INVISIBLE
                albumViewModel.albumList.value?.let { list ->
                    bindSampleAlbumListRecyclerView(binding.albumListLayout.albumListRecyclerView, list)
                    adapter.submitList(this@AlbumFragment.albumViewModel.albumList.value)
                }
            }
        })

        albumViewModel.navigateToPhotoList.observe(viewLifecycleOwner, {
            if (it != null) {
                photosViewModel.setMediaItemFromAlbum(albumViewModel.getSelectedAlbum()?.mediaItems
                        ?: listOf())
                this.findNavController().navigate(
                        AlbumFragmentDirections.actionPageAlbumToPhotoListFragment(it.getName()
                                ?: "???"))
                albumViewModel.doneNavigatingToPhotoList()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.favorites.setOnClickListener {
            when (requireActivity()) {
                is PhotosPagerActivity -> {
                    findNavController().navigate(
                            HomeViewPagerFragmentDirections.actionHomeViewPagerFragmentToFavouriteAlbumFragment()
                    )
                }
                else -> {

                    findNavController().navigate(
                            AlbumFragmentDirections.actionPageAlbumToFavouriteAlbumFragment()
                    )
                }
            }
        }
        binding.customAlbum.setOnClickListener {
            when (requireActivity()) {
                is PhotosPagerActivity -> {
                    findNavController().navigate(
                            HomeViewPagerFragmentDirections.actionHomeViewPagerFragmentToPageCustomAlbum()
                    )
                }
                else -> {

                    findNavController().navigate(
                            AlbumFragmentDirections.actionPageAlbumToPageCustomAlbum()
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        return when (item.itemId) {
            R.id.menu_refresh -> onRefreshAlbumList()
            else -> false
        }
    }

    private fun onRefreshAlbumList(): Boolean {
//        binding.swipeRefreshLayout.isRefreshing = true
        (requireActivity() as AbstractPhotosActivity).mediaProvider.loadAlbum(object : MediaProvider.MediaProviderCallBack {
            override fun onMediaLoaded(albums: ArrayList<Album>?) {
                this@AlbumFragment.albumViewModel.notifyAlbumLoaded()
                // binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onHasNoPermission() {
                (requireActivity() as AbstractPhotosActivity).jumpToMainActivity()
            }
        })
        return true
    }

    override fun refreshRecyclerView() {
        binding.apply {
            adapter = AlbumListAdapter(
                    currentListItemView,
                    currentListItemSize,
                    albumAdapterListener
            )
            val recyclerView = albumListLayout.albumListRecyclerView
            val albumList = this@AlbumFragment.albumViewModel.albumList

            recyclerView.adapter = adapter
            val layoutManager = albumListLayout.albumListRecyclerView.layoutManager as? GridLayoutManager
            layoutManager?.spanCount = getSpanCountForAlbumList(resources,
                    currentListItemView,
                    currentListItemSize)

            bindSampleAlbumListRecyclerView(recyclerView, albumList.value ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar.searchActionBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar.appBarLayout

    override fun getToolbarTitleRes(): Int = R.string.album_title

    override fun getOptionMenuResId(): Int = R.menu.album_menu
}