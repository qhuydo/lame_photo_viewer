package com.hcmus.clc18se.photos.utils.images

import android.content.Context
import android.location.Geocoder
import android.media.ExifInterface
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.lang.Math.floor

class GPSImage {
    var latitude: Double? = null
    var longitude: Double? = null
    lateinit var exif:ExifInterface

    private fun convertToDegree(stringDMS: String): Double {
        val result: Double?

        val dms = stringDMS.split(",".toRegex(), 3).toTypedArray()
        val stringD = dms[0].split("/".toRegex(), 2).toTypedArray()

        val d0: Double = stringD[0].toDouble()
        val d1: Double = stringD[1].toDouble()

        val floatD = d0 / d1

        val stringM = dms[1].split("/".toRegex(), 2).toTypedArray()
        val m0: Double = stringM[0].toDouble()
        val m1: Double = stringM[1].toDouble()

        val floatM = m0 / m1
        val stringS = dms[2].split("/".toRegex(), 2).toTypedArray()

        val s0: Double = stringS[0].toDouble()
        val s1: Double = stringS[1].toDouble()
        val floatS = s0 / s1

        result = (floatD + floatM / 60 + floatS / 3600)
        return result
    }

    override fun toString(): String {
        return "$latitude, $longitude"
    }

//    val latitudeE6: Int
//        get() = latitude!!.toInt()
//    val longitudeE6: Int
//        get() = longitude!!.toInt()

    @RequiresApi(Build.VERSION_CODES.N)
    constructor(inputStream: InputStream) {
        try {
            exif = ExifInterface(inputStream)
//            val floatArray = FloatArray(2)
//            val hasLatLong = exif.getLatLong(floatArray)
//            if (hasLatLong) {
//                latitude = floatArray.get(0).toDouble()
//                longitude = floatArray.get(1).toDouble()
//            } else {
//            }
            getLatLong(exif)

        } catch (ex: Exception) {
            Timber.e("$ex")
        }
    }

    constructor(path: String?) {
        try {
            if (path != null){
                exif = ExifInterface(path)
                getLatLong(exif)
            }

        } catch (ex: Exception) {
            Timber.e("$ex")
        }
    }

    private fun getLatLong(exifInterface: ExifInterface) = exifInterface.run {
        val lat = getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val latRef = getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val long = getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val longRef = getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
        if (lat != null
                && latRef != null
                && long != null
                && longRef != null) {
            latitude = if (latRef == "N") {
                convertToDegree(lat)
            } else {
                0 - convertToDegree(lat)
            }
            longitude = if (longRef == "E") {
                convertToDegree(long)
            } else {
                0 - convertToDegree(long)
            }
        }
    }

    fun getDMSLatitude():String?{
        latitude?.let {
            val degrees = floor(latitude!!)
            val minutes = floor(60.0 * (latitude!! - degrees))
            val seconds = 3600.0 * (latitude!! - degrees) - (60.0 * minutes) * 10000
            return degrees.toString() + "/1," + minutes.toString() + "/1," + seconds.toLong().toString() + "/10000"
        }
        return null
    }

    fun getDMSLongtitude():String?{
        latitude?.let {
            val degrees = floor(longitude!!)
            val minutes = floor(60.0 * (longitude!! - degrees))
            val seconds = 3600.0 * (longitude!! - degrees) - (60.0 * minutes)
            return degrees.toString() + "/1," + minutes.toString() + "/1," + seconds.toLong().toString() + "/1"
        }
        return null
    }

    fun geoTag(latitude1: Double, longitude1: Double) {
        var latitude = latitude1
        var longitude = longitude1
        try {
            val num1Lat = floor(latitude).toInt()
            val num2Lat = floor((latitude - num1Lat) * 60).toInt()
            val num3Lat = (latitude - (num1Lat.toDouble() + num2Lat.toDouble() / 60)) * 360000000
            val num1Lon = floor(longitude).toInt()
            val num2Lon = floor((longitude - num1Lon) * 60).toInt()
            val num3Lon = (longitude - (num1Lon.toDouble() + num2Lon.toDouble() / 60)) * 360000000
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "$num1Lat/1,$num2Lat/1,${num3Lat.toLong()}/1000")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "$num1Lon/1,$num2Lon/1,${num3Lon.toLong()}/1000")
            if (latitude > 0) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            } else {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S")
            }
            if (longitude > 0) {
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            } else {
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            }
            exif.saveAttributes()
        } catch (e: IOException) {
            Log.e("Error", e.localizedMessage)
        }
    }


    companion object {

        fun getAddressFromGPSImage(gpsImage: GPSImage, context: Context): String? {
            val latitude = gpsImage.latitude
            val longitude = gpsImage.longitude

            if (latitude != null && longitude != null) {
                val geoCoder = Geocoder(context)
                try {
                    val list = geoCoder.getFromLocation(latitude, longitude, 1)
                    if (list.isNotEmpty()
                            && list[0].getAddressLine(0) != null) {
                        return list.first().getAddressLine(0)
                    }

                } catch (ioEx: IOException) {
                    return null
                }

            }
            return null
        }
    }

}
