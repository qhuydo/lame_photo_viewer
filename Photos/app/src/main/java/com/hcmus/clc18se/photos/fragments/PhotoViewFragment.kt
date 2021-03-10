package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import com.hcmus.clc18se.photos.data.SamplePhoto
import com.hcmus.clc18se.photos.databinding.FragmentPhotoViewBinding
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotoViewFragment : Fragment() {

    private val viewModel: PhotosViewModel by activityViewModels()
    private val photos by lazy { viewModel.photoList.value ?: listOf<SamplePhoto>() }

    private lateinit var binding: FragmentPhotoViewBinding

    protected var page: Int = 0
        private set

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotoViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.photosViewModel = viewModel
        return binding.root

    }

    override fun onResume() {
        super.onResume()
        binding.horizontalViewPager.adapter = ScreenSlidePagerAdapter(childFragmentManager)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.containsKey(BUNDLE_PAGE) == true) {
            page = savedInstanceState.getInt(BUNDLE_PAGE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_PAGE, page)
    }

    companion object {
        private const val BUNDLE_PAGE = "page"
    }

    private inner class ScreenSlidePagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            val fragment = PhotoViewPagerFragment()
            fragment.resId = photos.get(position).resId
            return fragment
        }

        override fun getCount(): Int {
            return photos.size
        }
    }

    private operator fun next() {
        page++
    }

    private fun previous() {
        page--
    }
}