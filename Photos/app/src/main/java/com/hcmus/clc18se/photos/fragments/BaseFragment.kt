package com.hcmus.clc18se.photos.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import timber.log.Timber

abstract class BaseFragment: Fragment() {

    // get the toolbar object from the layout which needs to be setup navigation
    abstract fun getToolbarView(): Toolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpNavigation()
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