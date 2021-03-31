package com.hcmus.clc18se.photos

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import com.hcmus.clc18se.photos.utils.DrawableImageView
import com.hcmus.clc18se.photos.utils.svg.SingleMediaScanner
import com.theartofdev.edmodo.cropper.CropImageView
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import kotlinx.coroutines.*
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max


class EditPhotoActivity : AppCompatActivity() {

    companion object {
        const val HIGHEST_COLOR_VALUE = 255
        const val LOWEST_COLOR_VALUE = 0
        const val BLUR_RADIUS = 25f
        const val PICK_IMAGE_INTENT = 2000

        const val BUNDLE_URI = "uri"
        const val BUNDLE_BITMAP = "bitmap"
    }

    protected val colorResource by lazy { (application as PhotosApplication).colorResource }

    private val binding by lazy { ActivityEditPhotoBinding.inflate(layoutInflater) }
    private var cur_item_id = 0
    private val bottomAppBarItemKey: String = "curItemId"

    private var uri: Uri? = null
    private var bitmap: Bitmap? = null

    private var brightness: Int = 100
    private var tempRed: Int = 100
    private var tempGreen: Int = 100
    private var tempBlue: Int = 100
    private var isCrop: Boolean = false
    private var isDraw: Boolean = false
    private var isAddIcon: Boolean = false

    private lateinit var viewCrop: CropImageView
    private lateinit var viewDraw: DrawableImageView
    private lateinit var viewAddIcon: PhotoEditorView
    private lateinit var photoEditor: PhotoEditor

    private var curColorDraw: Int = Color.GREEN
    private var curWeightDraw: Int = 5

    // use in draw in picture
    var downx: Float = 0f
    var downy = 0f
    var upx = 0f
    var upy = 0f
    var alteredBitmap: Bitmap? = null
    var canvas: Canvas? = null
    var paint: Paint? = null
    var matrix: Matrix? = null

    // a task that runs in background
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            cur_item_id = it.getInt(bottomAppBarItemKey)
            setBarVisibility(cur_item_id)

