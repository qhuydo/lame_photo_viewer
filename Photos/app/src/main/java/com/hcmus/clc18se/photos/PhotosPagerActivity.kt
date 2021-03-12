package com.hcmus.clc18se.photos

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.hcmus.clc18se.photos.databinding.ActivityPhotosPagerBinding
import com.hcmus.clc18se.photos.utils.*
import timber.log.Timber

class PhotosPagerActivity : AbstractPhotosActivity() {

    internal val binding by lazy { ActivityPhotosPagerBinding.inflate(layoutInflater) }

    private val navHostFragment by lazy { supportFragmentManager.findFragmentById(R.id.navHostFragmentPager) as NavHostFragment }

    private val navController by lazy { navHostFragment.navController }

    private val drawerLayout by lazy { binding.drawerLayout }

    private var isFabRotate = false

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configColor()
        configTheme()

        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.homeViewPagerFragment), drawerLayout
        )
        Timber.d("On Create called")
        Timber.d("----------------")

        registerOnChangedPreferenceListener()

        setContentView(binding.root)

        setUpNavigationBar()
    }

    override fun addOnDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val newState = destination.id in arrayOf(
                    R.id.homeViewPagerFragment
            )
            setAppbarVisibility(newState)

            closeFabBeforeNavigating()
        }
    }

    override fun closeFabBeforeNavigating() {
        if (isFabRotate) {
            isFabRotate = ViewAnimation.rotateFab(binding.fab, !isFabRotate)
            ViewAnimation.showOut(binding.fabAddPicture)
            ViewAnimation.showOut(binding.fabAddVideo)
        }

    }

    override fun setUpNavigationBar() {
        addOnDestinationChangedListener()

        val toolbar2 = binding.topAppBar2.fragmentToolBar
        setSupportActionBar(toolbar2)
        toolbar2.setupWithNavController(navController, appBarConfiguration)

        val toolbar = binding.topAppBar.searchActionBar
        setSupportActionBar(toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)

        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)

        ViewAnimation.init(binding.fabAddPicture)
        ViewAnimation.init(binding.fabAddVideo)

        binding.fab.setOnClickListener {
            isFabRotate = ViewAnimation.rotateFab(it, !isFabRotate)
            if (isFabRotate) {
                ViewAnimation.showIn(binding.fabAddPicture)
                ViewAnimation.showIn(binding.fabAddVideo)
            } else {
                ViewAnimation.showOut(binding.fabAddPicture)
                ViewAnimation.showOut(binding.fabAddVideo)
            }
        }
        binding.topAppBar.appBarLayout.bringToFront()
    }

    override fun setAppbarVisibility(visibility: Boolean) {
        Timber.d("setAppbarVisibility(visibility: $visibility)")

        if (visibility) {
            binding.apply {
                topAppBar.appBarLayout.visibility = View.VISIBLE
                topAppBar2.fragmentAppBarLayout.visibility = View.INVISIBLE

                fab.visibility = View.VISIBLE

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()

                setSupportActionBar(topAppBar.searchActionBar)
                topAppBar.searchActionBar.setupWithNavController(navController, appBarConfiguration)
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.apply {
                topAppBar.appBarLayout.visibility = View.GONE
                topAppBar2.fragmentAppBarLayout.visibility = View.VISIBLE

                fab.visibility = View.GONE
                fabAddPicture.visibility = View.GONE
                fabAddVideo.visibility = View.GONE

                setAppBarHeight<CoordinatorLayout.LayoutParams>(
                        binding.topAppBar2.fragmentAppBarLayout,
                        getAppBarSizeAttr(this@PhotosPagerActivity) ?: DEFAULT_APP_BAR_HEIGHT)

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()

                setSupportActionBar(topAppBar2.fragmentToolBar)
                topAppBar2.fragmentToolBar.setupWithNavController(navController, appBarConfiguration)
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(
                item,
                navController
        ) || super.onOptionsItemSelected(
                item
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        configTheme(newConfig.uiMode)
        recreate()
    }

}