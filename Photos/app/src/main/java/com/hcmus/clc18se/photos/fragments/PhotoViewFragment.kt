package com.hcmus.clc18se.photos.fragments

import android.app.Dialog
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
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
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.FavouriteAlbumViewModelFactory
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class PhotoViewFragment : Fragment() {

    private lateinit var viewModel: PhotosViewModel

    companion object {
        const val DELETE_CODE = 2021
    }

    private val photos by lazy { viewModel.mediaItemList.value ?: listOf() }

    private val preferences by lazy { (requireActivity() as AbstractPhotosActivity).preferences }

    private val contentProvider by lazy { PhotosDatabase.getInstance(requireContext()).photosDatabaseDao }

    private val favouriteAlbumViewModel: FavouriteAlbumViewModel by activityViewModels {
        FavouriteAlbumViewModelFactory(
                requireActivity().application, contentProvider
        )
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
        )
        this.viewModel = viewModel
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
            val resolver = requireContext().contentResolver
            var result = 0
            try {
                result = resolver.delete(photos[currentPosition].requireUri(), null, null)
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                            securityException as? RecoverableSecurityException
                                    ?: throw SecurityException()

                    val intentSender =
                            recoverableSecurityException.userAction.actionIntent.intentSender

                    intentSender?.let {
                        startIntentSenderForResult(intentSender, 0, null, 0, 0, 0, null)
                    }
                    result = 1
                } else {
                    throw SecurityException()
                }
            }

            if (result > 0) {
                Toast.makeText(context, "Delete success", Toast.LENGTH_SHORT).show()
                favouriteAlbumViewModel.requestReloadingData()
                requireActivity().onBackPressed()
            } else {
                Toast.makeText(context, "Delete unsuccess", Toast.LENGTH_SHORT).show()
            }
        }

        infoButton.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_info)
            dialog.findViewById<TextView>(R.id.path).text = "Path: " + photos[currentPosition].requirePath(requireContext())
            dialog.findViewById<TextView>(R.id.date_create).text = "Date create: " + photos[currentPosition].requireDateTaken()
            dialog.findViewById<Button>(R.id.off_info_dialog).setOnClickListener(View.OnClickListener { dialog.dismiss() })
            dialog.show()
        }

        shareButton.setOnClickListener {
            if (photos[currentPosition].isVideo()) {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_STREAM, photos[currentPosition].requireUri())
                sendIntent.type = "video/*"
                startActivity(Intent.createChooser(sendIntent, "Send video via:"))
            } else {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_STREAM, photos[currentPosition].requireUri())
                sendIntent.type = "image/*"
                startActivity(Intent.createChooser(sendIntent, "Send image via:"))
            }
        }
    }


//    @RequiresApi(Build.VERSION_CODES.R)
//    fun deleteListUri(uris: List<Uri>) {
//        val pendingIntent = MediaStore.createDeleteRequest(requireContext().contentResolver, uris)
//        startIntentSenderForResult(pendingIntent.intentSender, DELETE_CODE, null, 0, 0, 0, null)
//    }

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

//            val fullscreen = preferences.getBoolean(getString(R.string.full_screen_view_image_key), false)
//            if (fullscreen) {
//                val window = requireActivity().window
//                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
//            }

        binding.horizontalViewPager.apply {
            adapter = ScreenSlidePagerAdapter(this@PhotoViewFragment)
            setCurrentItem(viewModel.idx.value!!, false)
            registerOnPageChangeCallback(viewPagerCallback)
        }

        debug = preferences.getBoolean(getString(R.string.image_debugger_key), false)

        activity?.window?.navigationBarColor = Color.BLACK
        return binding.root
    }

    private fun toggleFavouriteButton() {

        CoroutineScope(Dispatchers.IO).launch {
            val mediaItem = viewModel.mediaItemList.value!![currentPosition]
            val isFavouriteItem = contentProvider.hasFavouriteItem(mediaItem.id)

            if (isFavouriteItem) {
                Timber.d("MediaItem ID{${mediaItem.id}} was removed from favourites")
                contentProvider.removeFavouriteItems(mediaItem.toFavouriteItem())
            } else {
                Timber.d("MediaItem ID{${mediaItem.id}} was added to favourites")
                contentProvider.addFavouriteItems(mediaItem.toFavouriteItem())
            }

            withContext(Dispatchers.Main) {
                changeFavouriteButtonState(!isFavouriteItem)
                favouriteAlbumViewModel.requestReloadingData()
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
        binding.horizontalViewPager.unregisterOnPageChangeCallback(viewPagerCallback)

        super.onDetach()
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

