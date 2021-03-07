package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.ITEM_TYPE_LIST
import com.hcmus.clc18se.photos.adapters.ITEM_TYPE_THUMBNAIL
import com.hcmus.clc18se.photos.adapters.PhotoListAdapter
import com.hcmus.clc18se.photos.adapters.bindSamplePhotoListRecyclerView
import com.hcmus.clc18se.photos.data.SamplePhoto
import com.hcmus.clc18se.photos.databinding.FragmentPhotosBinding
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel

class PhotosFragment : Fragment() {

    private lateinit var binding: FragmentPhotosBinding
    var nextAdapter = ITEM_TYPE_THUMBNAIL

    private val viewModel by lazy {
        ViewModelProvider(this).get(PhotosViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_photos, container, false
        )

        binding.apply {
            lifecycleOwner = this@PhotosFragment

            photosViewModel = viewModel
            photoListLayout.photosViewModel = viewModel
            photoListLayout.photoListRecyclerView.adapter = PhotoListAdapter()

            photoListLayout.upperBar.listViewBtn.setOnClickListener {

                val adapter = PhotoListAdapter(nextAdapter)
                val recyclerView = photoListLayout.photoListRecyclerView
                val viewModel = photoListLayout.photosViewModel as PhotosViewModel
                recyclerView.adapter = adapter
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                layoutManager.spanCount = if (nextAdapter == ITEM_TYPE_LIST) 1 else 2

                bindSamplePhotoListRecyclerView(recyclerView, viewModel.photoList.value
                        ?: listOf<SamplePhoto>())

                adapter.notifyDataSetChanged()
                nextAdapter = if (nextAdapter == ITEM_TYPE_LIST) ITEM_TYPE_THUMBNAIL else ITEM_TYPE_LIST
            }
        }

        return binding.root
    }
}