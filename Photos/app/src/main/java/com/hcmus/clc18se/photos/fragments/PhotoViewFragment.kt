package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.EditPhotoActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel


class PhotoViewFragment : Fragment() {
    private lateinit var viewModel: PhotosViewModel

    private val photos by lazy { viewModel.mediaItemList.value ?: listOf() }
    private val preferences by lazy { (requireActivity() as AbstractPhotosActivity).preferences }
    private lateinit var binding: FragmentPhotoViewBinding
    private var positionCurrent: Int? = null

    private var debug: Boolean = false

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

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        (activity as AbstractPhotosActivity).setNavHostFragmentTopMargin(0)

        binding = FragmentPhotoViewBinding.inflate(inflater, container, false)

        // setBottomToolbarVisibility(false)

        binding.apply {
            lifecycleOwner = this@PhotoViewFragment
            photosViewModel = viewModel
            positionCurrent = viewModel.idx.value

            navBarColor = activity?.window?.navigationBarColor ?: Color.BLACK
        }

        binding.horizontalViewPager.apply {
            adapter = ScreenSlidePagerAdapter(this@PhotoViewFragment)

//            val fullscreen = preferences.getBoolean(getString(R.string.full_screen_view_image_key), false)
//            if (fullscreen) {
//                val window = requireActivity().window
//                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
//            }
            setCurrentItem(viewModel.idx.value!!, false)
            (activity as AbstractPhotosActivity).supportActionBar?.title = photos[viewModel.idx.value!!].name
            setEditButtonVisibility(photos[viewModel.idx.value!!].isEditable())

            binding.horizontalViewPager.registerOnPageChangeCallback(object :
                    ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    positionCurrent = position
                    super.onPageSelected(position)

                    (activity as AbstractPhotosActivity).supportActionBar?.title = photos[position].name
                    setEditButtonVisibility(photos[position].isEditable())

                }
            })

        }

        binding.bottomLayout.editButton.setOnClickListener {
            if (positionCurrent != null) {
                val intent = Intent(context, EditPhotoActivity::class.java)
                intent.putExtra("uri", photos[positionCurrent!!].requireUri())
                startActivity(intent)
            }
        }

        debug = preferences.getBoolean(getString(R.string.image_debugger_key), false)

        activity?.window?.navigationBarColor = Color.BLACK
        return binding.root
    }

    private inner class ScreenSlidePagerAdapter(fragment: Fragment) :
            FragmentStateAdapter(fragment) {

        val fullscreen = preferences.getBoolean(getString(R.string.full_screen_view_image_key), false)

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
        (activity as AbstractPhotosActivity).supportActionBar?.show()
        activity?.window?.navigationBarColor = binding.navBarColor
    }
}

