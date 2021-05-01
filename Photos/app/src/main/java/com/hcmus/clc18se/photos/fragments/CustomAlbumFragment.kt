package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
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
import com.hcmus.clc18se.photos.databinding.FragmentCustomAlbumBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class CustomAlbumFragment : BaseFragment() {

    private lateinit var binding: FragmentCustomAlbumBinding

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireActivity()) }

    private var currentListItemView: Int = AlbumListAdapter.ITEM_TYPE_LIST

    private var currentListItemSize: Int = 0

    private val viewModel: CustomAlbumViewModel by activityViewModels {
        CustomAlbumViewModelFactory(
                requireActivity().application,
                PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
        )
    }

    private lateinit var photosViewModel: PhotosViewModel

    private lateinit var customAlbumAdapter: AlbumListAdapter

    private val onClickListener = AlbumListAdapter.OnClickListener {
        // viewModel.
        TODO("not implemented yet")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }

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
        binding = FragmentCustomAlbumBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.customAlbumViewModel = viewModel

        currentListItemView = requireContext().currentAlbumListItemView(preferences)
        currentListItemSize = requireContext().currentAlbumListItemSize(preferences)

        customAlbumAdapter = AlbumListAdapter(currentListItemView,
                currentListItemSize,
                onClickListener)

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
        inflater.inflate(R.menu.custom_album_menu, menu)
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
            else -> false
        }
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
}