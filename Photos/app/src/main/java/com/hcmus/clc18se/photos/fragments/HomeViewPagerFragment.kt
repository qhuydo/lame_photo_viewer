package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialcab.attached.destroy
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.PhotosPagerActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.HomeViewPagerAdapter
import com.hcmus.clc18se.photos.adapters.PAGE_ALBUM
import com.hcmus.clc18se.photos.adapters.PAGE_PEOPLE
import com.hcmus.clc18se.photos.adapters.PAGE_PHOTOS
import com.hcmus.clc18se.photos.databinding.FragmentHomePagerBinding
import timber.log.Timber

class HomeViewPagerFragment : BaseFragment() {

    private lateinit var binding: FragmentHomePagerBinding
    private val parentActivity by lazy { requireActivity() as PhotosPagerActivity }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomePagerBinding.inflate(inflater, container, false)
        setUpTabsLayout()
        return binding.root
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            if (position != PAGE_PHOTOS) {
                Timber.d("OnPageSelected(): position $position")
                val photosFragment = childFragmentManager.findFragmentByTag("f$PAGE_PHOTOS") as? PhotosFragment

                photosFragment?.mainCab?.let {
                    it.destroy()
                    // photosFragment.mainCab = null
                    Timber.d("Multi-select menu dismissed")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private fun setUpTabsLayout() {
        val viewPager = binding.viewPager
        viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)

        val tabLayout = binding.topAppBar.tabs
        val adapter = HomeViewPagerAdapter(this)

        viewPager.adapter = adapter

        // set text for tab elements
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setIcon(getTabIcon(position))
            tab.text = getTabText(position)
        }.attach()

        // Register on page change listener to dismiss the multi-select menu in photo fragment when
        // changing to other tabs
        viewPager.registerOnPageChangeCallback(onPageChangeCallback)

        parentActivity.setSupportActionBar(binding.topAppBar.searchActionBar)

    }

    private fun getTabIcon(position: Int): Int {
        return when (position) {
            PAGE_PHOTOS -> R.drawable.selector_photo
            PAGE_PEOPLE -> R.drawable.selector_face
            PAGE_ALBUM -> R.drawable.selector_photo_album
            else -> throw IndexOutOfBoundsException()
        }
    }

    private fun getTabText(position: Int): String {
        return getString(when (position) {
            PAGE_PHOTOS -> R.string.photo_title
            PAGE_PEOPLE -> R.string.people_title
            PAGE_ALBUM -> R.string.album_title
            else -> throw IndexOutOfBoundsException()
        })
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar.searchActionBar

    override fun getAppbar(): AppBarLayout  = binding.topAppBar.appBarLayout
}