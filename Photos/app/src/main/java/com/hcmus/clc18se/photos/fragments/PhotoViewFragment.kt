package com.hcmus.clc18se.photos.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.EditPhotoActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.utils.GPSImage
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoViewFragment : Fragment() {

    private lateinit var viewModel: PhotosViewModel

    private val photos by lazy { viewModel.mediaItemList.value ?: listOf() }

    private val preferences by lazy { (requireActivity() as AbstractPhotosActivity).preferences }

    private val contentProvider by lazy { PhotosDatabase.getInstance(requireContext()).photosDatabaseDao }

    private val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
        FavouriteAlbumViewModelFactory(
                requireActivity().application, contentProvider
        )
    }

    companion object {
        const val PLACES_API_KEY = BuildConfig.PLACES_API_KEY
    }
    private lateinit var binding: FragmentPhotoViewBinding

    private var currentPosition = -1

    private var debug: Boolean = false

    private val viewPagerCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                super.onPageSelected(position)

                (activity as AbstractPhotosActivity).supportActionBar?.title = photos[position].name
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
        this.viewModel = viewModel
    }

    private lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultLauncher = registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                viewModel.deletePendingImage()
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
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_info)
            dialog.findViewById<TextView>(R.id.path).text = "Path: " + photos[currentPosition].requirePath(requireContext())
            dialog.findViewById<TextView>(R.id.date_create).text = "Date create: " + photos[currentPosition].requireDateTaken()
            val gpsImage = GPSImage(photos[currentPosition].requirePath(requireContext()))
            val latitude = gpsImage.Latitude
            val longtitude = gpsImage.Longitude
            if (latitude != null && longtitude != null) {
                val geocoder = Geocoder(context)
                val list = geocoder.getFromLocation(latitude,longtitude,1)
                if (list[0].getAddressLine(0) != null) {
                    dialog.findViewById<TextView>(R.id.name_place).text = "Place: " + list[0].getAddressLine(0)
                }
            }
            dialog.findViewById<Button>(R.id.off_info_dialog).setOnClickListener(View.OnClickListener { dialog.dismiss() })
            dialog.show()
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
        (activity as AbstractPhotosActivity).supportActionBar?.title = photos[viewModel.idx.value!!].name

        setEditButtonVisibility(photos[viewModel.idx.value!!].isEditable())
        currentPosition = viewModel.idx.value!!

        initFavouriteButtonState()

        binding.horizontalViewPager.apply {
            adapter = ScreenSlidePagerAdapter(this@PhotoViewFragment)
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

        viewModel.deleteSucceed.observe(viewLifecycleOwner) { deleteResult ->
            if (deleteResult == true) {
                favouriteAlbumViewModel.requestReloadingData()
                requireActivity().onBackPressed()
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

        // val drawable = ResourcesCompat.getDrawable(resources, resId, requireContext().theme)
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
        (activity as AbstractPhotosActivity).supportActionBar?.show()
        activity?.window?.navigationBarColor = binding.navBarColor

    }

    private inner class ScreenSlidePagerAdapter(fragment: Fragment) :
            FragmentStateAdapter(fragment) {

        val fullscreen =
                preferences.getBoolean(getString(R.string.full_screen_view_image_key), false)

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