package com.hcmus.clc18se.photos

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.hcmus.clc18se.photos.databinding.ActivityPhotosBinding
import com.hcmus.clc18se.photos.utils.*
import timber.log.Timber


class PhotosActivity : AbstractPhotosActivity() {

    internal val binding by lazy { ActivityPhotosBinding.inflate(layoutInflater) }

    override val navHostFragment: NavHostFragment by lazy { supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment }

    private val navController by lazy { navHostFragment.navController }

    override val drawerLayout by lazy { binding.drawerLayout }

    private var isFabRotate = false

    override val appBarConfiguration: AppBarConfiguration by lazy {
        AppBarConfiguration(
                setOf(R.id.page_photo, R.id.page_album, R.id.page_people), drawerLayout
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("On Create called")
        Timber.d("----------------")

        registerOnChangedPreferenceListener()

        setContentView(binding.root)

        setUpNavigationBar()
        setBottomAppBarVisibility()
        savedInstanceState?.let {
            bottomAppBarVisibility = it.getBoolean(BUNDLE_BOTTOM_APPBAR_VISIBILITY)
            setAppbarVisibility(bottomAppBarVisibility)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)

            Handler(Looper.getMainLooper()).postDelayed({
                when (item.itemId) {
                    else -> NavigationUI.onNavDestinationSelected(
                            item, navController
                    ) || onOptionsItemSelected(item)
                }
            }, 300)
        }
    }

    override fun addOnDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val newState = destination.id in arrayOf(
                    R.id.page_photo,
                    R.id.page_people,
                    R.id.page_album,
            )

            bottomAppBarVisibility = newState
            setAppbarVisibility(bottomAppBarVisibility)

            closeFabBeforeNavigating()
//
//            when(destination.id) {
//                R.id.photoViewFragment -> {
//                    val layoutParams = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
//                    layoutParams.topMargin = 0
//                }
//            }
        }
    }

    override fun setNavHostFragmentTopMargin(pixelValue: Int) {
        val layoutParams = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.topMargin = pixelValue
    }

    override fun closeFabBeforeNavigating() {
        if (isFabRotate) {
            isFabRotate = ViewAnimation.rotateFab(binding.fab, !isFabRotate)
            ViewAnimation.showOut(binding.fabAddPicture)
            ViewAnimation.showOut(binding.fabAddVideo)
        }

    }

    override fun setUpNavigationBar() {

        val toolbar = binding.topAppBar.searchActionBar
        setSupportActionBar(toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)

        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

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
        addOnDestinationChangedListener()

        binding.topAppBar.appBarLayout.bringToFront()
    }

    override fun setAppbarVisibility(visibility: Boolean) {
        Timber.d("setAppbarVisibility(visibility: $visibility)")

        val layoutParams = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams

        if (visibility) {
            binding.apply {
                topAppBar.appBarLayout.visibility = View.VISIBLE
                topAppBar2.fragmentAppBarLayout.visibility = View.GONE

                setBottomAppBarVisibility()

                fab.visibility = View.VISIBLE

                layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.search_bar_height)

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()

                setSupportActionBar(topAppBar.searchActionBar)
                topAppBar.searchActionBar.setupWithNavController(navController, appBarConfiguration)
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.apply {
                topAppBar2.fragmentAppBarLayout.visibility = View.VISIBLE
                topAppBar.appBarLayout.visibility = View.GONE

                bottomAppBar.visibility = View.GONE

                fab.visibility = View.GONE
                fabAddPicture.visibility = View.GONE
                fabAddVideo.visibility = View.GONE

                setAppBarHeight<CoordinatorLayout.LayoutParams>(
                        binding.topAppBar2.fragmentAppBarLayout,
                        getAppBarSizeAttr(this@PhotosActivity) ?: DEFAULT_APP_BAR_HEIGHT
                )

                layoutParams.topMargin = getAppBarSizeAttr(this@PhotosActivity)
                        ?: DEFAULT_APP_BAR_HEIGHT

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()

                setSupportActionBar(topAppBar2.fragmentToolBar)
                topAppBar2.fragmentToolBar.setupWithNavController(
                        navController,
                        appBarConfiguration
                )
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    private fun setBottomAppBarVisibility() {
        binding.bottomAppBar.visibility =
                if (!displayBottomBarPreference()) View.INVISIBLE else View.VISIBLE
    }

    override fun makeToolbarInvisible(wantToMakeToolbarInvisible: Boolean) {
        val visibility = if (wantToMakeToolbarInvisible) View.INVISIBLE else View.VISIBLE
        //binding.topAppBar.appBarLayout.visibility = visibility
        binding.topAppBar2.fragmentAppBarLayout.visibility = visibility
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(
                item,
                navController
        ) || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun getNavGraphResId() = R.id.navigation
}