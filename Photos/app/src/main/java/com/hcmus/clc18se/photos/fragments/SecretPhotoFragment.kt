package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.data.MediaProvider
import com.hcmus.clc18se.photos.databinding.FragmentPeopleBinding
import com.hcmus.clc18se.photos.databinding.FragmentSecretPhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SecretPhotoFragment : Fragment() {
    private lateinit var binding: FragmentSecretPhotoBinding
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_secret_photo, container, false
        )

        val cw = ContextWrapper(requireContext().applicationContext)
        val directory = cw.getDir("images", Context.MODE_PRIVATE)
        val list = ArrayList<Uri>()
        for (file in directory.listFiles())
        {
            if (file != null)
            {
                list.add(Uri.fromFile(file))
            }
        }

        binding.listView.adapter = ArrayAdapter<Any?>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                list as List<Any?>
        )

        return binding.root
    }
}