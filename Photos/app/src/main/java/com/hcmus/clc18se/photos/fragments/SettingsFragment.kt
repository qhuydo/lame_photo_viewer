package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.hcmus.clc18se.photos.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}