package com.hcmus.clc18se.photos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.databinding.ActivityViewPhotoBinding
import com.hcmus.clc18se.photos.utils.getMimeType

class ViewPhotoActivity: AppCompatActivity() {

    private val binding: ActivityViewPhotoBinding by lazy {
        ActivityViewPhotoBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra("uri")) {
            val uri:Uri? = intent.getParcelableExtra("uri")
            uri?.let {
                val mediaItem = MediaItem(
                        -1,
                        it.path.toString(),
                        it,
                        null,
                        getMimeType(this, it),
                        null,
                        it.path.toString()
                )
                binding.photo = mediaItem
            }
        }
        else {
            (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                val mediaItem = MediaItem(
                        -1,
                        it.path.toString(),
                        it,
                        null,
                        getMimeType(this, it),
                        null,
                        it.path.toString()
                )

                binding.photo = mediaItem
            }
        }

        setContentView(binding.root)
    }
}