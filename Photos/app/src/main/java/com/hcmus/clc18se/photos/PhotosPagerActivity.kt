package com.hcmus.clc18se.photos

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
import com.hcmus.clc18se.photos.databinding.ActivityPhotosPagerBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.utils.ui.ViewAnimation
import timber.log.Timber

class PhotosPagerActivity : AbstractPhotosActivity() {

    internal val binding by lazy { ActivityPhotosPagerBinding.inflate(layoutInflater) }

    override val navHostFragment by lazy { supportFragmentManager.findFragmentById(R.id.navHostFragmentPager) as NavHostFragment }

    private val navController by lazy { navHostFragment.navController }

    override val drawerLayout by lazy { binding.drawerLayout }

    private var isFabRotate = false

    override val appBarConfiguration by lazy {
        AppBarConfiguration(
                setOf(R.id.homeViewPagerFragment), drawerLayout
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Timber.d("On Create called")
        Timber.d("----------------")

        registerOnChangedPreferenceListener()

        setContentView(binding.root)

        setUpNavigationBar()

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
                    R.id.homeViewPagerFragment
            )
            setAppbarVisibility(newState)

            closeFabBeforeNavigating()

            when (destination.id) {
                R.id.photoViewFragment -> {
                    val layoutParams =
                            binding.navHostFragmentPager.layoutParams as CoordinatorLayout.LayoutParams
                    layoutParams.topMargin = 0
                }
            }
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
        addOnDestinationChangedListener()

    }

    override fun setAppbarVisibility(visibility: Boolean) {
        if (visibility) {
            binding.apply {

                fab.visibility = View.VISIBLE
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.apply {
                fab.visibility = View.GONE
                fabAddPicture.visibility = View.GONE
                fabAddVideo.visibility = View.GONE
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_BOTTOM_APPBAR_VISIBILITY, bottomAppBarVisibility)
    }

    override fun makeToolbarInvisible(wantToMakeToolbarInvisible: Boolean) {
        val visibility = if (wantToMakeToolbarInvisible) View.INVISIBLE else View.VISIBLE
        // TODO
    }

    override fun setNavHostFragmentTopMargin(pixelValue: Int) {
        val layoutParams =
                binding.navHostFragmentPager.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.topMargin = pixelValue
    }

    override fun getNavGraphResId() = R.id.pager_navigation

}