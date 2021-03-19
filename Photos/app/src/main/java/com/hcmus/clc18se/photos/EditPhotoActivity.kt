package com.hcmus.clc18se.photos

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max


class EditPhotoActivity : AppCompatActivity() {

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val HIGHEST_COLOR_VALUE = 255
    private val LOWEST_COLOR_VALUE = 0
    private val BLUR_RADIUS = 25f
    private val binding by lazy { ActivityEditPhotoBinding.inflate(layoutInflater) }
    private var cur_item_id = 0
    private val bottomAppBarItemKey: String = "curItemId"
    private var uri: Uri? = null
    private var bitmap: Bitmap? = null
    private var tempRed: Int = 100
    private var tempGreen: Int = 100
    private var tempBlue: Int = 100
    private var isCrop:Boolean = false
    private var viewCrop:CropImageView? = null

    // a task that runs in background
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (intent.hasExtra("uri")) {
            uri = intent.getParcelableExtra("uri")

            CoroutineScope(Dispatchers.IO).launch {
                bitmap = getBitMapFromUri()
            }

            // binding.imageEdit.setImageURI(uri)
            bindImage(binding.imageEdit, uri)
            //binding.imageEdit.setImageBitmap(bitmap)
        }

        viewCrop = binding.cropEditor.findViewById<CropImageView>(R.id.cropImageView)

