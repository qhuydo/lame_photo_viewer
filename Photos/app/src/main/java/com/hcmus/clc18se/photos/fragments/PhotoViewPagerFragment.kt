package com.hcmus.clc18se.photos.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.textfield.TextInputEditText
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.PhotoViewPagerPageBinding
import com.hcmus.clc18se.photos.utils.VideoDialogActivity
import java.io.*


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

                parentFragment.hideTheToolbar()
                it.hide()
            } else {
                parentFragment.setBottomToolbarVisibility(true)
                if (fullScreen) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
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

        if (savedInstanceState?.containsKey(BUNDLE_MEDIAITEM) == true) {
            mediaItem = savedInstanceState.getParcelable(BUNDLE_MEDIAITEM)
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

    @SuppressLint("CutPasteId")
    private fun secret() {
        val cw = ContextWrapper(requireContext().applicationContext)
        val directory = cw.getDir("images", Context.MODE_PRIVATE)
        var fileDest:File = File(directory, mediaItem!!.name)
        var newName:String? = null
        if (fileDest.exists()) {
            val dialog = Dialog(requireContext()).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_duplicate_file)
            }

            dialog.findViewById<Button>(R.id.ok_override)?.setOnClickListener {
                newName?.let {
                    fileDest = File(directory, newName)
                    if (fileDest.exists()) {
                        Toast.makeText(requireContext(), "Your file is duplicate", Toast.LENGTH_SHORT).show()
                    } else {
                        moveFile(fileDest)
                        dialog.dismiss()
                    }
                }?: run{
                    moveFile(fileDest)
                    dialog.dismiss()
                }
            }

            dialog.findViewById<Button>(R.id.rename_button)?.setOnClickListener {
                dialog.findViewById<TextInputEditText>(R.id.rename_field).visibility = View.VISIBLE
                newName = dialog.findViewById<TextInputEditText>(R.id.rename_field).text.toString() + mediaItem!!.name.substring(mediaItem!!.name.lastIndexOf("."))
                dialog.findViewById<TextInputEditText>(R.id.rename_field).addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int,
                                                   count: Int, after: Int) {

                    }

                    override fun onTextChanged(s: CharSequence, start: Int,
                                               before: Int, count: Int) {
                        newName = s.toString() + mediaItem!!.name.substring(mediaItem!!.name.lastIndexOf("."))
                    }
                })
            }

            dialog.findViewById<Button>(R.id.cancel_override)?.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
        else{
            moveFile(fileDest)
        }
    }

    @SuppressLint("CutPasteId")
    private fun rename(){
        var newName:String? = mediaItem!!.name
        val path = mediaItem!!.requirePath(requireContext())
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_rename)
        }

        dialog.findViewById<Button>(R.id.ok_override)?.setOnClickListener {
            newName?.let {
                val fileDest = File(path!!.substring(0, path.lastIndexOf("/") + 1) + newName)
                if (fileDest.exists()) {
                    Toast.makeText(requireContext(), "Your file is duplicate", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val contentResolver = requireContext().contentResolver
                        val contentValues = ContentValues()
                        contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName);
                        contentResolver.update(mediaItem!!.requireUri(), contentValues, null, null);
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Rename successful", Toast.LENGTH_SHORT).show()
                        (requireActivity() as AppCompatActivity).supportActionBar?.title = newName!!
                    }
                    catch (e: Exception){
                        Toast.makeText(requireContext(), "Rename unsuccessful", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.findViewById<TextInputEditText>(R.id.rename_field).visibility = View.VISIBLE
        newName = dialog.findViewById<TextInputEditText>(R.id.rename_field).text.toString() + mediaItem!!.name.substring(mediaItem!!.name.lastIndexOf("."))
        dialog.findViewById<TextInputEditText>(R.id.rename_field).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                newName = s.toString() + mediaItem!!.name.substring(mediaItem!!.name.lastIndexOf("."))
            }
        })
        dialog.show()
    }

    private fun moveFile(fileDest: File){
        try {
            val outputStream = FileOutputStream(fileDest)
            val inputStream = requireContext().contentResolver.openInputStream(mediaItem!!.requireUri())
            inputStream!!.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Toast.makeText(requireContext(), "Move successful", Toast.LENGTH_SHORT).show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == COPY_FILE) {
            data?.let {
                val cw = ContextWrapper(requireContext().applicationContext)
                val treeUri = data.data
                val fileDest = DocumentFile.fromTreeUri(requireContext(), treeUri!!)?.createFile(mediaItem!!.mimeType!!, mediaItem!!.name)
                try {
                    val outputStream = requireContext().contentResolver.openOutputStream(fileDest!!.uri)
                    val inputStream = requireContext().contentResolver.openInputStream(mediaItem!!.requireUri())
                    inputStream!!.copyTo(outputStream!!)
                    inputStream.close()
                    outputStream.close()
                    Toast.makeText(context, "Copy file success", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Copy file unsuccessful", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val BUNDLE_MEDIAITEM = "uri"
        private const val BUNDLE_FULLSCREEN = "fullscreen"
        private const val COPY_FILE = 2345
    }
}