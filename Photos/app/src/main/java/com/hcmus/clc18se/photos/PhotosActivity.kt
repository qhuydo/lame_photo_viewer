package com.hcmus.clc18se.photos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
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
import com.hcmus.clc18se.photos.utils.ui.ViewAnimation
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

    private fun handleSendImage(intent: Intent) {
        val sendingIntent = Intent(this, ViewPhotoActivity::class.java)
        sendingIntent.putExtra(Intent.EXTRA_STREAM, intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM))
        startActivity(sendingIntent)
    }

    private fun handleViewImage(intent: Intent) {
        val sendingIntent = Intent(this, ViewPhotoActivity::class.java)
        sendingIntent.putExtra("uri", intent.data)
        startActivity(sendingIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                Timber.d("Send")
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent) // Handle single image being sent
                }
            }
            Intent.ACTION_VIEW -> {
                Timber.d("Send")
                if (intent.type?.startsWith("image/") == true) {
                    handleViewImage(intent) // Handle single image being sent
                }
            }
        }

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
            ViewAnimation.showOut(binding.fabAddAlbum)
        }

    }

    override fun setUpNavigationBar() {
        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        val fabs = listOf(binding.fabAddPicture, binding.fabAddVideo, binding.fabAddAlbum)

        fabs.forEach { ViewAnimation.init(it) }

        binding.fab.setOnClickListener {
            isFabRotate = ViewAnimation.rotateFab(it, !isFabRotate)
            if (isFabRotate) {
                fabs.forEach { ViewAnimation.showIn(it) }
            } else {
                fabs.forEach { ViewAnimation.showOut(it) }
            }
        }
        addOnDestinationChangedListener()
    }

    override fun setAppbarVisibility(visibility: Boolean) {
        Timber.d("setAppbarVisibility(visibility: $visibility)")

        // val layoutParams = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams

        if (visibility) {
            binding.apply {
                setBottomAppBarVisibility()
                fab.visibility = View.VISIBLE

                mainCoordinatorLayout.requestLayout()
                mainCoordinatorLayout.invalidate()
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.apply {

                bottomAppBar.visibility = View.GONE

                fab.visibility = View.GONE
                fabAddPicture.visibility = View.GONE
                fabAddVideo.visibility = View.GONE
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
        //binding.topAppBar.appBarLayout.visibility = visibility
        // binding.topAppBar2.fragmentAppBarLayout.visibility = visibility
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