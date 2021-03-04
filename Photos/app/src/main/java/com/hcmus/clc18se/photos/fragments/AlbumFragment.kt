package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.hcmus.clc18se.photos.PhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.FragmentAlbumBinding

class AlbumFragment: Fragment() {
    private lateinit var fragmentActivity: FragmentActivity
    private lateinit var binding: FragmentAlbumBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let { fragmentActivity = it }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_album, container, false
        )
        setUpToolBar()
        return binding.root
    }


    private fun setUpToolBar() {
        val photosActivity = activity as PhotosActivity
        val navigationView: NavigationView = photosActivity.findViewById(R.id.navView)

        val toolbar = binding.topAppBar.searchActionBar
        photosActivity.setSupportActionBar(toolbar)

        val navController = NavHostFragment.findNavController(this)
        val appBarConfiguration = photosActivity.appBarConfiguration

        NavigationUI.setupActionBarWithNavController(photosActivity, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navController)

    }
}