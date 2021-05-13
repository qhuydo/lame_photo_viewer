package com.hcmus.clc18se.photos.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.hcmus.clc18se.photos.PhotosApplication.Companion.list
import com.hcmus.clc18se.photos.PhotosApplication.Companion.newList
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.utils.getBitMap
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

    private fun listFaceImage(list: List<MediaItem>): List<MediaItem> {
        val numberMediaItem = list.size
        var numberDetected = 0
        val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {

            setContentTitle(getString(R.string.detect_face_title))
            setContentText(getString(R.string.detect_in_progress))
            setSmallIcon(R.drawable.ic_baseline_face_24)

            setProgress(numberMediaItem, 0, false)
            priority = NotificationCompat.PRIORITY_LOW
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
                            val context = applicationContext
                            Timber.d("${item.mimeType} ${item.name}")
                            try {
                                if (item.isVideo()) continue
                                var bitmap: Bitmap = context.contentResolver.getBitMap(item.requireUri())
                                var scale = 1
                                val byteBitmap = bitmap.width * bitmap.height * 4
                                while (byteBitmap / scale / scale > 2000000) {
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
                                    // Timber.d("Face")
                                    val intent = Intent()
                                    intent.action = "test.Broadcast"
                                    sendBroadcast(intent)
                                }
                            }
                            catch (e: NullPointerException)
                            {

                            }
                            numberDetected++
                            builder.setContentText("$numberDetected/$numberMediaItem")
                            builder.setProgress(numberMediaItem, numberDetected, false)
                            notify(1, builder.build())
                        }
                        builder.setContentText("Detect face complete")
                                .setProgress(0, 0, false)
                        notify(1, builder.build())
                    } catch (ex: Exception) {
                        Timber.d("Exception in thread")
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
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}