package com.hcmus.clc18se.photos

import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.view.drawToBitmap
import androidx.exifinterface.media.ExifInterface
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import com.hcmus.clc18se.photos.utils.images.ConvolutionMatrix
import com.hcmus.clc18se.photos.utils.images.DrawableImageView
import com.hcmus.clc18se.photos.utils.UndoPhoto
import com.hcmus.clc18se.photos.utils.getBitMap
import com.hcmus.clc18se.photos.utils.images.SingleMediaScanner
import com.hcmus.clc18se.photos.utils.ui.allColorPaletteRes
import com.theartofdev.edmodo.cropper.CropImageView
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.SaveSettings
import kotlinx.coroutines.*
import java.io.*
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

// TODO: refactor me
class EditPhotoActivity : AppCompatActivity() {

    companion object {
        const val HIGHEST_COLOR_VALUE = 255

        // const val LOWEST_COLOR_VALUE = 0
        const val BLUR_RADIUS = 25f
        const val PICK_IMAGE_INTENT = 2000
    }

    protected val colorResource by lazy { (application as PhotosApplication).colorResource }

    private val binding by lazy { ActivityEditPhotoBinding.inflate(layoutInflater) }
    private var curItemId = 0
    // private val bottomAppBarItemKey: String = "curItemId"

    private lateinit var uri: Uri
    private lateinit var bitmap: Bitmap
    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar

    private var brightness: Int = 100
    private var tempRed: Int = 100
    private var tempGreen: Int = 100
    private var tempBlue: Int = 100
    private var isCrop: Boolean = false
    private var isDraw: Boolean = false
    private var isAddIcon: Boolean = false
    private var indexQueue: Int = 0
    private var queue: LinkedList<UndoPhoto> = LinkedList<UndoPhoto>()

    private lateinit var viewCrop: CropImageView
    private lateinit var viewDraw: DrawableImageView
    private lateinit var viewAddIcon: PhotoEditorView
    private lateinit var photoEditor: PhotoEditor

    private var curColorDraw: Int = Color.GREEN
    private var curWeightDraw: Int = 5

    // use in draw in picture
    // var downx: Float = 0f
    // var downy = 0f
    // var upx = 0f
    // var upy = 0f
    private lateinit var alteredBitmap: Bitmap
    // var canvas: Canvas? = null
    // var paint: Paint? = null
    // var matrix: Matrix? = null

    // a task that runs in background
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        colorResource.configColor(this)
        colorResource.configTheme()

        setContentView(binding.root)

