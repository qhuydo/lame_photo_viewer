package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.MainActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AdapterItem
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import com.l4digital.fastscroll.FastScroller
import timber.log.Timber
import java.util.*

class PhotosFragment : AbstractPhotoListFragment(R.menu.photos_menu) {

    private lateinit var binding: FragmentPhotosBinding
    private val viewModel: PhotosViewModel by activityViewModels {
        PhotosViewModelFactory(requireActivity().application, database)
    }

    override val actionCallbacks = object : MediaItemListAdapter.ActionCallbacks {
        override fun onClick(mediaItem: MediaItem) {
            viewModel.startNavigatingToImageView(mediaItem)
        }

        override fun onSelectionChange() {
            invalidateCab()
        }

        override suspend fun onGroupListItem(items: List<MediaItem>): List<AdapterItem> {
            val groupByKey = getString(R.string.group_by_key)
            val groupOption = preferences.getInt(groupByKey, MediaItemListAdapter.DEFAULT_GROUP_BY)
            val context = requireContext()

            return when (groupOption) {
                MediaItemListAdapter.GROUP_BY_MONTH -> MediaItem.groupByMonth(context, items)
                MediaItemListAdapter.GROUP_BY_YEAR -> MediaItem.groupByYear(context, items)
                else -> MediaItem.groupByDate(context, items)
            }
        }
    }

    override fun getCurrentViewModel(): PhotosViewModel = viewModel

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
        binding = FragmentPhotosBinding.inflate(inflater, container, false)
        photoListBinding = binding.photoListLayout

        setHasOptionsMenu(true)

        adapter = MediaItemListAdapter(
            actionCallbacks,
            currentListItemView,
            currentListItemSize
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.apply {
            lifecycleOwner = this@PhotosFragment

            photosViewModel = viewModel

            photoListLayout.apply {
                photoList = viewModel.mediaItemList.value
                setUpRecyclerView()

                swipeRefreshLayout.setOnRefreshListener {
                    viewModel.loadImages()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        viewModel.mediaItemList.observe(viewLifecycleOwner) { images ->
            if (images != null) {
                Timber.d("viewModel.mediaItemList")
                adapter.filterAndSubmitList(images)
            }
        }

        viewModel.navigateToImageView.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem != null) {
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
                this@PhotosFragment.viewModel.doneNavigatingToImageView()
            }
        }

        return binding.root
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

            val recyclerView = photoListLayout.fastscrollView.recyclerView
            val photoList = viewModel.mediaItemList.value

            recyclerView.adapter = adapter
            (recyclerView.layoutManager as? StaggeredGridLayoutManager)?.apply {
                spanCount = getSpanCountForPhotoList(
                    resources,
                    currentListItemView,
                    currentListItemSize
                )
            }

            photoListLayout.fastscrollView.fastScroller.setSectionIndexer { position ->
                adapter.getSectionText(position)
            }

            bindMediaListRecyclerView(recyclerView, photoList ?: listOf())

            adapter.notifyDataSetChanged()
        }
    }

    override fun getCadSubId(): Int {
        val bottomAppBarPref =
            preferences.getString(getString(R.string.app_bottom_bar_navigation_key), "0")
        val usingTabLayout = bottomAppBarPref == MainActivity.TAB_LAYOUT_OPTION
        return if (usingTabLayout) R.id.cab_stub_tab else R.id.cab_stub
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar.searchActionBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar.appBarLayout

    override fun getToolbarTitleRes(): Int = R.string.photo_title

    private fun setUpRecyclerView() {

        binding.photoListLayout.apply {
            val recyclerView = fastscrollView.recyclerView
            recyclerView.layoutManager = StaggeredGridLayoutManager(
                getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize),
                StaggeredGridLayoutManager.VERTICAL
            )

            fastscrollView.fastScroller.setFastScrollListener(object :
                FastScroller.FastScrollListener {
                override fun onFastScrollStart(fastScroller: FastScroller?) {
                    swipeRefreshLayout.isEnabled = false
                }

                override fun onFastScrollStop(fastScroller: FastScroller?) {
                    swipeRefreshLayout.isEnabled = true
                    (recyclerView.layoutManager as? StaggeredGridLayoutManager)?.apply {
                        invalidateSpanAssignments()
                        requestLayout()
                    }
                }

            })

            fastscrollView.fastScroller.setSectionIndexer { position ->
                adapter.getSectionText(position)
            }

            recyclerView.adapter = adapter
            bindMediaListRecyclerView(recyclerView, photoList)
        }
    }

    override fun isSwipeLayoutEnabled() = true
}