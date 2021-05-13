package com.hcmus.clc18se.photos.utils

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.clc18se.photos.R

class VideoDialogActivity : AppCompatActivity() {
    private lateinit var uri: Uri

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = intent.getParcelableExtra("uri")!!

        setContentView(R.layout.video_view_dialog)

        val mediaController = MediaController(this).apply {
            setAnchorView(findViewById(R.id.container))
        }

        (findViewById<View>(R.id.videos) as VideoView).apply {
            setMediaController(mediaController)
            setVideoURI(uri)
            start()
        }
    }
}