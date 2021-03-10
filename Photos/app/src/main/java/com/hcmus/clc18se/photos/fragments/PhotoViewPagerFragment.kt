package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding

class PhotoViewPagerFragment : Fragment() {

    internal var resId: Int = R.drawable.ic_launcher_sample

    private lateinit var binding: PhotoViewPagerPageBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = PhotoViewPagerPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState?.containsKey(BUNDLE_ASSET) == true) {
            resId = savedInstanceState.getInt(BUNDLE_ASSET)
        }
        binding.imageView.setImage(ImageSource.resource(resId))

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val rootView = view
        if (rootView != null) {
            outState.putInt(BUNDLE_ASSET, resId)
        }
    }

    companion object {
        private const val BUNDLE_ASSET = "asset"
    }
}