package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import timber.log.Timber

abstract class BaseFragment : Fragment() {

    // a flag indicating whether the toolbar should be set up,
    // or it should be hidden from the screen.
    // This value must be indicate before the fragment calling onViewCreated.
    var hideToolbar = false

    // get the toolbar object from the layout which needs to be setup navigation
    abstract fun getToolbarView(): Toolbar

    abstract fun getAppbar(): AppBarLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hideToolbar) {
            setUpNavigation()
        } else {
            getAppbar().visibility = View.GONE
        }
    }

    protected fun setUpNavigation() {
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
    }

}