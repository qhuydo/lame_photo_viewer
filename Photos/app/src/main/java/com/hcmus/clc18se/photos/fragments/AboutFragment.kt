package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
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
}