package com.hcmus.clc18se.photos

import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.view.drawToBitmap
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.adapters.bindImage
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import com.hcmus.clc18se.photos.utils.DrawableImageView
import com.hcmus.clc18se.photos.utils.svg.SingleMediaScanner
import com.theartofdev.edmodo.cropper.CropImageView
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.SaveSettings
import kotlinx.coroutines.*
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.io.*
import java.nio.IntBuffer
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
    }

    protected val colorResource by lazy { (application as PhotosApplication).colorResource }

    private val binding by lazy { ActivityEditPhotoBinding.inflate(layoutInflater) }
    private var cur_item_id = 0
    private val bottomAppBarItemKey: String = "curItemId"

    private var uri: Uri? = null
    private var bitmap: Bitmap? = null
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

        colorResource.configColor(this)
        colorResource.configTheme()

        setContentView(binding.root)

        if (intent.hasExtra("uri")) {
            uri = intent.getParcelableExtra("uri")

            CoroutineScope(Dispatchers.IO).launch {
                bitmap = getBitMapFromUri(uri!!)
                var scale = 1
                val byteBitmap = bitmap!!.width * bitmap!!.height * 4
                while (byteBitmap / scale / scale > 6000000) {
                    scale++
                }
                bitmap = bitmap!!.copy(Bitmap.Config.ARGB_8888, true).scale(
                    bitmap!!.width / scale,
                    bitmap!!.height / scale,
                    false
                )
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
        val graybm = toGrayscale(newBmp)
        val invertbm = invertColors(graybm!!)
        val blurbm = blur(invertbm)

        val result = ColorDodgeBlend(graybm, blurbm!!)

        return result!!
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
            redSeekBar.progress = 0
            greenSeekBar.progress = 255
            blueSeekBar.progress = 0
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

    //link: https://stackoverflow.com/questions/9826273/photo-image-to-sketch-algorithm
    private fun colordodge(in1: Int, in2: Int): Int {
        val image = in2.toFloat()
        val mask = in1.toFloat()
        return (if (image == 255f) image else Math.min(
            255f,
            (mask.toLong() shl 8) / (255 - image)
        )).toInt()
    }

    //link: https://stackoverflow.com/questions/9826273/photo-image-to-sketch-algorithm
    fun ColorDodgeBlend(source: Bitmap, layer: Bitmap): Bitmap? {
        val base = source.copy(Bitmap.Config.ARGB_8888, true)
        val blend = layer.copy(Bitmap.Config.ARGB_8888, false)
        val buffBase: IntBuffer = IntBuffer.allocate(base.width * base.height)
        base.copyPixelsToBuffer(buffBase)
        buffBase.rewind()
        val buffBlend: IntBuffer = IntBuffer.allocate(blend.width * blend.height)
        blend.copyPixelsToBuffer(buffBlend)
        buffBlend.rewind()
        val buffOut: IntBuffer = IntBuffer.allocate(base.width * base.height)
        buffOut.rewind()
        while (buffOut.position() < buffOut.limit()) {
            val filterInt: Int = buffBlend.get()
            val srcInt: Int = buffBase.get()
            val redValueFilter = Color.red(filterInt)
            val greenValueFilter = Color.green(filterInt)
            val blueValueFilter = Color.blue(filterInt)
            val redValueSrc = Color.red(srcInt)
            val greenValueSrc = Color.green(srcInt)
            val blueValueSrc = Color.blue(srcInt)
            val redValueFinal: Int = colordodge(redValueFilter, redValueSrc)
            val greenValueFinal: Int = colordodge(greenValueFilter, greenValueSrc)
            val blueValueFinal: Int = colordodge(blueValueFilter, blueValueSrc)
            val pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal)
            buffOut.put(pixel)
        }
        buffOut.rewind()
        base.copyPixelsFromBuffer(buffOut)
        blend.recycle()
        return base
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

    private fun handlingBitmap(): Boolean {
        if (isCrop) {
            viewCrop.let {
                bitmap = it.croppedImage
                var scale = 1
                val byteBitmap = bitmap!!.width * bitmap!!.height * 4
                while (byteBitmap / scale / scale > 6000000) {
                    scale++
                }
                bitmap = bitmap!!.copy(Bitmap.Config.ARGB_8888, true).scale(
                    bitmap!!.width / scale,
                    bitmap!!.height / scale,
                    false
                )
                binding.imageEdit.colorFilter = null
                bindImage(binding.imageEdit, bitmap)
                isCrop = false
            }
            return true
        }
        if (isDraw) {
            viewDraw.let {
                bitmap = it.drawable.toBitmap(bitmap!!.width, bitmap!!.height, bitmap!!.config)
                bindImage(binding.imageEdit, bitmap)
                isDraw = false
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
                isAddIcon = false
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

                cur_item_id = item.itemId
                return@OnNavigationItemSelectedListener true
            }
            false
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
            binding.saveImage.visibility = View.VISIBLE
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
        viewDraw.setNewImage(alteredBitmap, bitmap!!, setColor(tempRed, tempGreen, tempBlue))
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
        binding.saveImage.visibility = View.INVISIBLE
    }

    fun pickEmoji(view: View) {
        val gridView = GridView(this)
        val emojis = PhotoEditor.getEmojis(applicationContext)
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setView(gridView)
        gridView.adapter = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_list_item_1,
            emojis as List<Any?>
        )
        gridView.numColumns = 5
        var dialog: android.app.AlertDialog? = null
        gridView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                photoEditor.addEmoji(emojis.get(position))
                isAddIcon = true
                dialog!!.dismiss()
            }
        dialog = builder.create()
        dialog.show()
    }

    fun onSaveImageButtonClick(view: View) {
        val fileSave = createFileToSave()
        var contentValues: ContentValues? = null
        var imageUri: Uri? = null
        val check = handlingBitmap()
        var bitmap2: Bitmap = bitmap!!
        val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        if (!check)
            bitmap2 = binding.imageEdit.drawToBitmap()
        try {
            var stream: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = baseContext.getContentResolver()
                val date = System.currentTimeMillis()
                contentValues = ContentValues().apply {
                    put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileSave.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.DATE_ADDED, date)
                    put(MediaStore.MediaColumns.DATE_TAKEN, date)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                    put(MediaStore.MediaColumns.SIZE, bitmap2!!.byteCount)
                    put(MediaStore.MediaColumns.WIDTH, bitmap2.width)
                    put(MediaStore.MediaColumns.HEIGHT, bitmap2.height)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                imageUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                baseContext.contentResolver.openOutputStream(imageUri!!, "w").use {
                    bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            } else {
                stream = FileOutputStream(fileSave)
                bitmap2!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to save Image", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues!!.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            baseContext.contentResolver.update(imageUri!!, contentValues, null, null)
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
        Toast.makeText(this, "Image Saved Successfully", Toast.LENGTH_LONG).show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPickColorButtonClick(view: View) {

        ColorPickerDialog()
            .withColor(Color.WHITE) // the default / initial color
            .withListener { dialog, color ->
                // a color has been picked; use it
                curColorDraw = color
                binding.drawConfigEditor.pickColor.setBackgroundColor(curColorDraw)
                viewDraw.setNewColor(curColorDraw)
            }
            .withTheme(R.style.ColorPickerDialog)
            .withPresets(*resources.getIntArray(R.array.color_choices))
            .withAlphaEnabled(true)
            .show(supportFragmentManager, "colorPicker")

//        val colorPicker = AmbilWarnaDialog(this, curColorDraw, object : OnAmbilWarnaListener {
//            override fun onCancel(dialog: AmbilWarnaDialog) {}
//            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
//
//            }
//        })
//        colorPicker.show()
    }

    fun pickImageFromPhone(view: View) {
        val intent = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_INTENT)
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