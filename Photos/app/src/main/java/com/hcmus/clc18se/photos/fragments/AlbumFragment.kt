package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.MediaProvider
import com.hcmus.clc18se.photos.databinding.FragmentAlbumBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForAlbumList
import com.hcmus.clc18se.photos.utils.setAlbumListIcon
import com.hcmus.clc18se.photos.utils.setAlbumListItemSizeOption
import com.hcmus.clc18se.photos.viewModels.AlbumViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import timber.log.Timber
import java.util.ArrayList

class AlbumFragment : Fragment() {
    private lateinit var binding: FragmentAlbumBinding

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    private var currentListItemView: Int = AlbumListAdapter.ITEM_TYPE_LIST

    private var currentListItemSize: Int = 0

    private val albumViewModel: AlbumViewModel by activityViewModels()

    private lateinit var photosViewModel: PhotosViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        )
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("ALBUM viewModel ${(requireActivity() as AbstractPhotosActivity).viewModel === albumViewModel}")

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_album, container, false
        )

        currentListItemView = preferences.getString(getString(R.string.album_list_view_type_key),
                AlbumListAdapter.ITEM_TYPE_LIST.toString())!!.toInt()

        currentListItemSize = preferences.getString(getString(R.string.album_list_item_size_key),
                "0")!!.toInt()

        setHasOptionsMenu(true)

        binding.lifecycleOwner = this@AlbumFragment

        binding.albumViewModel = this@AlbumFragment.albumViewModel
        val adapter = AlbumListAdapter(albumAdapterListener,
                currentListItemView,
                currentListItemSize)


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

        // TODO: clean this mess
        binding.apply {

            albumListLayout.albumListRecyclerView.adapter = adapter
            albumListLayout.albumList = this@AlbumFragment.albumViewModel.albumList.value

            val layoutManager = albumListLayout.albumListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = getSpanCountForAlbumList(
                    resources, currentListItemView, currentListItemSize)

            swipeRefreshLayout.setOnRefreshListener {
                (requireActivity() as AbstractPhotosActivity).mediaProvider.loadAlbum(object : MediaProvider.MediaProviderCallBack {
                    override fun onMediaLoaded(albums: ArrayList<Album>?) {
                        this@AlbumFragment.albumViewModel.notifyAlbumLoaded()
                        swipeRefreshLayout.isRefreshing = false
                    }

                    override fun onHasNoPermission() {
                        (requireActivity() as AbstractPhotosActivity).jumpToMainActivity()
                    }
                })
            }
        }
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val albumListImageItem = menu.findItem(R.id.album_list_item_view_type)
        setAlbumListIcon(albumListImageItem, currentListItemView)

        val currentPreference = preferences.getString(getString(
                R.string.album_list_item_size_key), "0") ?: "0"
        setAlbumListItemSizeOption(resources, menu, currentPreference)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.album_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.album_list_item_view_type -> {
                onItemTypeButtonClicked()
                setAlbumListIcon(item, currentListItemView)
                true
            }
            R.id.album_item_view_size_big -> onItemSizeOptionClicked(item)
            R.id.album_item_view_size_medium -> onItemSizeOptionClicked(item)
            R.id.album_item_view_size_small -> onItemSizeOptionClicked(item)
            R.id.menu_refresh -> onRefreshAlbumList()
            else -> false
        }
    }

    private fun onRefreshAlbumList(): Boolean {
        binding.swipeRefreshLayout.isRefreshing = true
        (requireActivity() as AbstractPhotosActivity).mediaProvider.loadAlbum(object : MediaProvider.MediaProviderCallBack {
            override fun onMediaLoaded(albums: ArrayList<Album>?) {
                this@AlbumFragment.albumViewModel.notifyAlbumLoaded()
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onHasNoPermission() {
                (requireActivity() as AbstractPhotosActivity).jumpToMainActivity()
            }
        })
        return true
    }

    private fun onItemTypeButtonClicked() {
        currentListItemView = if (currentListItemView == AlbumListAdapter.ITEM_TYPE_LIST)
            AlbumListAdapter.ITEM_TYPE_GRID else AlbumListAdapter.ITEM_TYPE_LIST

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.album_list_view_type_key), currentListItemView.toString())
                .apply()

        refreshRecyclerView()
    }

    private fun onItemSizeOptionClicked(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        val options = resources.getStringArray(R.array.photo_list_item_size_value)

        val option = when (menuItem.itemId) {
            R.id.album_item_view_size_big -> options[0]
            R.id.album_item_view_size_medium -> options[1]
            else -> options[2]
        }

        currentListItemSize = option.toInt()

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.album_list_item_size_key), option)
                .apply()

        refreshRecyclerView()

        return true
    }

    private fun refreshRecyclerView() {
        binding.apply {
            val adapter = AlbumListAdapter(albumAdapterListener,
                    currentListItemView, currentListItemSize)
            val recyclerView = albumListLayout.albumListRecyclerView
            val albumList = albumListLayout.albumList

            recyclerView.adapter = adapter
            val layoutManager = albumListLayout.albumListRecyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = getSpanCountForAlbumList(
                    resources, currentListItemView, currentListItemSize)

            bindSampleAlbumListRecyclerView(recyclerView, albumList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

}