        val bottomNavigation = binding.bottomEdit
        bottomNavigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        setBarVisibility(R.id.bright)
        savedInstanceState?.let {
            cur_item_id = it.getInt(bottomAppBarItemKey)
            setBarVisibility(cur_item_id)
        }
        val brightSeekBar = binding.brightEditor.findViewById<SeekBar>(R.id.bright_seek_bar)
        brightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
            ) {
                val brightness = brightSeekBar.progress
                binding.imageEdit.colorFilter = setBrightness(brightness)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        val redSeekBar = binding.colorEditor.findViewById<SeekBar>(R.id.editor_red)
        redSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
            ) {
                tempRed = redSeekBar.progress
                binding.imageEdit.colorFilter = setColor(tempRed, tempGreen, tempBlue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        val greenSeekBar = binding.colorEditor.findViewById<SeekBar>(R.id.editor_green)
        greenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
            ) {
                tempGreen = greenSeekBar.progress
                binding.imageEdit.colorFilter = setColor(tempRed, tempGreen, tempBlue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        val blueSeekBar = binding.colorEditor.findViewById<SeekBar>(R.id.editor_blue)
        blueSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
            ) {
                tempBlue = blueSeekBar.progress
                binding.imageEdit.colorFilter = setColor(tempRed, tempGreen, tempBlue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        // cancel the job when activity is destroyed to prevent memory leak
        job.cancel()
        super.onDestroy()
    }

    fun pencilImage(view: View) {
        bitmap?.let {
            binding.progressCircular.visibility = View.VISIBLE
            scope.launch {
                val bitmap = toPencilImage(it)
                withContext(Dispatchers.Main) {
                    binding.imageEdit.setImageBitmap(bitmap)
                    binding.progressCircular.visibility = View.INVISIBLE
                }
            }
        }
    }

    //link hàm  toPencilImage https://github.com/theshivamlko/ImageFilterAlogrithm/tree/master/ImageFIlters
    private fun toPencilImage(bmp: Bitmap): Bitmap {
        val newBitmap: Bitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)

        val imageHeight = newBitmap.height
        val imageWidth = newBitmap.width

        // traversing each pixel in Image as an 2D Array
        for (i in 0 until imageWidth) {
            for (j in 0 until imageHeight) {

                // operating on each pixel
                val oldPixel: Int = bmp.getPixel(i, j)

                // each pixel is made from RED_BLUE_GREEN
                // so, getting current values of pixel
                val oldRed = Color.red(oldPixel)
                val oldBlue = Color.blue(oldPixel)
                val oldGreen = Color.green(oldPixel)
                val oldAlpha = Color.alpha(oldPixel)


                // Algorithm for getting new values after calculation of filter
                // Algorithm for SKETCH FILTER
                val intensity = (oldRed + oldBlue + oldGreen) / 3

                // applying new pixel value to newBitmap
                // condition for Sketch
                var newPixel = 0
                val INTENSITY_FACTOR = 120
                newPixel = if (intensity > INTENSITY_FACTOR) {
                    // apply white color
                    Color.argb(oldAlpha, HIGHEST_COLOR_VALUE, HIGHEST_COLOR_VALUE, HIGHEST_COLOR_VALUE)
                } else if (intensity > 100) {
                    // apply grey color
                    Color.argb(oldAlpha, 150, 150, 150)
                } else {
                    // apply black color
                    Color.argb(oldAlpha, LOWEST_COLOR_VALUE, LOWEST_COLOR_VALUE, LOWEST_COLOR_VALUE)
                }
                newBitmap.setPixel(i, j, newPixel)
            }
        }

        return newBitmap
    }

    fun coloriseImage(view: View) {
        bitmap?.let {
            binding.progressCircular.visibility = View.VISIBLE
            binding.imageEdit.colorFilter =
                    PorterDuffColorFilter(
                            Color.argb(200, 0, 255, 0),
                            PorterDuff.Mode.SRC_OVER)
            binding.progressCircular.visibility = View.INVISIBLE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun grayImage(view: View) {
        bitmap?.let {
            binding.progressCircular.visibility = View.VISIBLE
            scope.launch {
                val bitmap = toGrayscale(it)
                withContext(Dispatchers.Main) {
                    bindImage(binding.imageEdit, bitmap)
                    binding.progressCircular.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun toGrayscale(bmpOriginal: Bitmap): Bitmap? {
        val width: Int
        val height: Int
        height = bmpOriginal.height
        width = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val f = ColorMatrixColorFilter(cm)
        paint.colorFilter = f
        c.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }

    fun invertColors(bmpOriginal: Bitmap): Bitmap? {
        val bitmap = Bitmap.createBitmap(
                bmpOriginal.width,
                bmpOriginal.height,
                Bitmap.Config.ARGB_8888
        )

        val matrixInvert = ColorMatrix(floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        ))


        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(matrixInvert)

        Canvas(bitmap).drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bitmap
    }

    @Suppress("UNUSED_PARAMETER")
    fun blurImage(view: View) {
        bitmap?.let {
            binding.progressCircular.visibility = View.VISIBLE
            scope.launch {
                val bitmap = blur(it)

                withContext(Dispatchers.Main) {
                    bindImage(binding.imageEdit, bitmap)
                    binding.progressCircular.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun blur(image: Bitmap?): Bitmap? {
        if (null == image) return null
        val outputBitmap = Bitmap.createBitmap(image)
        val renderScript = RenderScript.create(this)
        val tmpIn = Allocation.createFromBitmap(renderScript, image)
        val tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap)

        //Intrinsic Gausian blur filter
        val theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)
        return outputBitmap
    }

    fun setBrightness(progress: Int): PorterDuffColorFilter? {
        return if (progress >= 100) {
            val value = (progress - 100) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 255, 255, 255), PorterDuff.Mode.SRC_OVER)
        } else {
            val value = (100 - progress) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        }
    }

    fun setColor(progressRed: Int, progressGreen: Int, progressBlue: Int): PorterDuffColorFilter? {
        val progress = max(max(abs(progressRed - 100), abs(progressGreen - 100)), abs(progressBlue - 100))
        val value = progress * 255 / 100
        return PorterDuffColorFilter(Color.argb(value, progressRed, progressGreen, progressBlue), PorterDuff.Mode.OVERLAY)
    }

    private fun getBitMapFromUri(): Bitmap {
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        return bitmap
    }

    private fun createFileToSave(): File {
        val timeStamp = SimpleDateFormat("yyyyddMM_HHmmss", Locale.CHINA).format(Date())
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(file.path + timeStamp)
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (item.itemId in listOf(R.id.bright,
                        R.id.filter,
                        R.id.add_icon,
                        R.id.crop,
                        R.id.change_color)) {
            if (isCrop)
            {
//                viewCrop!!.croppedImage?.let {
//                    bitmap = viewCrop!!.croppedImage
//                    bindImage(binding.imageEdit, bitmap)
//                }
                isCrop = false
            }
            setBarVisibility(item.itemId)

            cur_item_id = item.itemId
            return@OnNavigationItemSelectedListener true
        }
        false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(bottomAppBarItemKey, cur_item_id)
    }

    private fun setBarVisibility(itemId: Int) {
        binding.apply {
            fragmentContainerEditPhoto.visibility = View.VISIBLE
            brightEditor.visibility = View.GONE
            filterEditor.visibility = View.GONE
            addIconEditor.visibility = View.GONE
            cropEditor.visibility = View.GONE
            colorEditor.visibility = View.GONE
            drawEditor.visibility = View.GONE
        }
        when (itemId) {
            R.id.bright -> binding.brightEditor.visibility = View.VISIBLE
            R.id.filter -> binding.filterEditor.visibility = View.VISIBLE
            R.id.add_icon -> binding.addIconEditor.visibility = View.VISIBLE
            R.id.crop ->{
                binding.fragmentContainerEditPhoto.visibility = View.GONE
                viewCrop!!.setImageBitmap(bitmap)
                binding.cropEditor.visibility = View.VISIBLE
                isCrop = true
            }
            R.id.change_color -> binding.colorEditor.visibility = View.VISIBLE
            R.id.draw -> binding.drawEditor.visibility = View.VISIBLE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDrawButtonClick(view: View) {
        setBarVisibility(R.id.draw)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSaveImageButtonClick(view: View) {

    }

}