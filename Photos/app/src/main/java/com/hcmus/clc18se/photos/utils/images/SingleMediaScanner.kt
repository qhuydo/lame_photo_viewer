package com.hcmus.clc18se.photos.utils.images

import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import java.io.File

//link https://stackoverflow.com/questions/4646913/android-how-to-use-mediascannerconnection-scanfile
class SingleMediaScanner(context: Context?, private val mFile: File) : MediaScannerConnectionClient {
    private val mediaScannerConnection = MediaScannerConnection(context, this)
    override fun onMediaScannerConnected() {
        mediaScannerConnection.scanFile(mFile.absolutePath, null)
    }

    override fun onScanCompleted(path: String, uri: Uri) {
        mediaScannerConnection.disconnect()
    }

    init {
        mediaScannerConnection.connect()
    }
}