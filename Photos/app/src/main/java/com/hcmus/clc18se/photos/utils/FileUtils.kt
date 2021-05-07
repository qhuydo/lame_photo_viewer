package com.hcmus.clc18se.photos.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import timber.log.Timber
import java.io.*

/**
 * Move the file from given input path to a given directory
 *
 * - This function should be used below Android R.
 *  - `android:requestLegacyExternalStorage="true"` must be declared to use the function in Android Q.
 * - The permissions in the manifest `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />`
 *  must be declared.
 * - Don't forget to add a slash at the end of `inputPath` and `outputPath`, Ex: `/sdcard/` NOT `/sdcard`.
 * - Don't forget to execute these on a background thread.
 *
 * htps://stackoverflow.com/questions/4178168/how-to-programmatically-move-copy-and-delete-files-and-directories-on-sd"
 */
fun moveFile(inputPath: String, inputFile: String, outputPath: String) {

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        var inputStream: InputStream? = null
        var out: OutputStream? = null
        try {

            //create output directory if it doesn't exist
            val dir = File(outputPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            inputStream = FileInputStream(inputPath + inputFile)
            out = FileOutputStream(outputPath + inputFile)

            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }

            // write the output file
            out.flush()

            // delete the original file
            File(inputPath + inputFile).delete()
        } catch (fileNotFoundException: FileNotFoundException) {
            Timber.e(fileNotFoundException)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            inputStream?.close()
            out?.close()
        }
    }
}

fun ContentResolver.getBitMap(uri: Uri): Bitmap {
    try {
        if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            return MediaStore.Images.Media.getBitmap(this, uri)
        } else {
            val source = ImageDecoder.createSource(this, uri)
            return ImageDecoder.decodeBitmap(source)
        }

    } catch (e: Exception) {
        throw e
    }
}