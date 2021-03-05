package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.PhotoListAdapter
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotosFragment : Fragment() {

    private lateinit var binding: FragmentPhotosBinding

    private val viewModel by lazy {
        ViewModelProvider(this).get(PhotosViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photos, container, false
        )

        binding.lifecycleOwner = this

        binding.photosViewModel = viewModel
        binding.photoListLayout.photosViewModel = viewModel
        binding.photoListLayout.photoListRecyclerView.adapter = PhotoListAdapter()

        return binding.root
    }
}