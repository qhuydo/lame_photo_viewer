package com.hcmus.clc18se.photos.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hcmus.clc18se.photos.fragments.AlbumFragment
import com.hcmus.clc18se.photos.fragments.PeopleFragment
import com.hcmus.clc18se.photos.fragments.PhotosFragment

const val PAGE_PHOTOS = 0
const val PAGE_PEOPLE = 1
const val PAGE_ALBUM = 2


class HomeViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * Mapping of the ViewPager page indexes to their respective Fragments
     */
    internal val tabFragmentsCreators: Map<Int, () -> Fragment> = mapOf(
            PAGE_PHOTOS to { PhotosFragment().apply { hideToolbar = true } },
            PAGE_PEOPLE to { PeopleFragment().apply { hideToolbar = true } },
            PAGE_ALBUM to { AlbumFragment().apply { hideToolbar = true } },
    )

    override fun getItemCount(): Int {
        return tabFragmentsCreators.size
    }

    override fun createFragment(position: Int): Fragment {
        return tabFragmentsCreators[position]?.invoke() ?: throw IndexOutOfBoundsException()
    }

}