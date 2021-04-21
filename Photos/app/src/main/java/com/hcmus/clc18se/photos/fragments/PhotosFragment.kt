package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.MainActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding
import com.hcmus.clc18se.photos.utils.SpaceItemDecoration
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class PhotosFragment : AbstractPhotoListFragment(
        R.menu.photos_menu
) {

    private lateinit var binding: FragmentPhotosBinding
    private val viewModel: PhotosViewModel by activityViewModels() {
        PhotosViewModelFactory(
                requireActivity().application,
                PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
        )
    }
    //private lateinit var viewModel: PhotosViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.loadImages()
    }

    override val onClickListener = object : MediaItemListAdapter.OnClickListener {
        override fun onClick(mediaItem: MediaItem) {

            val viewModel: PhotosViewModel by navGraphViewModels(
                    (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
            ) {
                PhotosViewModelFactory(
                        requireActivity().application,
                        PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
                )
            }
            viewModel.loadDataFromOtherViewModel(this@PhotosFragment.viewModel)

            val idx = viewModel.mediaItemList.value?.indexOf(mediaItem) ?: -1
            viewModel.setCurrentItemView(idx)
            this@PhotosFragment.findNavController().navigate(
                    PhotosFragmentDirections.actionPagePhotoToPhotoViewFragment()
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

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photos, container, false
        )

        photoListBinding = binding.photoListLayout

        currentListItemView = preferences.getString(getString(R.string.photo_list_view_type_key),
                MediaItemListAdapter.ITEM_TYPE_LIST.toString())!!.toInt()

        currentListItemSize = preferences.getString(getString(R.string.photo_list_item_size_key),
                "0")!!.toInt()

        setHasOptionsMenu(true)

        adapter = MediaItemListAdapter(onClickListener,
                currentListItemView,
                currentListItemSize)


        binding.apply {
            lifecycleOwner = this@PhotosFragment

            photosViewModel = viewModel

            photoListLayout.apply {
                photoList = viewModel.mediaItemList.value
                photoListRecyclerView.adapter = adapter

                photoListRecyclerView.addItemDecoration(
                        SpaceItemDecoration(resources.getDimension(R.dimen.photo_list_item_margin).toInt())
                )

                (photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                    spanCount = getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)
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

        return binding.root
    }

    override fun refreshRecyclerView() {
        binding.apply {
            adapter = MediaItemListAdapter(onClickListener,
                    currentListItemView,
                    currentListItemSize)

            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = viewModel.mediaItemList.value

            recyclerView.adapter = adapter
            (photoListLayout.photoListRecyclerView.layoutManager as GridLayoutManager).apply {
                spanCount = getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)
            }

            bindMediaListRecyclerView(recyclerView, photoList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

    override fun getCadSubId(): Int {
        val bottomAppBarPref = preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0")
        val usingTabLayout = bottomAppBarPref == MainActivity.TAB_LAYOUT_OPTION
        return if (usingTabLayout) R.id.cab_stub_tab else R.id.cab_stub
    }

}