package com.hcmus.clc18se.photos.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.EditPhotoActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.DialogInfo2Binding
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.utils.OnBackPressed
import com.hcmus.clc18se.photos.utils.OnDirectionKeyDown
import com.hcmus.clc18se.photos.utils.images.GPSImage
import com.hcmus.clc18se.photos.utils.images.GPSImage.Companion.getAddressFromGPSImage
import com.hcmus.clc18se.photos.viewModels.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

// TODO: create a view model for this
class PhotoViewFragment : BaseFragment(), OnDirectionKeyDown, OnBackPressed {

    companion object {
        const val BUNDLE_CURRENT_POS = "current_position"
    }

    private lateinit var viewModel: PhotosViewModel

    private lateinit var photos: List<MediaItem>

    private val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
        FavouriteAlbumViewModelFactory(requireActivity().application, database)
    }

    private val secretPhotosViewModel: SecretPhotosViewModel by activityViewModels {
        SecretViewModelFactory(requireActivity().application)
    }

    private lateinit var binding: FragmentPhotoViewBinding

    private var currentPosition = -1

    private var debug: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val args by lazy { PhotoViewFragmentArgs.fromBundle(requireArguments()) }

    private val isSecret by lazy { args.isSecret }

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

                if (!isSecret) {
                    setEditButtonVisibility(photos[position].isEditable())
                    initFavouriteButtonState()
                }
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
        if (isSecret) {
            val hiddenButtons = listOf(editButton, heartButton, shareButton, editButton)
            hiddenButtons.forEach { it.visibility = View.GONE }
            infoButton.setOnClickListener { actionDisplayInfo() }
            nukeButton.setOnClickListener { actionRemoveSecret() }
        } else {
            editButton.setOnClickListener { actionEdit() }
            heartButton.setOnClickListener { actionFavourite() }
            nukeButton.setOnClickListener { actionPermanentRemove() }
            infoButton.setOnClickListener { actionDisplayInfo() }
            shareButton.setOnClickListener { actionShare() }
        }
    }

    internal fun actionRemoveSecret() {
        MaterialDialog(requireContext()).show {
            title(R.string.delete_warning_dialog_title)
            message(R.string.delete_warning_dialog_msg)
            positiveButton(R.string.yes) {
                viewModel.deleteSecretPhoto(photos[currentPosition])
            }
            negativeButton(R.string.no) {}
        }
    }

    private fun actionEdit() {
        val intent = Intent(context, EditPhotoActivity::class.java)
        intent.putExtra("uri", photos[currentPosition].requireUri())
        startActivity(intent)
    }

    private fun actionShare() {
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

    private fun actionPermanentRemove() {
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

    private fun actionDisplayInfo() = MaterialDialog(requireContext()).show {
        lifecycleOwner(this@PhotoViewFragment)

        val dialogBinding = DialogInfo2Binding.inflate(layoutInflater, view, false)
        setContentView(dialogBinding.root)

        val photoPath = photos[currentPosition].requirePath(requireContext())
        val photoDate = photos[currentPosition].getDateSorted()
        val photoSize = getFileSize(photoPath!!)

        dialogBinding.apply {
            path.text = photoPath
            date.text = "$photoDate"
            size.visibility = View.VISIBLE
            size.text = photoSize

            // get image resolution
            if (photos[currentPosition].isSupportedStaticImage()) {
                getImageSize(photoPath).let { imageSize ->
                    resolution.visibility = View.VISIBLE
                    resolution.text = imageSize
                }
            }

            // only display the address & coordinator row
            // when the photo contains Exif
            if (photos[currentPosition].isSupportExif()) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // request exif reading permission
                    // when the permission is approved, call getMediaItemAddressCallback()
                    getMediaItemAddress()
                }
                else {
                    getLatLong()?.let { latLong ->
                        Timber.d(latLong)
                        geoPlace.visibility = View.VISIBLE
                        geoPlace.text = latLong
                    }

                    getMediaItemAddress()?.let { address ->
                        Timber.d(address)
                        namePlace.visibility = View.VISIBLE
                        namePlace.text = address
                    }
                }

            } else {
                addressRow.visibility = View.GONE
                coordinateRow.visibility = View.GONE
            }

        }

        this@PhotoViewFragment.dialogBinding = dialogBinding
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
        } catch (ignored: Exception) {
        }

        return sizeString
    }

    private fun getFileSize(path: String): String? {
        var sizeString: String? = null
        try {
            val file = File(path)
            var size: Double = file.length().toDouble()
            var count = 0
            while (size > 1024) {
                size /= 1024
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

    private var dialogBinding: DialogInfo2Binding? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMediaItemAddressCallback() {

        val uri = MediaStore.setRequireOriginal(photos[currentPosition].requireUri())
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val gpsImage = GPSImage(inputStream)
                if (gpsImage.latitude != null && gpsImage.longitude != null) {
                    val latLng = "(${String.format("%.4f", gpsImage.latitude)};${
                        String.format(
                            "%.4f",
                            gpsImage.longitude
                        )
                    })"

                    dialogBinding?.geoPlace?.text = latLng
                }

                getAddressFromGPSImage(gpsImage, requireContext())?.let { address ->
                    dialogBinding?.namePlace?.text = address
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

        debug = preferences.getBoolean(getString(R.string.image_debugger_key), false)

        startSlideShow()

        // requireActivity().window?.navigationBarColor = Color.BLACK
        return binding.root
    }

    private fun startSlideShow() {
        if (viewModel.liveShow) {
            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        if (currentPosition < photos.size - 1) {
                            ++currentPosition
                        } else {
                            return@withContext
                        }
                        binding.horizontalViewPager.setCurrentItem(currentPosition, true)
                    }
                }
            }
        }
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
            setCurrentItem(currentPosition, false)
            registerOnPageChangeCallback(viewPagerCallback)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (currentPosition == -1) {
            currentPosition = viewModel.idx.value!!
        }

        if (!isSecret) {
            setEditButtonVisibility(photos[currentPosition].isEditable())
            initFavouriteButtonState()
        }

        initViewPager()

        (activity as? AppCompatActivity)?.supportActionBar?.title = photos[currentPosition].name

        initObservers()
        if (savedInstanceState?.containsKey(BUNDLE_CURRENT_POS) == true) {
            val pos = savedInstanceState.getInt(BUNDLE_CURRENT_POS)
            if (pos != -1) {
                currentPosition = pos
                binding.horizontalViewPager.currentItem = pos
            }
        }
    }

    private fun actionFavourite() {
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
                secretPhotosViewModel.requestReloadingData()

                if (photos.isEmpty()) {
                    requireActivity().onBackPressed()
                    return@observe
                }

                if (currentPosition >= photos.size) {
                    currentPosition = photos.size - 1
                }

                binding.horizontalViewPager.adapter?.notifyItemRemoved(currentPosition)
                (activity as? AppCompatActivity)?.supportActionBar?.title =
                    photos[currentPosition].name

            } else if (deleteResult == false) {
                Toast.makeText(requireContext(), "Failed to remove item", Toast.LENGTH_SHORT).show()
            }
            if (deleteResult != null) {
                viewModel.finishPerformingDelete()
            }
        }
    }

    override fun onBackPress(): Boolean {
        if (viewModel.liveShow) {
            // thread.interrupt()
            if (scope.isActive) {
                scope.cancel()
            }
            viewModel.liveShow = false
        }
        return false
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

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_CURRENT_POS, currentPosition)
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

        override fun createFragment(position: Int) = PhotoViewPagerFragment().also {
            val mediaItem = photos[position]

            it.mediaItem = mediaItem
            it.debug = debug
            it.fullScreen = fullscreen
            it.isSecret = isSecret
        }
    }

}