package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.hcmus.clc18se.photos.PhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding

class PhotoViewPagerFragment : Fragment() {

    internal var resId: Int = R.drawable.ic_launcher_sample

    private lateinit var binding: PhotoViewPagerPageBinding
    private var actionBar: ActionBar? = null

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
        actionBar = (activity as PhotosActivity).supportActionBar

        if (savedInstanceState?.containsKey(BUNDLE_RESID) == true) {
            resId = savedInstanceState.getInt(BUNDLE_RESID)
        }

        binding.imageView.setImage(ImageSource.resource(resId))

        binding.imageView.setOnClickListener {
            actionBar?.let {
                if (it.isShowing) {
                    it.hide()
                } else {
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
        actionBar?.show()
        super.onDetach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val rootView = view
        if (rootView != null) {
            outState.putInt(BUNDLE_RESID, resId)
        }
    }

    companion object {
        private const val BUNDLE_RESID = "resId"
    }
}