        if (intent.hasExtra("uri")) {
            uri = intent.getParcelableExtra("uri")!!

            CoroutineScope(Dispatchers.IO).launch {
                bitmap = getBitMapFromUri(uri)
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

                withContext(Dispatchers.Main) {
                    addToQueue(bitmap, binding.imageEdit.colorFilter)
                    bindImage(binding.imageEdit, uri)
                }
            }
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
        redSeekBar = binding.colorEditor.editorRed
        greenSeekBar = binding.colorEditor.editorGreen
        blueSeekBar = binding.colorEditor.editorBlue
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

    private fun addToQueue(bitmap: Bitmap, colorFilter: ColorFilter?) {
        while (indexQueue < queue.size - 1)
            queue.poll()
        while (queue.size > 4)
            queue.poll()
        queue.add(UndoPhoto(bitmap, colorFilter))
        indexQueue = queue.size - 1
    }

    private fun undoFromQueue() {
        if (queue.size == 0 || indexQueue - 1 < 0) {
            return
        }
        indexQueue--

        if (isDraw) {
            alteredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            viewDraw.setNewImage(alteredBitmap, queue[indexQueue].bitmap, queue[indexQueue].colorFilter)
        }

        if (isAddIcon) {
            viewAddIcon.source.setImageBitmap(queue[indexQueue].bitmap)
            viewAddIcon.source.colorFilter = queue[indexQueue].colorFilter
        }

        if (!isDraw && !isAddIcon) {
            binding.imageEdit.setImageBitmap(queue[indexQueue].bitmap)
            binding.imageEdit.colorFilter = queue[indexQueue].colorFilter
            binding.progressCircular.visibility = View.INVISIBLE
        }
    }

    private fun redoFromQueue() {
        if (queue.size == 0 || indexQueue + 1 > queue.size - 1) {
            return
        }
        indexQueue++

        if (isDraw) {
            alteredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            viewDraw.setNewImage(alteredBitmap, queue[indexQueue].bitmap, queue[indexQueue].colorFilter)
        }

        if (isAddIcon) {
            viewAddIcon.source.setImageBitmap(queue[indexQueue].bitmap)
            viewAddIcon.source.colorFilter = queue[indexQueue].colorFilter
        }

        if (!isDraw && !isAddIcon) {
            binding.imageEdit.setImageBitmap(queue[indexQueue].bitmap)
            binding.imageEdit.colorFilter = queue[indexQueue].colorFilter
            bitmap = queue[indexQueue].bitmap
            binding.progressCircular.visibility = View.INVISIBLE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun pencilImage(view: View) {
        binding.progressCircular.visibility = View.VISIBLE

        scope.launch {
            bitmap = toPencilImage(bitmap)
            addToQueue(bitmap, binding.imageEdit.colorFilter)
            withContext(Dispatchers.Main) {
                bindImage(binding.imageEdit, bitmap)
                binding.progressCircular.visibility = View.INVISIBLE
            }
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun snowImage(view: View) {
        binding.progressCircular.visibility = View.VISIBLE

        scope.launch {
            bitmap = applySnowEffect(bitmap)
            addToQueue(bitmap, binding.imageEdit.colorFilter)
            withContext(Dispatchers.Main) {
                bindImage(binding.imageEdit, bitmap)
                binding.progressCircular.visibility = View.INVISIBLE
            }
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun fleaImage(view: View) {

        binding.progressCircular.visibility = View.VISIBLE
        scope.launch {
            bitmap = applyNoiseEffect(bitmap)
            addToQueue(bitmap, binding.imageEdit.colorFilter)
            withContext(Dispatchers.Main) {
                bindImage(binding.imageEdit, bitmap)
                binding.progressCircular.visibility = View.INVISIBLE
            }
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun smoothImage(view: View) {
        binding.progressCircular.visibility = View.VISIBLE
        scope.launch {
            bitmap = smooth(bitmap, 2.0)
            addToQueue(bitmap, binding.imageEdit.colorFilter)
            withContext(Dispatchers.Main) {
                bindImage(binding.imageEdit, bitmap)
                binding.progressCircular.visibility = View.INVISIBLE
            }
        }

    }

    private fun toPencilImage(bmp: Bitmap): Bitmap {
        var scale = 1

        val byteBitmap = bmp.width * bmp.height * 4
        while (byteBitmap / scale / scale > 6000000) {
            scale++
        }

        val newBmp = bmp.copy(Bitmap.Config.ARGB_8888, true).scale(
            bmp.width / scale,
            bmp.height / scale,
            false
        )

        val grayBm = toGrayscale(newBmp)
        val invertBm = invertColors(grayBm)
        val blurBm = blur(invertBm)

        val result = colorDodgeBlend(grayBm, blurBm)

        return result
    }

    @Suppress("UNUSED_PARAMETER")
    fun colorizeImage(view: View) {
        binding.progressCircular.visibility = View.VISIBLE
        binding.imageEdit.colorFilter =
            PorterDuffColorFilter(
                Color.argb(200, 0, 255, 0),
                PorterDuff.Mode.SRC_OVER
            )
        addToQueue(bitmap, binding.imageEdit.colorFilter)
        redSeekBar.progress = 0
        greenSeekBar.progress = 255
        blueSeekBar.progress = 0
        binding.progressCircular.visibility = View.INVISIBLE

    }

    @Suppress("UNUSED_PARAMETER")
    fun grayImage(view: View) {

        binding.progressCircular.visibility = View.VISIBLE
        scope.launch {
            bitmap = toGrayscale(bitmap)
            addToQueue(bitmap, binding.imageEdit.colorFilter)
            withContext(Dispatchers.Main) {
                bindImage(binding.imageEdit, bitmap)
                binding.progressCircular.visibility = View.INVISIBLE
            }
        }

    }

    private fun toGrayscale(bmpOriginal: Bitmap): Bitmap {
        val height: Int = bmpOriginal.height
        val width: Int = bmpOriginal.width

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

    // link: https://xjaphx.wordpress.com/2011/06/22/image-processing-smooth-effect/
    private fun smooth(src: Bitmap, value: Double): Bitmap {
        val convolutionMatrix = ConvolutionMatrix(3).apply {
            setAll(1.0)
            matrix[1][1] = value
            factor = value + 8
            offset = 1.0
        }

        return ConvolutionMatrix.computeConvolution3x3(src, convolutionMatrix)
    }

    // link: https://xjaphx.wordpress.com/2011/10/30/image-processing-snow-effect/
    private fun applySnowEffect(source: Bitmap): Bitmap {

        // get image size
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        // get pixel array from source
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // random object
        val random = Random()

        // iteration through pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                val index = y * width + x

                // get color
                val r = Color.red(pixels[index])
                val g = Color.green(pixels[index])
                val b = Color.blue(pixels[index])

                // generate threshold
                val threshold = random.nextInt(HIGHEST_COLOR_VALUE)
                if (r > threshold && g > threshold && b > threshold) {
                    pixels[index] =
                        Color.rgb(HIGHEST_COLOR_VALUE, HIGHEST_COLOR_VALUE, HIGHEST_COLOR_VALUE)
                }
            }
        }

        // output bitmap
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    // link: https://xjaphx.wordpress.com/2011/10/30/image-processing-flea-noise-effect/
    private fun applyNoiseEffect(source: Bitmap): Bitmap {
        // get image size
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        // get pixel array from source
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // a random object
        val random = Random()

        // iteration through pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                val index = y * width + x
                // get random color
                val randColor = Color.rgb(
                    random.nextInt(HIGHEST_COLOR_VALUE),
                    random.nextInt(HIGHEST_COLOR_VALUE), random.nextInt(HIGHEST_COLOR_VALUE)
                )
                // OR
                pixels[index] = pixels[index] or randColor
            }
        }

        // output bitmap
        return Bitmap.createBitmap(width, height, source.config).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    //link: https://stackoverflow.com/questions/9826273/photo-image-to-sketch-algorithm
    private fun colorDodge(in1: Int, in2: Int): Int {
        val image = in2.toFloat()
        val mask = in1.toFloat()
        return (if (image == 255f) image else 255f.coerceAtMost((mask.toLong() shl 8) / (255 - image))).toInt()
    }

    //link: https://stackoverflow.com/questions/9826273/photo-image-to-sketch-algorithm
    private fun colorDodgeBlend(source: Bitmap, layer: Bitmap): Bitmap {

        val base = source.copy(Bitmap.Config.ARGB_8888, true)
        val blend = layer.copy(Bitmap.Config.ARGB_8888, false)

        val buffBase: IntBuffer = IntBuffer.allocate(base.width * base.height).apply {
            base.copyPixelsToBuffer(this)
            rewind()
        }

        val buffBlend: IntBuffer = IntBuffer.allocate(blend.width * blend.height).apply {
            blend.copyPixelsToBuffer(this)
            rewind()
        }

        val buffOut: IntBuffer = IntBuffer.allocate(base.width * base.height).apply {
            rewind()
        }

        while (buffOut.position() < buffOut.limit()) {

            val filterInt: Int = buffBlend.get()
            val srcInt: Int = buffBase.get()

            val redValueFilter = Color.red(filterInt)
            val greenValueFilter = Color.green(filterInt)
            val blueValueFilter = Color.blue(filterInt)

            val redValueSrc = Color.red(srcInt)
            val greenValueSrc = Color.green(srcInt)
            val blueValueSrc = Color.blue(srcInt)

            val redValueFinal: Int = colorDodge(redValueFilter, redValueSrc)
            val greenValueFinal: Int = colorDodge(greenValueFilter, greenValueSrc)
            val blueValueFinal: Int = colorDodge(blueValueFilter, blueValueSrc)

            val pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal)

            buffOut.put(pixel)
        }

        buffOut.rewind()
        base.copyPixelsFromBuffer(buffOut)
        blend.recycle()

        return base
    }

    private fun invertColors(bmpOriginal: Bitmap): Bitmap {
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

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrixInvert)
        }

        Canvas(bitmap).drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bitmap
    }

    @Suppress("UNUSED_PARAMETER")
    fun blurImage(view: View) {

        binding.progressCircular.visibility = View.VISIBLE
        scope.launch {
            bitmap = blur(bitmap)
            addToQueue(bitmap, binding.imageEdit.colorFilter)
            withContext(Dispatchers.Main) {
                bindImage(binding.imageEdit, bitmap)
                binding.progressCircular.visibility = View.INVISIBLE
            }
        }

    }

    private fun blur(image: Bitmap): Bitmap {

        val outputBitmap = Bitmap.createBitmap(image)
        val renderScript = RenderScript.create(this)

        val tmpIn = Allocation.createFromBitmap(renderScript, image)
        val tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap)

        //Intrinsic Gaussian blur filter
        ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript)).apply {
            setRadius(BLUR_RADIUS)
            setInput(tmpIn)
            forEach(tmpOut)
        }

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
        return contentResolver.getBitMap(uri)
    }

    private fun createFileToSave(): File {
        val timeStamp = SimpleDateFormat("yyyy_dd_MM_HH_mm_ss", Locale.ROOT).format(Date())
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        //TODO: fix the file extension
        return File(file.path, "$timeStamp.jpg")
    }

    private fun handlingBitmap(): Boolean {
        if (isCrop) {

            viewCrop.let {
                bitmap = it.croppedImage
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
                binding.imageEdit.colorFilter = null
                bindImage(binding.imageEdit, bitmap)
            }
            return true
        }

        if (isDraw) {
            viewDraw.let {
                bitmap = it.drawable.toBitmap(bitmap.width, bitmap.height, bitmap.config)
                bindImage(binding.imageEdit, bitmap)
            }
            return true
        }

        if (isAddIcon) {
            viewAddIcon.let {
                val saveSettings = SaveSettings.Builder()
                    .setClearViewsEnabled(false)
                    .setTransparencyEnabled(true)
                    .build()
                photoEditor.saveAsBitmap(saveSettings, object : OnSaveBitmap {
                    override fun onBitmapReady(saveBitmap: Bitmap) {
                        bitmap = saveBitmap
                        bindImage(binding.imageEdit, bitmap)
                    }

                    override fun onFailure(exception: Exception) {

                    }
                })
            }
            return true
        }
        return false
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
                addToQueue(bitmap, binding.imageEdit.colorFilter)
                curItemId = item.itemId
                return@OnNavigationItemSelectedListener true
            }
            false
        }

    private fun setBarVisibility(itemId: Int) {
        binding.apply {
            isCrop = false
            isAddIcon = false
            isDraw = false
            viewDraw.checkDown = false

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
            binding.saveImage.visibility = View.VISIBLE
            binding.rotate.visibility = View.VISIBLE
        }

        when (itemId) {

            R.id.bright -> binding.brightEditor.brightEditorLayout.visibility = View.VISIBLE
            R.id.filter -> binding.filterEditor.visibility = View.VISIBLE
            R.id.add_icon -> binding.addEditor.visibility = View.VISIBLE
            R.id.crop -> {
                binding.fragmentContainerEditPhoto.visibility = View.INVISIBLE
                viewCrop.setImageUriAsync(uri)
                binding.cropEditor.cropEditorLayout.visibility = View.VISIBLE
                binding.rotate.visibility = View.INVISIBLE
                isCrop = true
            }
            R.id.change_color -> binding.colorEditor.colorEditorLayout.visibility = View.VISIBLE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun undoImage(view: View) {
        if (isDraw && viewDraw.checkDown == true)
        {
            addToQueue(viewDraw.drawable.toBitmap(bitmap.width, bitmap.height, bitmap.config),binding.imageEdit.colorFilter)
            viewDraw.checkDown = false
        }
        undoFromQueue()
    }

    @Suppress("UNUSED_PARAMETER")
    fun redoImage(view: View) {
        if (isDraw && viewDraw.checkDown == true)
        {
            addToQueue(viewDraw.drawable.toBitmap(bitmap.width, bitmap.height, bitmap.config),binding.imageEdit.colorFilter)
            viewDraw.checkDown = false
        }
        redoFromQueue()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDrawMode(view: View) {
        binding.apply {
            fragmentContainerEditPhoto.visibility = View.INVISIBLE
            addEditor.visibility = View.GONE
            drawConfigEditor.drawConfigLayout.visibility = View.VISIBLE
        }

        alteredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)

        viewDraw.apply {
            setNewImage(alteredBitmap, bitmap, setColor(tempRed, tempGreen, tempBlue))
            setNewColor(curColorDraw)
            setWeight(curWeightDraw)
        }
        binding.rotate.visibility = View.INVISIBLE
        binding.drawEditor.drawEditorLayout.visibility = View.VISIBLE
        isDraw = true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onAddIconMode(view: View) {
        binding.fragmentContainerEditPhoto.visibility = View.INVISIBLE
        binding.addEditor.visibility = View.GONE
        binding.addIconConfigEditor.addIconConfigLayout.visibility = View.VISIBLE
        viewAddIcon.getSource().setImageBitmap(bitmap);
        viewAddIcon.getSource().setColorFilter(setColor(tempRed, tempGreen, tempBlue));
        binding.addIconEditor.addIconEditorLayout.visibility = View.VISIBLE
        binding.saveImage.visibility = View.INVISIBLE
        binding.rotate.visibility = View.INVISIBLE
    }

    @Suppress("UNUSED_PARAMETER")
    fun rotateImage(view: View){
        val matrix = Matrix()
        matrix.postRotate(90F)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        addToQueue(bitmap,binding.imageEdit.colorFilter)
        bindImage(binding.imageEdit, bitmap)
    }

    @Suppress("UNUSED_PARAMETER")
    fun pickEmoji(view: View) {

        val gridView = GridView(this)
        val emojis = PhotoEditor.getEmojis(applicationContext)

        val builder = AlertDialog.Builder(this).apply {
            setView(gridView)
        }

        gridView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            emojis
        )

        gridView.numColumns = 5
        var dialog: AlertDialog? = null

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            photoEditor.addEmoji(emojis.get(position))
            isAddIcon = true
            dialog!!.dismiss()
        }

        dialog = builder.create()
        dialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSaveImageButtonClick(view: View) {
        val fileSave = createFileToSave()
        var contentValues: ContentValues? = null
        var imageUri: Uri? = null
        val check = handlingBitmap()
        var bitmap2: Bitmap = bitmap

        val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ROOT)

        if (!check) {
            bitmap2 = binding.imageEdit.drawToBitmap()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val resolver = contentResolver
                val date = System.currentTimeMillis()

                // TODO: get the mediaStore from the current uri, then saved the new file
                //      corresponding with the data from the mediaStore columns
                contentValues = ContentValues().apply {
                    put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileSave.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.DATE_ADDED, date)
                    put(MediaStore.MediaColumns.DATE_TAKEN, date)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                    put(MediaStore.MediaColumns.SIZE, bitmap2.byteCount)
                    put(MediaStore.MediaColumns.WIDTH, bitmap2.width)
                    put(MediaStore.MediaColumns.HEIGHT, bitmap2.height)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                imageUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                contentResolver.openOutputStream(imageUri!!, "w").use {
                    bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

            } else {
                FileOutputStream(fileSave).use { stream ->
                    bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            }
        } catch (e: IOException) {
            Toast.makeText(this, getString(R.string.image_saved_fail), Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues?.clear()
            contentValues?.put(MediaStore.Images.Media.IS_PENDING, 0)

            contentResolver.update(imageUri!!, contentValues, null, null)

            // Add exif data
            contentResolver.openFileDescriptor(imageUri, "rw")?.use {
                // set Exif attribute so MediaStore.Images.Media.DATE_TAKEN will be set
                ExifInterface(it.fileDescriptor)
                    .apply {
                        setAttribute(
                            ExifInterface.TAG_DATETIME_ORIGINAL,
                            exifDateFormatter.format(Date())
                        )
                        saveAttributes()
                    }
            }
        }
        SingleMediaScanner(this, fileSave)

        Toast.makeText(this, getString(R.string.image_saved), Toast.LENGTH_LONG).show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPickColorButtonClick(view: View) {
        MaterialDialog(this).show {
            title(R.string.pick_color)
            colorChooser(
                colors = resources.getIntArray(R.array.color_picker_dialog_values),
                subColors = context.allColorPaletteRes(),
                allowCustomArgb = true,
                showAlphaSelector = true
            ) { _, color ->
                // Use color integer
                curColorDraw = color
                binding.drawConfigEditor.pickColor.setBackgroundColor(curColorDraw)
                viewDraw.setNewColor(curColorDraw)
            }
            positiveButton(R.string.select)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun pickImageFromPhone(view: View) {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_image_intent_chooser_title)), PICK_IMAGE_INTENT)
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