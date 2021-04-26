package com.hcmus.clc18se.photos.utils

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build

class GPSImage internal constructor(filepath: String?, uri: Uri?, context: Context){
    // your Final lat Long Values
    public var Latitude: Double? = null
    public var Longitude: Double? = null
    private fun convertToDegree(stringDMS: String): Double {
        var result: Double? = null
        val DMS = stringDMS.split(",".toRegex(), 3).toTypedArray()
        val stringD = DMS[0].split("/".toRegex(), 2).toTypedArray()
        val D0: Double = stringD[0].toDouble()
        val D1: Double = stringD[1].toDouble()
        val FloatD = D0 / D1
        val stringM = DMS[1].split("/".toRegex(), 2).toTypedArray()
        val M0: Double = stringM[0].toDouble()
        val M1: Double = stringM[1].toDouble()
        val FloatM = M0 / M1
        val stringS = DMS[2].split("/".toRegex(), 2).toTypedArray()
        val S0: Double = stringS[0].toDouble()
        val S1: Double = stringS[1].toDouble()
        val FloatS = S0 / S1
        result = (FloatD + FloatM / 60 + FloatS / 3600).toDouble()
        return result
    }

    override fun toString(): String {
        // TODO Auto-generated method stub
        return (Latitude.toString() + ", "
                + Longitude.toString())
    }

    val latitudeE6: Int
        get() = Latitude!!.toInt()
    val longitudeE6: Int
        get() = Longitude!!.toInt()


    init {
        try {
            var exif: ExifInterface? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                exif = ExifInterface(context.getContentResolver().openInputStream(uri!!)!!)
            else
                exif = ExifInterface(filepath!!)
            val LATITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val LATITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
            val LONGITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            val LONGITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
            if (LATITUDE != null
                    && LATITUDE_REF != null
                    && LONGITUDE != null
                    && LONGITUDE_REF != null) {
                Latitude = if (LATITUDE_REF == "N") {
                    convertToDegree(LATITUDE)
                } else {
                    0 - convertToDegree(LATITUDE)
                }
                Longitude = if (LONGITUDE_REF == "E") {
                    convertToDegree(LONGITUDE)
                } else {
                    0 - convertToDegree(LONGITUDE)
                }
            }
        }
        catch (e:Exception)
        {

        }
    }
}