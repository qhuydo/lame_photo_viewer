package com.hcmus.clc18se.photos.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.database.PhotosDatabase
import timber.log.Timber

abstract class BaseFragment : Fragment() {

    // a flag indicating whether the toolbar should be set up,
    // or it should be hidden from the screen.
    // This value must be indicate before the fragment calling onViewCreated.
    var hideToolbar = false

    companion object {
        const val BUNDLE_TOOLBAR_VISIBILITY = "BUNDLE_TOOLBAR_VISIBILITY"
    }

    protected val database by lazy {
        PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
    }

    protected val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    // get the toolbar object from the layout which needs to be setup navigation
    abstract fun getToolbarView(): Toolbar

    abstract fun getAppbar(): AppBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            hideToolbar = it.getBoolean(BUNDLE_TOOLBAR_VISIBILITY)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hideToolbar) {
            setUpNavigation(getToolbarTitleRes())
        } else {
            getAppbar().visibility = View.GONE
        }
    }

    open fun setUpNavigation(@StringRes titleRes: Int? = null) {
        val toolbar = getToolbarView()
        val parentActivity = requireActivity() as? AbstractPhotosActivity

        if (parentActivity == null) {
            Timber.e("Parent activity of fragment ${this.tag} is not BuggyNoteActivity")
        }

        parentActivity?.setSupportActionBar(toolbar)

        parentActivity?.let {

            it.setupActionBarWithNavController(
                    findNavController(),
                    parentActivity.appBarConfiguration
            )

            toolbar.setupWithNavController(findNavController(), it.appBarConfiguration)
        }
        titleRes?.let { toolbar.title = getString(titleRes) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_TOOLBAR_VISIBILITY, hideToolbar)
    }

    open fun getToolbarTitleRes(): Int? = null

    fun hideTheToolbar() {
        getAppbar().visibility = View.GONE
    }

    fun showTheToolbar() {
        getAppbar().visibility = View.VISIBLE
    }

}