            uri = it.getParcelable(BUNDLE_URI)
            bitmap = it.getParcelable(BUNDLE_BITMAP)

        }

        colorResource.configColor(this)
        colorResource.configTheme()

        setContentView(binding.root)

        if (intent.hasExtra("uri")) {
            uri = intent.getParcelableExtra("uri")

            CoroutineScope(Dispatchers.IO).launch {
                bitmap = getBitMapFromUri(uri!!)
            }

            bindImage(binding.imageEdit, uri)
        }

        viewCrop = binding.cropEditor.cropImageView
        viewDraw = binding.drawEditor.drawImageView
        viewAddIcon = binding.addIconEditor.addIconImageView

        photoEditor = PhotoEditor.Builder(this, viewAddIcon)
            .setPinchTextScalable(true)
            .build()

        val bottomNavigation = binding.bottomEdit
        bottomNavigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        setBarVisibility(R.id.bright)


        val brightSeekBar = binding.brightEditor.brightSeekBar
        val redSeekBar = binding.colorEditor.editorRed
        val greenSeekBar = binding.colorEditor.editorGreen
        val blueSeekBar = binding.colorEditor.editorBlue
        val weightSeekBar = binding.drawConfigEditor.weightSeekbar

        brightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                brightness = brightSeekBar.progress
                tempRed = brightness
                tempGreen = brightness
                tempBlue = brightness
                redSeekBar.progress = brightness
                greenSeekBar.progress = brightness
                blueSeekBar.progress = brightness
                binding.imageEdit.colorFilter = setBrightness(brightness)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
        weightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                curWeightDraw = weightSeekBar.progress
                viewDraw.setWeight(curWeightDraw)
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

    @Suppress("UNUSED_PARAMETER")
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
                    Color.argb(
                        oldAlpha,
                        HIGHEST_COLOR_VALUE,
                        HIGHEST_COLOR_VALUE,
                        HIGHEST_COLOR_VALUE
                    )
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

    @Suppress("UNUSED_PARAMETER")
    fun coloriseImage(view: View) {
        bitmap?.let {
            binding.progressCircular.visibility = View.VISIBLE
            binding.imageEdit.colorFilter =
                PorterDuffColorFilter(
                    Color.argb(200, 0, 255, 0),
                    PorterDuff.Mode.SRC_OVER
                )
            binding.progressCircular.visibility = View.INVISIBLE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun grayImage(view: View) {
        bitmap?.let {
            binding.progressCircular.visibility = View.VISIBLE
            scope.launch {
                bitmap = toGrayscale(it)
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

        val matrixInvert = ColorMatrix(
            floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )


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
                bitmap = blur(it)

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

    fun setBrightness(progress: Int): PorterDuffColorFilter {
        return if (progress >= 100) {
            val value = (progress - 100) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 255, 255, 255), PorterDuff.Mode.SRC_OVER)
        } else {
            val value = (100 - progress) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        }
    }

    fun setColor(progressRed: Int, progressGreen: Int, progressBlue: Int): PorterDuffColorFilter {
        val progress = max(
            max(abs(progressRed - 100), abs(progressGreen - 100)),
            abs(progressBlue - 100)
        )
        val value = progress * 255 / 100
        return PorterDuffColorFilter(
            Color.argb(value, progressRed, progressGreen, progressBlue),
            PorterDuff.Mode.SRC_OVER
        )
    }

    private fun getBitMapFromUri(uri: Uri): Bitmap {
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        return bitmap
    }

    private fun createFileToSave(): File {
        val timeStamp = SimpleDateFormat("yyyyddMM_HHmmss", Locale.CHINA).format(Date())
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(file.path, "$timeStamp.jpg")
    }

    private fun handlingBitmap() {
        if (isCrop) {
            viewCrop.let {
                bitmap = it.croppedImage
                bindImage(binding.imageEdit, bitmap)
                isCrop = false
            }
        }
        if (isDraw) {
            viewDraw.let {
                bitmap = it.drawable.toBitmap(bitmap!!.width, bitmap!!.height, bitmap!!.config)
                bindImage(binding.imageEdit, bitmap)
                isCrop = false
            }
        }
        if (isAddIcon) {
            viewAddIcon.let {
                photoEditor.saveAsBitmap(object : OnSaveBitmap {
                    override fun onBitmapReady(saveBitmap: Bitmap) {
                        bitmap = saveBitmap
                        bindImage(binding.imageEdit, bitmap)
                    }

                    override fun onFailure(exception: Exception) {

                    }
                })
                isAddIcon = false
            }
        }
    }

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            if (item.itemId in listOf(
                    R.id.bright,
                    R.id.filter,
                    R.id.add_icon,
                    R.id.crop,
                    R.id.change_color
                )
            ) {
                handlingBitmap()
                setBarVisibility(item.itemId)

                cur_item_id = item.itemId
                return@OnNavigationItemSelectedListener true
            }
            false
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        handlingBitmap()

        outState.putInt(bottomAppBarItemKey, cur_item_id)
        outState.putParcelable(BUNDLE_BITMAP, bitmap)
        outState.putParcelable(BUNDLE_URI, uri)
    }

    private fun setBarVisibility(itemId: Int) {
        binding.apply {
            fragmentContainerEditPhoto.visibility = View.VISIBLE
            brightEditor.brightEditorLayout.visibility = View.GONE
            filterEditor.visibility = View.GONE
            addEditor.visibility = View.GONE
            cropEditor.cropEditorLayout.visibility = View.GONE
            colorEditor.colorEditorLayout.visibility = View.GONE
            drawEditor.drawEditorLayout.visibility = View.GONE
            drawConfigEditor.drawConfigLayout.visibility = View.GONE
            addIconEditor.addIconEditorLayout.visibility = View.GONE
            addIconConfigEditor.addIconConfigLayout.visibility = View.GONE
        }
        when (itemId) {
            R.id.bright -> binding.brightEditor.brightEditorLayout.visibility = View.VISIBLE
            R.id.filter -> binding.filterEditor.visibility = View.VISIBLE
            R.id.add_icon -> binding.addEditor.visibility = View.VISIBLE
            R.id.crop -> {
                binding.fragmentContainerEditPhoto.visibility = View.INVISIBLE
                viewCrop.setImageUriAsync(uri)
                binding.cropEditor.cropEditorLayout.visibility = View.VISIBLE
                isCrop = true

            }
            R.id.change_color -> binding.colorEditor.colorEditorLayout.visibility = View.VISIBLE
        }
    }

    fun onDrawMode(view: View) {
        binding.fragmentContainerEditPhoto.visibility = View.INVISIBLE
        binding.addEditor.visibility = View.GONE
        binding.drawConfigEditor.drawConfigLayout.visibility = View.VISIBLE
        alteredBitmap = Bitmap.createBitmap(
            bitmap!!.width, bitmap!!
                .height, bitmap!!.getConfig()
        )
        viewDraw.setNewImage(alteredBitmap, bitmap, setColor(tempRed, tempGreen, tempBlue))
        viewDraw.setNewColor(curColorDraw)
        viewDraw.setWeight(curWeightDraw)
        binding.drawEditor.drawEditorLayout.visibility = View.VISIBLE
        isDraw = true

    }

    fun onAddIconMode(view: View) {
        binding.fragmentContainerEditPhoto.visibility = View.INVISIBLE
        binding.addEditor.visibility = View.GONE
        binding.addIconConfigEditor.addIconConfigLayout.visibility = View.VISIBLE
        viewAddIcon.getSource().setImageBitmap(bitmap);
        viewAddIcon.getSource().setColorFilter(setColor(tempRed, tempGreen, tempBlue));
        binding.addIconEditor.addIconEditorLayout.visibility = View.VISIBLE

    }

    @SuppressLint("ShowToast")
    fun onSaveImageButtonClick(view: View) {
        val fileSave = createFileToSave()
        handlingBitmap()
        try {
            var stream: OutputStream? = null
            stream = FileOutputStream(fileSave)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to save Image", Toast.LENGTH_LONG).show()
            return
        }
        SingleMediaScanner(this, fileSave)
        Toast.makeText(this, "Image Saved Successfully", Toast.LENGTH_LONG).show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPickColorButtonClick(view: View) {
        val colorPicker = AmbilWarnaDialog(this, curColorDraw, object : OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {}
            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                curColorDraw = color
                binding.drawConfigEditor.pickColor.setBackgroundColor(curColorDraw)
                viewDraw!!.setNewColor(curColorDraw)
            }
        })
        colorPicker.show()
    }

    fun pickImageFromPhone(view: View) {
        val i = Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(i, PICK_IMAGE_INTENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_INTENT && resultCode == RESULT_OK) {
            val uriPick = data!!.data
            photoEditor.addImage(getBitMapFromUri(uriPick!!))
            isAddIcon = true
        }
    }
}