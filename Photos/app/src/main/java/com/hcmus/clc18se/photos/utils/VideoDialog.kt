package com.hcmus.clc18se.photos.utils

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import com.hcmus.clc18se.photos.R


class VideoDialog : Activity() {
    private var uri: Uri? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = intent.getParcelableExtra("uri")
        setContentView(R.layout.video_view_dialog)
        val videoView = findViewById<View>(R.id.videos) as VideoView
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(uri!!)
        videoView.start()
    }
}