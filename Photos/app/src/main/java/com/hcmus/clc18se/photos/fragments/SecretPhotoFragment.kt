package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.FragmentSecretPhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.collections.ArrayList

class SecretPhotoFragment : BaseFragment() {
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
        for (file in directory.listFiles()) {
            if (file != null) {
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

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.secret_photos_title
}