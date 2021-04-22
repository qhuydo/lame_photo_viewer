package com.hcmus.clc18se.photos.fragments

import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding
import com.hcmus.clc18se.photos.utils.VideoDialog
import java.io.*
import java.nio.file.StandardCopyOption.*

class PhotoViewPagerFragment : Fragment() {

    internal var mediaItem: MediaItem? = null
    private var dialog: Dialog? = null
    private lateinit var binding: PhotoViewPagerPageBinding
    private var actionBar: ActionBar? = null
    internal var debug: Boolean = false
    internal var fullScreen: Boolean = false

    private val parentFragment by lazy { requireParentFragment() as PhotoViewFragment }

    private val onImageClickListener = View.OnClickListener {
        val window = requireActivity().window

        actionBar?.let {
            if (it.isShowing) {
                parentFragment.setBottomToolbarVisibility(false)
                if (fullScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                }
                (requireActivity() as AbstractPhotosActivity).makeToolbarInvisible(true)
                it.hide()
            } else {
                parentFragment.setBottomToolbarVisibility(true)
                if (fullScreen) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                }
                (requireActivity() as AbstractPhotosActivity).makeToolbarInvisible(false)
                it.show()
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = PhotoViewPagerPageBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionBar = (activity as AbstractPhotosActivity).supportActionBar

        binding.imageView.setOnImageEventListener(object :
                SubsamplingScaleImageView.OnImageEventListener {

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

        if (savedInstanceState?.containsKey(BUNDLE_MEDIAITEM) == true) {
            mediaItem = savedInstanceState.getParcelable(BUNDLE_MEDIAITEM)
            fullScreen = savedInstanceState.getBoolean(BUNDLE_FULLSCREEN)
        }

        MediaItem.bindMediaItemToImageDrawable(
                requireContext(),
                binding.imageView,
                binding.glideImageView,
                mediaItem,
                binding.videoViewImage,
                debug
        )

        binding.videoViewImage.playIcon.setOnClickListener {
            val intent = Intent(context, VideoDialog::class.java)
            intent.putExtra("uri", mediaItem!!.requireUri())
            startActivity(intent)
        }

        binding.apply {
            imageView.setOnClickListener(onImageClickListener)
            glideImageView.setOnClickListener(onImageClickListener)
        }
    }

    override fun onPause() {
        super.onPause()
        if (dialog != null) {
            dialog!!.dismiss()
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
            R.id.action_move_to_secret_album -> {
                secret()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val rootView = view
        if (rootView != null) {
            outState.putParcelable(BUNDLE_MEDIAITEM, mediaItem)
            outState.putBoolean(BUNDLE_FULLSCREEN, fullScreen)
        }
    }

    private fun setAs() {
        val intent = Intent(Intent.ACTION_ATTACH_DATA)
                .setDataAndType(mediaItem?.requireUri(), mediaItem?.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.set_as)))
        } catch (anfe: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No App found", Toast.LENGTH_SHORT).show()
            anfe.printStackTrace()
        }
    }

    private fun secret() {
        val cw = ContextWrapper(requireContext().applicationContext)
        val directory = cw.getDir("images", Context.MODE_PRIVATE)
        val fileDest = File(directory, mediaItem!!.name)
        try {
            val outputStream = FileOutputStream(fileDest)
            val inputStream = requireContext().contentResolver.openInputStream(mediaItem!!.requireUri())
            inputStream!!.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Move file unsuccess", Toast.LENGTH_SHORT).show()
        }

//        // delete old file
//        val resolver = requireContext().contentResolver
//        try {
//            result = resolver.delete(mediaItem!!.requireUri(), null, null)
//        }
//        catch (securityException: SecurityException) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                val recoverableSecurityException =
//                        securityException as? RecoverableSecurityException
//                                ?: throw SecurityException()
//
//                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
//
//                intentSender?.let {
//                    startIntentSenderForResult(intentSender, 0, null, 0, 0, 0, null)
//                }
//            } else {
//                throw SecurityException()
//            }
//        }
    }

    private fun openWith() {
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(mediaItem?.requireUri(), mediaItem?.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.set_as)))
        } catch (anfe: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No App found", Toast.LENGTH_SHORT).show()
            anfe.printStackTrace()
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (fullScreen) {
            val window = requireActivity().window
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        actionBar?.show()
    }

    companion object {
        private const val BUNDLE_MEDIAITEM = "uri"
        private const val BUNDLE_FULLSCREEN = "fullscreen"
    }
}