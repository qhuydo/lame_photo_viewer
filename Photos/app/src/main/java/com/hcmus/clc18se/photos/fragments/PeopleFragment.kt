package com.hcmus.clc18se.photos.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.scale
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.FragmentPeopleBinding
import com.hcmus.clc18se.photos.utils.getBitMap
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class PeopleFragment : BaseFragment() {
    private var numberMediaItem = 0
    private lateinit var binding: FragmentPeopleBinding

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
    }

    val actionCallbacks = object : MediaItemListAdapter.ActionCallbacks {
        override fun onClick(mediaItem: MediaItem) {
            viewModel.startNavigatingToImageView(mediaItem)
        }

        override fun onSelectionChange() {
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_people, container, false
        )

        binding.photoList.photoListRecyclerView.adapter = MediaItemListAdapter(actionCallbacks).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.lifecycleOwner = this

        viewModel.mediaItemList.observe(viewLifecycleOwner) {
            if (it != null) {
                numberMediaItem = it.size
                CoroutineScope(Dispatchers.Default).launch {
                    val list = listFaceImage(it)
                    Timber.d("${list.size}")
                    withContext(Dispatchers.Main) {
                        binding.photos = list
                        binding.progressPeople.visibility = View.GONE
                        binding.photoList.root.visibility = View.VISIBLE
                    }
                }
            }
        }

        return binding.root
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar.searchActionBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar.appBarLayout

    override fun getToolbarTitleRes(): Int = R.string.people_title

    private suspend fun listFaceImage(list: List<MediaItem>): List<MediaItem> {
        val context = requireActivity().applicationContext
        val detector: FaceDetector = FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build()
        val newList = ArrayList<MediaItem>()
        var number = 0
        for (item in list) {
            Timber.d("${item.mimeType} ${item.name}")
            try {
                if (item.isVideo()) continue

                var bitmap: Bitmap = context.contentResolver.getBitMap(item.requireUri())
                var scale = 1
                val byteBitmap = bitmap.width * bitmap.height * 4
                while (byteBitmap / scale / scale > 6000000) {
                    scale++
                }

                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true).scale(
                        bitmap.width / scale,
                        bitmap.height / scale,
                        false
                )
                val frame = Frame.Builder().setBitmap(bitmap).build()
                val faces = detector.detect(frame)
                if (faces.size() > 0) {
                    newList.add(item)
                    Timber.d("Face")
                }
            } catch (e: Exception) { }
            number++
            withContext(Dispatchers.Main) {
                binding.progressPeople.text = "$number/$numberMediaItem"
            }
        }
        return newList
    }
}