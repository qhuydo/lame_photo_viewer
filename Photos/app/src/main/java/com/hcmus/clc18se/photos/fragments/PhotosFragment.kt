package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding

class PhotosFragment : Fragment() {

    private lateinit var binding: FragmentPhotosBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photos, container, false
        )
        return binding.root
    }
}