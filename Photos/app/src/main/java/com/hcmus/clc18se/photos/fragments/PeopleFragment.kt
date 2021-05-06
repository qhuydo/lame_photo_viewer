package com.hcmus.clc18se.photos.fragments

import android.graphics.Bitmap
import android.media.FaceDetector
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.scale
import androidx.databinding.DataBindingUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.FragmentPeopleBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PeopleFragment : BaseFragment() {

    private lateinit var binding: FragmentPeopleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_people, container, false
        )

        return binding.root
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar.searchActionBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar.appBarLayout

    override fun getToolbarTitleRes(): Int = R.string.people_title

    private fun listFaceImage(list:List<MediaItem>):List<MediaItem>{
        val newList = ArrayList<MediaItem>()
        for (item in list){
            var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, item.requireUri())
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
            val faceDetector:FaceDetector = FaceDetector(bitmap.width,bitmap.height,1)
            val faces = Array<FaceDetector.Face?>(1){null}
            val numFace = faceDetector.findFaces(bitmap, faces)
            if (numFace > 0)
                newList.add(item)
        }
        return newList
    }
}