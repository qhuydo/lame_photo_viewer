package com.hcmus.clc18se.photos.fragments

import android.app.Activity
import android.app.Dialog
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding
import com.hcmus.clc18se.photos.utils.VideoDialogActivity
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import java.io.*

class PhotoViewPagerFragment : Fragment() {
    private lateinit var viewModel: PhotosViewModel
    internal var mediaItem: MediaItem? = null
    private var dialog: Dialog? = null
    private lateinit var binding: PhotoViewPagerPageBinding
    private var actionBar: ActionBar? = null
    internal var debug: Boolean = false
    internal var fullScreen: Boolean = false

    private val parentFragment by lazy { requireParentFragment() as PhotoViewFragment }

    private val onImageClickListener = View.OnClickListener {
        // val window = requireActivity().window

        actionBar?.let {
            if (it.isShowing) {
                parentFragment.setBottomToolbarVisibility(false)
                if (fullScreen) {
                    // window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    (requireActivity() as AbstractPhotosActivity).toggleImmersiveMode()
                }
                parentFragment.hideTheToolbar()
                it.hide()
            } else {
                parentFragment.setBottomToolbarVisibility(true)
                if (fullScreen) {
                    // window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    (requireActivity() as AbstractPhotosActivity).toggleImmersiveMode()
                }
                parentFragment.showTheToolbar()
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
        actionBar = (activity as AppCompatActivity).supportActionBar

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

        if (savedInstanceState?.containsKey(BUNDLE_MEDIA_ITEM) == true) {
            mediaItem = savedInstanceState.getParcelable(BUNDLE_MEDIA_ITEM)
            fullScreen = savedInstanceState.getBoolean(BUNDLE_FULLSCREEN)
        }

        binding.playIcon.setOnClickListener {
            val intent = Intent(context, VideoDialogActivity::class.java)
            intent.putExtra("uri", mediaItem!!.requireUri())
            startActivity(intent)
        }

        binding.apply {
            photo = mediaItem
            debug = this@PhotoViewPagerFragment.debug
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
            R.id.action_rename -> {
                rename()
                true
            }
            R.id.action_move -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(Intent.createChooser(intent, "Choose directory"), MOVE_FILE)
                true
            }
            R.id.action_copy -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(Intent.createChooser(intent, "Choose directory"), COPY_FILE)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val rootView = view
        if (rootView != null) {
            outState.putParcelable(BUNDLE_MEDIA_ITEM, mediaItem)
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
        var fileDest = File(directory, mediaItem!!.name)

        if (fileDest.exists()) {
            MaterialDialog(requireContext()).show {
                title(R.string.file_duplicate_warning_dialog_title)
                message(R.string.file_duplicate_warning_dialog_msg)
                negativeButton(R.string.cancel) { }
                @Suppress("DEPRECATION")
                neutralButton(R.string.rename) {

                    MaterialDialog(requireContext()).show {
                        input()
                        title(R.string.set_file_name_dialog_title)
                        positiveButton {
                            val newName = "${getInputField().text}${
                                mediaItem!!.name.substring(
                                        mediaItem!!.name.lastIndexOf(".")
                                )
                            }"
                            fileDest = File(directory, newName)
                            if (fileDest.exists()) {
                                Toast.makeText(
                                        requireContext(),
                                        R.string.failed_filename_exist_again,
                                        Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                copyToInternal(fileDest)
                            }
                        }
                    }

                }
                positiveButton(R.string.override) {
                    copyToInternal(fileDest)
                }
            }
        } else {
            copyToInternal(fileDest)
        }
    }

    private fun rename() {
        val path = mediaItem!!.requirePath(requireContext())

        MaterialDialog(requireContext()).show {
            input()
            title(R.string.set_file_name_dialog_title)
            positiveButton {
                val newName = "${getInputField().text}${
                    mediaItem!!.name.substring(
                            mediaItem!!.name.lastIndexOf(".")
                    )
                }"
                val fileDest = File("${path!!.substring(0, path.lastIndexOf("/") + 1)}$newName")

                if (fileDest.exists()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.file_duplicate_warning_dialog_title),
                            Toast.LENGTH_SHORT
                    ).show()
                } else {
                    try {
                        val contentResolver = requireContext().contentResolver
                        val contentValues = ContentValues()
                        contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName);
                        contentResolver.update(mediaItem!!.requireUri(), contentValues, null, null);

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.rename_succeed),
                                Toast.LENGTH_SHORT
                        ).show()
                        (requireActivity() as AppCompatActivity).supportActionBar?.title = newName

                    } catch (e: Exception) {
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.rename_unsucceed),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun copyToInternal(fileDest: File) {
        var outputStream: FileOutputStream? = null
        var inputStream: InputStream? = null

        try {
            outputStream = FileOutputStream(fileDest)
            inputStream = requireContext().contentResolver.openInputStream(mediaItem!!.requireUri())
            inputStream?.copyTo(outputStream)
            Toast.makeText(requireContext(), getString(R.string.move_succeed), Toast.LENGTH_SHORT)
                    .show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, getString(R.string.move_unsucceed), Toast.LENGTH_SHORT).show()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun openWith() {
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(mediaItem?.requireUri(), mediaItem?.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.set_as)))
        } catch (anfe: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.no_app_found), Toast.LENGTH_SHORT)
                    .show()
            anfe.printStackTrace()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModel: PhotosViewModel by navGraphViewModels(
                (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                    requireActivity().application,
                    PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        this.viewModel = viewModel
    }
    override fun onDetach() {
        super.onDetach()
        if (fullScreen) {
            val window = requireActivity().window
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        actionBar?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK ) {
            when (requestCode){
                COPY_FILE ->{
                    data?.let {
                        val cw = ContextWrapper(requireContext().applicationContext)
                        val treeUri = data.data
                        var fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                ?.findFile(mediaItem!!.name)
                        if (fileDest != null)
                        {
                            MaterialDialog(requireContext()).show {
                                title(R.string.file_duplicate_warning_dialog_title)
                                message(R.string.file_duplicate_warning_dialog_msg)
                                negativeButton(R.string.cancel) { }
                                @Suppress("DEPRECATION")
                                neutralButton(R.string.rename) {

                                    MaterialDialog(requireContext()).show {
                                        input()
                                        title(R.string.set_file_name_dialog_title)
                                        positiveButton {
                                            val newName = "${getInputField().text}${
                                                mediaItem!!.name.substring(
                                                        mediaItem!!.name.lastIndexOf(".")
                                                )
                                            }"
                                            fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                                    ?.findFile(newName)
                                            if (fileDest != null) {
                                                Toast.makeText(
                                                        requireContext(),
                                                        R.string.failed_filename_exist_again,
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            else{
                                                dismiss()
                                                fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                                        ?.createFile(mediaItem!!.mimeType!!,newName)
                                                copyFile(fileDest)
                                            }
                                        }
                                    }

                                }
                                positiveButton(R.string.override) {
                                    dismiss()
                                    copyFile(fileDest)
                                }
                            }
                        }
                        else{
                            fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                    ?.createFile(mediaItem!!.mimeType!!, mediaItem!!.name)
                            copyFile(fileDest)
                        }
                    }
                }
                MOVE_FILE ->{
                    data?.let {
                        val cw = ContextWrapper(requireContext().applicationContext)
                        val treeUri = data.data
                        var fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                ?.findFile(mediaItem!!.name)
                        if (fileDest != null)
                        {
                            MaterialDialog(requireContext()).show {
                                title(R.string.file_duplicate_warning_dialog_title)
                                message(R.string.file_duplicate_warning_dialog_msg)
                                negativeButton(R.string.cancel) { }
                                @Suppress("DEPRECATION")
                                neutralButton(R.string.rename) {

                                    MaterialDialog(requireContext()).show {
                                        input()
                                        title(R.string.set_file_name_dialog_title)
                                        positiveButton {
                                            val newName = "${getInputField().text}${
                                                mediaItem!!.name.substring(
                                                        mediaItem!!.name.lastIndexOf(".")
                                                )
                                            }"
                                            fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                                    ?.findFile(newName)
                                            if (fileDest != null) {
                                                Toast.makeText(
                                                        requireContext(),
                                                        R.string.failed_filename_exist_again,
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            else{
                                                dismiss()
                                                fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                                        ?.createFile(mediaItem!!.mimeType!!,newName)
                                                moveFile(fileDest)
                                            }
                                        }
                                    }

                                }
                                positiveButton(R.string.override) {
                                    dismiss()
                                    moveFile(fileDest)
                                }
                            }
                        }
                        else{
                            fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)
                                    ?.createFile(mediaItem!!.mimeType!!, mediaItem!!.name)
                            moveFile(fileDest)
                        }
                    }
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun moveFile(fileDest: DocumentFile?){
        try {
            val outputStream = requireContext().contentResolver.openOutputStream(fileDest!!.uri)
            val inputStream = requireContext().contentResolver.openInputStream(mediaItem!!.requireUri())
            inputStream!!.copyTo(outputStream!!)
            inputStream.close()
            outputStream.close()
            Toast.makeText(context, getString(R.string.move_succeed), Toast.LENGTH_SHORT)
                    .show()
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                MaterialDialog(requireContext()).show {
                    title(R.string.delete_warning_dialog_title)
                    message(R.string.delete_warning_dialog_msg)
                    positiveButton(R.string.yes) {
                        viewModel.deleteImage(mediaItem!!)
                    }
                    negativeButton(R.string.no) {}
                }
            } else {
                viewModel.deleteImage(mediaItem!!)
            }
        } catch (e: Exception) {
            Toast.makeText(
                    context,
                    getString(R.string.move_unsucceed),
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun copyFile(fileDest: DocumentFile?){
        try {
            val outputStream = requireContext().contentResolver.openOutputStream(fileDest!!.uri)
            val inputStream = requireContext().contentResolver.openInputStream(mediaItem!!.requireUri())
            inputStream!!.copyTo(outputStream!!)
            inputStream.close()
            outputStream.close()
            Toast.makeText(context, getString(R.string.copy_succeed), Toast.LENGTH_SHORT)
                    .show()
        } catch (e: Exception) {
            Toast.makeText(
                    context,
                    getString(R.string.copy_file_unsucceed),
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val BUNDLE_MEDIA_ITEM = "uri"
        private const val BUNDLE_FULLSCREEN = "fullscreen"
        private const val COPY_FILE = 2345
        private const val MOVE_FILE = 3456
    }
}