package com.hcmus.clc18se.photos.service

import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.hcmus.clc18se.photos.PhotosApplication.Companion.list
import com.hcmus.clc18se.photos.PhotosApplication.Companion.newList
import com.hcmus.clc18se.photos.data.MediaItem
import timber.log.Timber


class DetectFace : Service() {
    private val CHANNEL_ID = "2021"
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStart(intent: Intent?, startId: Int) {
        createNotificationChannel()
        list.let {
            listFaceImage(list!!)
        }
        this.stopSelf()
    }

    @SuppressLint("SetTextI18n")
    private fun listFaceImage(list: List<MediaItem>): List<MediaItem> {
        val numberMediaItem = list.size
        var numberDetected = 0
        val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("Detect face")
            setContentText("Detect in progress")
            setSmallIcon(android.R.drawable.ic_media_next)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setProgress(numberMediaItem, 0, false)
        }
        val detector: FaceDetector = FaceDetector.Builder(baseContext)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build()
        NotificationManagerCompat.from(this).apply {
            // Issue the initial notification with zero progress
            object : Thread() {
                override fun run() {
                    //If there are stories, add them to the table
                    try {
                        for (item in list) {
                            Timber.d("${item.mimeType} ${item.name}")
                            try {
                                if (item.isVideo()) continue
                                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(baseContext.contentResolver, item.requireUri())
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
                                    val intent = Intent()
                                    intent.action = "test.Broadcast"
                                    sendBroadcast(intent)
                                }
                            }
                            catch (e: NullPointerException)
                            {

                            }
                            numberDetected++
                            builder.setContentText(numberDetected.toString() + "/" + numberMediaItem.toString())
                            builder.setProgress(numberMediaItem, numberDetected, false);
                            notify(1, builder.build())
                        }
                        builder.setContentText("Detect face complete")
                                .setProgress(0, 0, false)
                        notify(1, builder.build())
                    } catch (ex: Exception) {
                        Log.d("---", "Exception in thread")
                    }
                }
            }.start()
        }
        return newList
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.unknownName)
            val description = getString(R.string.unknownName)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}