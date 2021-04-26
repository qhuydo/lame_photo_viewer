package com.hcmus.clc18se.photos.fragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.BuildConfig
import com.hcmus.clc18se.photos.EditPhotoActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.utils.GPSImage
import com.hcmus.clc18se.photos.utils.GPSImage.Companion.getAddressFromGPSImage
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

// TODO: create a viewmodel for this
class PhotoViewFragment : Fragment() {

    private lateinit var viewModel: PhotosViewModel

    private lateinit var photos: List<MediaItem>

    private val preferences by lazy { (requireActivity() as AbstractPhotosActivity).preferences }

    private val contentProvider by lazy { PhotosDatabase.getInstance(requireContext()).photosDatabaseDao }

    private val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
        FavouriteAlbumViewModelFactory(
                requireActivity().application, contentProvider
        )
    }

    companion object {
        const val PLACES_API_KEY = BuildConfig.PLACES_API_KEY
        const val ACCESS_MEDIA_LOCATION_REQUEST_CODE = 0x2704
    }

    private lateinit var binding: FragmentPhotoViewBinding

    private var currentPosition = -1

    private var debug: Boolean = false

    private val viewPagerCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                super.onPageSelected(position)

                (requireActivity() as AppCompatActivity).supportActionBar?.title = photos[position].name
                setEditButtonVisibility(photos[position].isEditable())
                initFavouriteButtonState()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                    requireActivity().application,
                    PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        this.photos = viewModel.mediaItemList.value ?: listOf()
        this.viewModel = viewModel
    }

    private lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var accessMediaLocationResultLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultLauncher = registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                viewModel.deletePendingImage()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            accessMediaLocationResultLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission(),
            ) {
                if (it) {
                    getMediaItemAddressCallback()
                }
            }

        }

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    private fun setUpBottomButtons() = binding.bottomLayout.apply {
        editButton.setOnClickListener {
            val intent = Intent(context, EditPhotoActivity::class.java)
            intent.putExtra("uri", photos[currentPosition].requireUri())
            startActivity(intent)
        }

        heartButton.setOnClickListener {
            toggleFavouriteButton()
        }

        nukeButton.setOnClickListener {
            viewModel.deleteImage(viewModel.mediaItemList.value!![currentPosition])
        }

        infoButton.setOnClickListener {
            displayInfoDialog()
        }

        shareButton.setOnClickListener {

            if (photos[currentPosition].isVideo()) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, photos[currentPosition].requireUri())
                    type = "video/*"
                }
                startActivity(Intent.createChooser(sendIntent, "Send video via:"))
            } else {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, photos[currentPosition].requireUri())
                    type = "image/*"
                }
                startActivity(Intent.createChooser(sendIntent, "Send image via:"))
            }
        }
    }

    private fun displayInfoDialog() {
        dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_info)

            val path = photos[currentPosition].requirePath(requireContext())
            findViewById<TextView>(R.id.path).text = resources.getString(R.string.path, path)

            val dateCreated = photos[currentPosition].requireDateTaken()
            findViewById<TextView>(R.id.date_create).text = resources.getString(R.string.date_created, dateCreated)
        }

        val address: String? = getMediaItemAddress()
        address?.let {
            Timber.d(it)
            dialog?.findViewById<TextView>(R.id.name_place)?.text = resources.getString(R.string.location, it)
        }

        dialog?.findViewById<Button>(R.id.off_info_dialog)?.setOnClickListener {
            dialog?.dismiss()
            dialog = null
        }
        dialog?.show()
    }

    private fun getMediaItemAddress(): String? {
        var address: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            Timber.d("ACCESS_MEDIA_LOCATION is a dangerous, request it at runtime ")
            accessMediaLocationResultLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            val path = photos[currentPosition].requirePath(requireContext())
            path?.let {
                val gpsImage = GPSImage(it)
                address = getAddressFromGPSImage(gpsImage, requireContext())
            }
        }
        return address
    }

    // sorry every one for this but i will fix this mess soon :<
    private var dialog: Dialog? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMediaItemAddressCallback() {

        val uri = MediaStore.setRequireOriginal(photos[currentPosition].requireUri())
        requireContext().contentResolver.openInputStream(uri).use { inputStream ->
            inputStream?.let {
                val gpsImage = GPSImage(it)
                val address = getAddressFromGPSImage(gpsImage, requireContext())
                address?.let {
                    dialog?.findViewById<TextView>(R.id.name_place)?.text = resources.getString(R.string.location, address)
                }
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // TODO: clean this code
        (activity as AbstractPhotosActivity).setNavHostFragmentTopMargin(0)

        binding = FragmentPhotoViewBinding.inflate(inflater, container, false)

        binding.apply {
            lifecycleOwner = this@PhotoViewFragment
            photosViewModel = viewModel
            navBarColor = activity?.window?.navigationBarColor ?: Color.BLACK
        }

        setUpBottomButtons()
        (activity as? AppCompatActivity)?.supportActionBar?.title = photos[viewModel.idx.value!!].name

        setEditButtonVisibility(photos[viewModel.idx.value!!].isEditable())
        currentPosition = viewModel.idx.value!!

        initFavouriteButtonState()

        binding.horizontalViewPager.apply {
            adapter = ScreenSlidePagerAdapter(childFragmentManager, lifecycle)
            setCurrentItem(viewModel.idx.value!!, false)
            registerOnPageChangeCallback(viewPagerCallback)
        }

        debug = preferences.getBoolean(getString(R.string.image_debugger_key), false)

        requireActivity().window?.navigationBarColor = Color.BLACK
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
    }

    private fun toggleFavouriteButton() {
        viewModel.changeFavouriteState(currentPosition)
    }

    private fun initObservers() {
        viewModel.itemFavouriteStateChanged.observe(viewLifecycleOwner) { newState ->
            if (newState != null) {
                changeFavouriteButtonState(newState)
                favouriteAlbumViewModel.requestReloadingData()
                viewModel.finishChangingFavouriteItemState()
            }
        }

        viewModel.permissionNeededForDelete.observe(viewLifecycleOwner) { intentSender ->
            intentSender?.let {
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                resultLauncher.launch(intentSenderRequest)
            }
        }

        viewModel.mediaItemList.observe(viewLifecycleOwner) {
            if (it != null) {
                photos = it
            }
        }

        viewModel.deleteSucceed.observe(viewLifecycleOwner) { deleteResult ->
            if (deleteResult == true) {
                favouriteAlbumViewModel.requestReloadingData()
                binding.horizontalViewPager.adapter?.notifyItemRemoved(currentPosition)

            } else if (deleteResult == false) {
                Toast.makeText(requireContext(), "Failed to remove item", Toast.LENGTH_SHORT).show()
            }
            if (deleteResult != null) {
                viewModel.finishPerformingDelete()
            }
        }
    }

    private fun changeFavouriteButtonState(isFavourite: Boolean) {
        val resId = if (isFavourite) {
            R.drawable.ic_baseline_favorite_24
        } else {
            R.drawable.ic_outline_favorite_border_24
        }

        binding.bottomLayout.heartButton.setImageResource(resId)
    }

    private fun initFavouriteButtonState() {
        lifecycleScope.launch {
            val mediaItem = viewModel.mediaItemList.value!![currentPosition]
            val isFavouriteItem = contentProvider.hasFavouriteItem(mediaItem.id)

            withContext(Dispatchers.Main) {
                changeFavouriteButtonState(isFavouriteItem)
            }
        }
    }

    internal fun setBottomToolbarVisibility(visibility: Boolean) {
        if (visibility) {
            binding.bottomLayout.layout.visibility = View.VISIBLE
        } else {
            binding.bottomLayout.layout.visibility = View.INVISIBLE
        }
    }

    internal fun setEditButtonVisibility(visibility: Boolean?) {
        binding.bottomLayout.apply {
            editButton.visibility = if (visibility == false) View.GONE else View.VISIBLE
            this.root.requestLayout()
            this.root.invalidate()
        }
    }

    override fun onDetach() {
        super.onDetach()

        binding.horizontalViewPager.unregisterOnPageChangeCallback(viewPagerCallback)
        resultLauncher.unregister()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            accessMediaLocationResultLauncher.unregister()
        }
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        activity?.window?.navigationBarColor = binding.navBarColor

    }

    private inner class ScreenSlidePagerAdapter(
            fragmentManager: FragmentManager,
            lifecycle: Lifecycle
    ) :
            FragmentStateAdapter(fragmentManager, lifecycle) {

        val fullscreen =
                preferences.getBoolean(getString(R.string.full_screen_view_image_key), false)

        override fun getItemId(position: Int): Long {
            return photos[position].id
        }

        override fun getItemCount(): Int {
            return photos.size
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = PhotoViewPagerFragment()

            val mediaItem = photos[position]

            fragment.mediaItem = mediaItem
            fragment.debug = debug
            fragment.fullScreen = fullscreen
            return fragment
        }
    }
}