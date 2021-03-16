package com.hcmus.clc18se.photos.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding
import java.lang.Exception

class PhotoViewPagerFragment : Fragment() {

    internal var uri: Uri = Uri.EMPTY

    private lateinit var binding: PhotoViewPagerPageBinding
    private var actionBar: ActionBar? = null

    private val parentFragment by lazy { requireParentFragment() as PhotoViewFragment }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = PhotoViewPagerPageBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionBar = (activity as AbstractPhotosActivity).supportActionBar

        if (savedInstanceState?.containsKey(BUNDLE_URI) == true) {
            uri = savedInstanceState.getParcelable(BUNDLE_URI) ?: Uri.EMPTY
        }

        binding.imageView.setImage(ImageSource.uri(uri))
        binding.imageView.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {

            override fun onImageLoadError(e: Exception?) {
                binding.progressCircular.visibility = View.GONE
            }

            override fun onImageLoaded() {
                binding.progressCircular.visibility = View.GONE
            }

            override fun onPreviewLoadError(e: Exception?) {
                binding.progressCircular.visibility = View.GONE
            }

            override fun onPreviewReleased() {}

            override fun onReady() {}

            override fun onTileLoadError(e: Exception?) {}
        })
        binding.imageView.setOnClickListener {
            actionBar?.let {
                if (it.isShowing) {
                    parentFragment.setBottomToolbarVisibility(false)
                    // (requireActivity() as AbstractPhotosActivity).hideSystemUI()
                    it.hide()
                } else {
                    parentFragment.setBottomToolbarVisibility(true)
                    // (requireActivity() as AbstractPhotosActivity).showSystemUI()
                    it.show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.view_photo_menu, menu)
    }

    override fun onDetach() {
        super.onDetach()
        actionBar?.show()
        // (requireActivity() as AbstractPhotosActivity).showSystemUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val rootView = view
        if (rootView != null) {
            outState.putParcelable(BUNDLE_URI, uri)
        }
    }

    companion object {
        private const val BUNDLE_URI = "uri"
    }
}