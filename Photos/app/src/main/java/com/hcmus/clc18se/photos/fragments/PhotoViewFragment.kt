package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotoViewFragment : Fragment() {

    private val viewModel: PhotosViewModel by activityViewModels()
    private val photos by lazy { viewModel.mediaItemList.value ?: listOf<MediaItem>() }

    private lateinit var binding: FragmentPhotoViewBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotoViewBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@PhotoViewFragment
            photosViewModel = viewModel
        }

        binding.horizontalViewPager.apply {
            adapter = ScreenSlidePagerAdapter(this@PhotoViewFragment)

            setCurrentItem(viewModel.idx.value!!, false)
            (activity as AbstractPhotosActivity).supportActionBar?.title = photos[viewModel.idx.value!!].name

            binding.horizontalViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    (activity as AbstractPhotosActivity).supportActionBar?.title = photos[position].name
                }
            })
        }

        return binding.root
    }

    private inner class ScreenSlidePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return photos.size
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = PhotoViewPagerFragment()
            fragment.uri = photos[position].uri

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

}