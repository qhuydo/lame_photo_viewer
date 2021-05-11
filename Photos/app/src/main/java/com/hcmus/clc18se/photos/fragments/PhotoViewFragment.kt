package com.hcmus.clc18se.photos.fragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.EditPhotoActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.utils.OnDirectionKeyDown
import com.hcmus.clc18se.photos.utils.images.GPSImage
import com.hcmus.clc18se.photos.utils.images.GPSImage.Companion.getAddressFromGPSImage
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

// TODO: create a view model for this
class PhotoViewFragment : BaseFragment(), OnDirectionKeyDown {

    private lateinit var viewModel: PhotosViewModel

    private lateinit var photos: List<MediaItem>

    private val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
        FavouriteAlbumViewModelFactory(requireActivity().application, database)
    }

    private lateinit var binding: FragmentPhotoViewBinding

    private var currentPosition = -1

    private var debug: Boolean = false

    private val viewPagerCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                if (photos.isEmpty()) {
                    requireActivity().onBackPressed()
                    return
                }

                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    photos[position].name
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

        // Từ phiên bản Android Q trở đi, khi truy cập EXIF location cần phải được HĐH cho phép
        // trong thời gian chạy.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            accessMediaLocationResultLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
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
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                MaterialDialog(requireContext()).show {
                    title(R.string.delete_warning_dialog_title)
                    message(R.string.delete_warning_dialog_msg)
                    positiveButton(R.string.yes) {
                        viewModel.deleteImage(photos[currentPosition])
                    }
                    negativeButton(R.string.no) {}
                }
            } else {
                viewModel.deleteImage(photos[currentPosition])
            }
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

            val dateCreated = photos[currentPosition].getDateSorted()
            findViewById<TextView>(R.id.date_create).text =
                resources.getString(R.string.date_created, dateCreated)

            val size = getFileSize(path!!)
            findViewById<TextView>(R.id.size)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.size)?.text = getString(R.string.size, size)

            if (!photos[currentPosition].isVideo()) {
                val imageSize = getImageSize(path)
                imageSize?.let {
                    findViewById<TextView>(R.id.size_image)?.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.size_image)?.text =
                        getString(R.string.image_size, imageSize)
                }
            }
        }

        val address: String? = getMediaItemAddress()
        address?.let {
            Timber.d(it)
            dialog?.findViewById<TextView>(R.id.name_place)?.visibility = View.VISIBLE
            dialog?.findViewById<TextView>(R.id.name_place)?.text = getString(R.string.location, it)
        }

        val latlng: String? = getLatLong()
        latlng?.let {
            Timber.d(it)
            dialog?.findViewById<TextView>(R.id.geo_place)?.visibility = View.VISIBLE
            dialog?.findViewById<TextView>(R.id.geo_place)?.text = getString(R.string.geo, it)
        }

        dialog?.findViewById<Button>(R.id.off_info_dialog)?.setOnClickListener {
            dialog?.dismiss()
            dialog = null
        }
        dialog?.show()
    }

    private fun getImageSize(path: String): String? {
        var sizeString: String? = null

        val gpsImage = GPSImage(path)
        try {
            val length = gpsImage.exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            val width = gpsImage.exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            if (length != null && width != null) {
                sizeString = "$length × $width"
            }
        } catch (ignored: Exception) { }

        return sizeString
    }

    private fun getFileSize(path: String): String? {
        var sizeString: String? = null
        try {
            val file = File(path)
            var size: Double = file.length().toDouble()
            var count = 0
            while (size > 1024) {
                size = size / 1024
                count++
            }
            sizeString = String.format("%.1f", size)
            when (count) {
                0 -> sizeString += "B"
                1 -> sizeString += "KB"
                2 -> sizeString += "MB"
                3 -> sizeString += "GB"
                4 -> sizeString += "TB"
            }
        } catch (e: Exception) {

        }
        return sizeString
    }

    private fun getMediaItemAddress(): String? {
        var address: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            Timber.d("ACCESS_MEDIA_LOCATION is a dangerous, request it at runtime ")
            accessMediaLocationResultLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            val path = photos[currentPosition].requirePath(requireContext())
            if (path != null) {
                val gpsImage = GPSImage(path)
                address = getAddressFromGPSImage(gpsImage, requireContext())
                Toast.makeText(
                    context,
                    gpsImage.exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        return address
    }

    private fun getLatLong(): String? {
        var latLng: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            Timber.d("ACCESS_MEDIA_LOCATION is a dangerous, request it at runtime ")
            accessMediaLocationResultLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            val path = photos[currentPosition].requirePath(requireContext())
            if (path != null) {
                val gpsImage = GPSImage(path)
                if (gpsImage.latitude != null && gpsImage.longitude != null) {
                    latLng = "(${String.format("%.4f", gpsImage.latitude)};${
                        String.format(
                            "%.4f",
                            gpsImage.longitude
                        )
                    })"
                }
            }
        }
        return latLng
    }

    private var dialog: Dialog? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMediaItemAddressCallback() {

        val uri = MediaStore.setRequireOriginal(photos[currentPosition].requireUri())
        try {
            requireContext().contentResolver.openInputStream(uri).use { inputStream ->
                inputStream?.let {
                    val gpsImage = GPSImage(it)
                    getAddressFromGPSImage(gpsImage, requireContext())?.let { address ->
                        dialog?.findViewById<TextView>(R.id.name_place)?.text =
                            resources.getString(R.string.location, address)
                    }
                }
            }
        } catch (ex: FileNotFoundException) {
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
            // navBarColor = activity?.window?.navigationBarColor ?: Color.BLACK
        }

        setUpBottomButtons()

        setEditButtonVisibility(photos[viewModel.idx.value!!].isEditable())
        currentPosition = viewModel.idx.value!!

        initFavouriteButtonState()

        initViewPager()

        debug = preferences.getBoolean(getString(R.string.image_debugger_key), false)

        // requireActivity().window?.navigationBarColor = Color.BLACK
        return binding.root
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (currentPosition > 0) {
                    binding.horizontalViewPager.setCurrentItem(currentPosition--, true)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (currentPosition < viewModel.mediaItemList.value!!.size) {
                    binding.horizontalViewPager.setCurrentItem(currentPosition++, true)
                    return true
                }
            }
        }

        return false
    }

    private fun initViewPager() {
        binding.horizontalViewPager.apply {
            adapter = ScreenSlidePagerAdapter(childFragmentManager, lifecycle)
            setCurrentItem(viewModel.idx.value!!, false)
            registerOnPageChangeCallback(viewPagerCallback)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            photos[viewModel.idx.value!!].name
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
            val isFavouriteItem = database.hasFavouriteItem(mediaItem.id)

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
        // activity?.window?.navigationBarColor = binding.navBarColor

    }

    private inner class ScreenSlidePagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        val fullscreen =
            preferences.getBoolean(getString(R.string.full_screen_view_image_key), false)

        override fun getItemId(position: Int): Long {
            return photos[position].id
        }

        override fun getItemCount(): Int {
            return photos.size
        }

        override fun containsItem(itemId: Long): Boolean {
            return photos.any { item -> item.id == itemId }
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

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout
}