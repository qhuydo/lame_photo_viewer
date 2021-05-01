package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.FragmentAboutBinding

class AboutFragment : BaseFragment() {
    private lateinit var fragmentActivity: FragmentActivity
    private lateinit var binding: FragmentAboutBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let { fragmentActivity = it }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_about, container, false
        )

        return binding.root
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.about
}