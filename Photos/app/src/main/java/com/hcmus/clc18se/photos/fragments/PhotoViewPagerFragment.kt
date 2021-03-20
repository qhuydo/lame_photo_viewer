package com.hcmus.clc18se.photos.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding

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
                    (requireActivity() as AbstractPhotosActivity).makeToolbarInvisible(true)
                    it.hide()
                } else {
                    parentFragment.setBottomToolbarVisibility(true)
                    (requireActivity() as AbstractPhotosActivity).makeToolbarInvisible(false)
                    it.show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.view_photo_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_kick_to_other_app -> {
                openWith()
                true
            }
            R.id.action_set_as -> {
                setAs()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun setAs() {
        val intent = Intent(Intent.ACTION_ATTACH_DATA)
                .setDataAndType(uri, requireContext().contentResolver.getType(uri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.set_as)))
        } catch (anfe: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No App found", Toast.LENGTH_SHORT).show()
            anfe.printStackTrace()
        }
    }

    private fun openWith() {
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, requireContext().contentResolver.getType(uri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.set_as)))
        } catch (anfe: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No App found", Toast.LENGTH_SHORT).show()
            anfe.printStackTrace()
        }
    }

    companion object {
        private const val BUNDLE_URI = "uri"
    }
}