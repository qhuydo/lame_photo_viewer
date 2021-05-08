package com.hcmus.clc18se.photos.fragments

import android.app.Activity
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.afollestad.materialdialogs.MaterialDialog
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.data.deleteMultipleMediaItems
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.PhotoListBinding
import com.hcmus.clc18se.photos.databinding.PhotoListFastScrollerBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.utils.ui.isColorDark
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


/**
 * Inherited by [PhotoListFragment] & [PhotosFragment]
 *
 * [PhotoListFragment] & [PhotosFragment] both have very similar implementation & layout structure.
 * The main different between them is [PhotosFragment] is the top destination but [PhotoListFragment]
 * is not.
 *
 * [AbstractPhotoListFragment] implements the option menu initialization, menu item action,
 * actionMode.
 * ViewModels, binding & recycler view behaviour are implemented in inherit classes.
 *
 * @param menuRes resource ID of the toolbar menu
 * - Parameter value provided by [PhotoListFragment] is [R.menu.photo_list_menu]
 * - And provided by [PhotosFragment] is [R.menu.photos_menu]
 *
 * @see [PhotosFragment]
 * @see [PhotoListFragment]
 */
abstract class AbstractPhotoListFragment(
        private val menuRes: Int
) : BaseFragment(), OnBackPressed {

    protected val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    // Current list item view type, get from preference
    protected var currentListItemView: Int = MediaItemListAdapter.ITEM_TYPE_LIST

    // Current list item size, get from preference
    protected var currentListItemSize: Int = MediaItemListAdapter.ITEM_SIZE_MEDIUM

    // On click listener object used by MediaItemListAdapter
    abstract val actionCallbacks: MediaItemListAdapter.ActionCallbacks

    // Used in refreshRecyclerView()
    // Must be init in onCreateView() from the inherit class
    protected lateinit var photoListBinding: ViewDataBinding

    protected lateinit var adapter: MediaItemListAdapter

    internal var mainCab: AttachedCab? = null

    private lateinit var deleteRequestLauncher: ActivityResultLauncher<IntentSenderRequest>

    // get the current viewModel of the fragment
    abstract fun getCurrentViewModel(): PhotosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentListItemView = preferences.getString(
                getString(R.string.photo_list_view_type_key),
                MediaItemListAdapter.ITEM_TYPE_LIST.toString()
        )!!.toInt()

        currentListItemSize = preferences.getString(
                getString(R.string.photo_list_item_size_key),
                "0"
        )!!.toInt()

        deleteRequestLauncher = registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            if (Activity.RESULT_OK == activityResult.resultCode) {
                Toast.makeText(activity, "Selected images deleted", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(menuRes, menu)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        mainCab?.destroy()
        super.onConfigurationChanged(newConfig)
    }

    /**
     * Set the icon in the toolbar to match with the current list item type in photo_list
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val photoListImageItem = menu.findItem(R.id.photo_list_item_view_type)
        setPhotoListIcon(photoListImageItem, currentListItemView)

        val currentPreference = preferences.getString(
                getString(
                        R.string.photo_list_item_size_key
                ), "0"
        ) ?: "0"
        setPhotoListItemSizeOption(resources, menu, currentPreference)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.photo_list_item_view_type -> {
                onItemTypeButtonClicked()
                setPhotoListIcon(item, currentListItemView)
                true
            }
            R.id.item_view_size_big -> onItemSizeOptionClicked(item)
            R.id.item_view_size_medium -> onItemSizeOptionClicked(item)
            R.id.item_view_size_small -> onItemSizeOptionClicked(item)
            R.id.menu_refresh -> onRefreshPhotoList()
            else -> false
        }
    }

    private fun onRefreshPhotoList(): Boolean {
        when (photoListBinding) {
            is PhotoListBinding -> {
                (photoListBinding as PhotoListBinding).apply {
                    swipeRefreshLayout.isRefreshing = true
                    adapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
            is PhotoListFastScrollerBinding -> {
                (photoListBinding as PhotoListFastScrollerBinding).apply {
                    swipeRefreshLayout.isRefreshing = true
                    adapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        return true
    }

    private fun onItemTypeButtonClicked() {
        currentListItemView = if (currentListItemView == MediaItemListAdapter.ITEM_TYPE_LIST)
            MediaItemListAdapter.ITEM_TYPE_GRID else MediaItemListAdapter.ITEM_TYPE_LIST

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.photo_list_view_type_key), currentListItemView.toString())
                .apply()

        refreshRecyclerView()
    }

    private fun onItemSizeOptionClicked(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        val options = resources.getStringArray(R.array.photo_list_item_size_value)

        val option = when (menuItem.itemId) {
            R.id.item_view_size_big -> options[0]
            R.id.item_view_size_medium -> options[1]
            else -> options[2]
        }

        currentListItemSize = option.toInt()

        // Save the preference
        preferences.edit()
                .putString(getString(R.string.photo_list_item_size_key), option)
                .apply()

        refreshRecyclerView()

        return true
    }

    override fun onStop() {
        super.onStop()
        mainCab?.destroy()
    }

    abstract fun refreshRecyclerView()

    abstract fun getCadSubId(): Int

    fun invalidateCab() {
        if (adapter.numberOfSelectedItems() == 0) {
            mainCab?.destroy()
            // mainCab = null
            return
        }

        if (mainCab.isActive()) {
            mainCab?.apply {
                title(literal = "${adapter.numberOfSelectedItems()}")
            }
        } else {
            val colorPrimary = getColorAttribute(requireContext(), R.attr.colorPrimary)
            Timber.d("Color primary $colorPrimary")
            val colorOnPrimary = getColorAttribute(requireContext(), R.attr.colorOnPrimary)
            Timber.d("Color On primary $colorOnPrimary")

            mainCab = createCab(getCadSubId()) {
                title(literal = "${adapter.numberOfSelectedItems()}")

                menu(R.menu.photo_list_context_menu)
                popupTheme(R.style.Theme_Photos_Indigo)
                titleColor(literal = colorOnPrimary)
                subtitleColor(literal = colorOnPrimary)
                backgroundColor(literal = colorPrimary)
                slideDown()

                onCreate { _, menu -> onPrepareCabMenu(menu) }
                onSelection { onCabItemSelected(it) }
                onDestroy {
                    requireActivity().window.statusBarColor = getColorAttribute(requireContext(), R.attr.statusBarBackground)
                    val statusBackground = getColorAttribute(requireContext(), R.attr.statusBarBackground)
                    if (isColorDark(statusBackground)) {
                        setLightStatusBar(requireActivity().window.decorView, requireActivity())
                    } else {
                        unsetLightStatusBar(requireActivity().window.decorView, requireActivity())
                    }

                    adapter.finishSelection()
                    mainCab = null
                    true
                }
            }
        }
    }

    private fun onCabItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_multiple_delete -> {
                onActionRemoveMediaItems()
                true
            }
            R.id.action_add_to_favourite -> {
                onActionAddToFavourite()
                true
            }
            R.id.action_remove_favourite -> {
                onActionRemoveFromToFavourite()
                true
            }
            R.id.action_select_all -> {
                lifecycleScope.launch {
                    adapter.selectAll()
                    invalidateCab()
                }
                true
            }
            else -> false
        }
    }

    private fun onActionAddToFavourite() {
        val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
            FavouriteAlbumViewModelFactory(requireActivity().application, database)
        }
        favouriteAlbumViewModel.viewModelScope.launch {

            favouriteAlbumViewModel.addToFavouriteAlbum(adapter.getSelectedItems())
            withContext(Dispatchers.Main) {
                mainCab?.destroy()
                Toast.makeText(requireContext(), getString(R.string.action_add_to_favourite_succeed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onActionRemoveFromToFavourite() {

        MaterialDialog(requireContext()).show {

            title(R.string.remove_favourite_dialog_title)
            message(R.string.remove_favourite_dialog_msg)
            positiveButton(R.string.delete_title) {

                val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
                    FavouriteAlbumViewModelFactory(requireActivity().application, database)
                }

                favouriteAlbumViewModel.viewModelScope.launch {
                    favouriteAlbumViewModel.removeFromFavourite(adapter.getSelectedItems())
                    withContext(Dispatchers.Main) {
                        mainCab?.destroy()
                        Toast.makeText(requireContext(), getString(R.string.action_remove_from_favourite_succeed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            negativeButton(R.string.cancel) { }

        }

    }

    private fun onActionRemoveMediaItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uriList = adapter.getSelectedItems().map { it.requireUri() }
            requestDeletePermission(uriList)
        } else {
            val parentActivity = requireActivity() as? AbstractPhotosActivity
            if (parentActivity?.haveWriteStoragePermission() == true) {
                showDeleteWarningDialog()
            } else {
                parentActivity?.jumpToMainActivity()
            }

        }
    }

    private fun showDeleteWarningDialog() {
        MaterialDialog(requireContext()).show {

            title(R.string.delete_warning_dialog_title)
            message(R.string.delete_warning_dialog_msg)
            positiveButton(R.string.yes) {
                GlobalScope.launch {
                    requireContext().contentResolver.deleteMultipleMediaItems(adapter.getSelectedItems())
                    withContext(Dispatchers.Main) {
                        mainCab?.destroy()
                    }
                }
            }
            negativeButton(R.string.no) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestDeletePermission(uriList: List<Uri>) {
        val pi = MediaStore.createDeleteRequest(requireContext().contentResolver, uriList)
        deleteRequestLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteRequestLauncher.unregister()
    }

    override fun onBackPress(): Boolean {
        mainCab?.let {
            it.destroy()
            return true
        } ?: return false
    }

    open fun onPrepareCabMenu(menu: Menu) {
        // set the status bar color to match with ?colorPrimary
        // to match with status bar color
        requireActivity().window.statusBarColor = getColorAttribute(requireContext(), R.attr.colorPrimary)
        val onPrimaryColor = getColorAttribute(requireContext(), R.attr.colorOnPrimary)
        if (isColorDark(onPrimaryColor)) {
            setLightStatusBar(requireActivity().window.decorView, requireActivity())
        } else {
            unsetLightStatusBar(requireActivity().window.decorView, requireActivity())
        }
    }
}