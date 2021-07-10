package com.hcmus.clc18se.photos

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoDialogActivity : AppCompatActivity() {

    companion object {
        val BUNDLE_CURRENT_POSITION = "BUNDLE_CURRENT_POSITION"
        val BUNDLE_IS_PLAYING = "BUNDLE_IS_PLAYING"
    }

    private lateinit var uri: Uri
    private lateinit var videoView: VideoView
    private var currentPosition = 0
    private var isPlaying = true

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = intent.getParcelableExtra("uri")!!

        if (savedInstanceState != null
            && savedInstanceState.containsKey(BUNDLE_CURRENT_POSITION)
            && savedInstanceState.containsKey(BUNDLE_IS_PLAYING)
        ) {
            currentPosition = savedInstanceState.getInt(BUNDLE_CURRENT_POSITION)
            isPlaying = savedInstanceState.getBoolean(BUNDLE_IS_PLAYING)
        }

        setContentView(R.layout.video_view_dialog)

        val mediaController = MediaController(this).apply {
            setAnchorView(findViewById(R.id.container))
        }

        videoView = (findViewById<View>(R.id.videos) as VideoView).apply {

            setMediaController(mediaController)
            setVideoURI(uri)
            // Proportional Scaling the video view
            setOnPreparedListener {
                setDimension(this)
            }
        }
        if (isPlaying) {
            videoView.start()
        }
        videoView.seekTo(currentPosition)
    }

    private fun setDimension(videoView: VideoView) {
        // Adjust the size of the video
        // so it fits on the screen
        val videoProportion = videoView.height.toFloat() / videoView.width

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val screenProportion = screenHeight.toFloat() / screenWidth

        val layoutParams = videoView.layoutParams

        if (videoProportion < screenProportion) {
            layoutParams.height = screenHeight
            layoutParams.width = (screenHeight.toFloat() / videoProportion).toInt()
        } else {
            layoutParams.width = screenWidth
            layoutParams.height = (screenWidth.toFloat() * videoProportion).toInt()
        }
        videoView.layoutParams = layoutParams
    }

    override fun onPause() {
        super.onPause()
        currentPosition = videoView.currentPosition
        isPlaying = videoView.isPlaying
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_CURRENT_POSITION, currentPosition)
        outState.putBoolean(BUNDLE_IS_PLAYING, isPlaying)
    }

}