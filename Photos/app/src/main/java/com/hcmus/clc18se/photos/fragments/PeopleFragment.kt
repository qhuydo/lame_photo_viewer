package com.hcmus.clc18se.photos.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.PhotosApplication.Companion.list
import com.hcmus.clc18se.photos.PhotosApplication.Companion.newList
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPeopleBinding
import com.hcmus.clc18se.photos.service.DetectFace
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory

class PeopleFragment : BaseFragment() {
//    private var numberMediaItem = 0
    private lateinit var binding: FragmentPeopleBinding
    private lateinit var mBroadcast:BroadcastReceiver

    private val viewModel: PhotosViewModel by activityViewModels {
        PhotosViewModelFactory(
                requireActivity().application,
                PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
        mBroadcast = MyMainLocalReceiver()
        val filter = IntentFilter("test.Broadcast")
        requireContext().registerReceiver(mBroadcast, filter)
    }

    private val actionCallbacks = object : MediaItemListAdapter.ActionCallbacks {
        override fun onClick(mediaItem: MediaItem) {
            viewModel.startNavigatingToImageView(mediaItem)
        }

        override fun onSelectionChange() {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_people, container, false
        )

        binding.photoList.photoListRecyclerView.adapter = MediaItemListAdapter(actionCallbacks).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.lifecycleOwner = this

        if (list != null) {
            binding.emptyLayout.visibility = View.GONE
        }

        binding.buttonStartService.setOnClickListener {
            showConfirmDialog()
        }

        return binding.root
    }

    private fun startDetectService() {
        binding.emptyLayout.visibility = View.GONE
        viewModel.mediaItemList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (list == null) {
                    list = it
                    val intent = Intent(context, DetectFace::class.java)
                    if (requireContext().applicationContext.startService(intent) != null) {
                        Toast.makeText(context, "Service detect face is running", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Service detect face unable to start", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            binding.photos = newList
        }
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar.searchActionBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar.appBarLayout

    override fun getToolbarTitleRes(): Int = R.string.people_title

    private fun showConfirmDialog() {
        MaterialDialog(requireContext()).show {
            title(R.string.start_detect_face_dialog_title)
            message(R.string.start_detect_face_dialog_msg)
            positiveButton(R.string.yes) { startDetectService() }
            negativeButton(R.string.no) {  }
        }

    }

    inner class MyMainLocalReceiver : BroadcastReceiver() {
        override fun onReceive(localContext: Context?, callerIntent: Intent) {
            Log.d("receive", "update face")
            binding.photos = newList
        }
    }
}