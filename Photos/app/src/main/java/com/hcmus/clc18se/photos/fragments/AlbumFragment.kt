package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.PhotosPagerActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.databinding.FragmentAlbumBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.viewModels.AlbumViewModel

class AlbumFragment : AbstractAlbumFragment() {

    private lateinit var binding: FragmentAlbumBinding

    private val albumViewModel: AlbumViewModel by activityViewModels()

    private lateinit var adapter: AlbumListAdapter

    private val albumAdapterListener = object : AlbumListAdapter.AlbumListAdapterCallbacks {
        override fun onClick(album: Album) {
            val idx = albumViewModel.albumList.value?.indexOf(album) ?: -1
            albumViewModel.setCurrentItemView(idx)
            albumViewModel.startNavigatingToPhotoList(album)
        }

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
                false,
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
        albumViewModel.albumList.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.progressCircular.visibility = View.INVISIBLE
//                albumViewModel.albumList.value?.let { list ->
//                    bindSampleAlbumListRecyclerView(binding.albumListLayout.albumListRecyclerView, list)
//                    adapter.submitList(this@AlbumFragment.albumViewModel.albumList.value)
//                }
            }
        }

        albumViewModel.navigateToPhotoList.observe(viewLifecycleOwner) {
            if (it != null) {
                this.findNavController().navigate(
                    AlbumFragmentDirections.actionPageAlbumToPhotoListFragment(
                        it.getName() ?: "???",
                        it.bucketId!!
                    )
                )
                albumViewModel.doneNavigatingToPhotoList()
            }
        }
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

    // TODO: onRefreshAlbumList
    private fun onRefreshAlbumList(): Boolean {
        // binding.swipeRefreshLayout.isRefreshing = true
        albumViewModel.loadAlbums()
        return true
    }

    override fun refreshRecyclerView() {
        binding.apply {
            adapter = AlbumListAdapter(
                    currentListItemView,
                    currentListItemSize,
                    false,
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