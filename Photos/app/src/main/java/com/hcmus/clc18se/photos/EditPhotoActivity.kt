package com.hcmus.clc18se.photos

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class EditPhotoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditPhotoBinding.inflate(layoutInflater) }
    private var cur_item_id = 0
    private val bottomAppBarItemKey: String = "curItemId"
    private var BitmapImage: Bitmap? = null
    private var imageView: ImageView? = null
    private var uri: Uri? = null
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (intent.hasExtra("uri")){
            uri = intent.getParcelableExtra("uri")
            bitmap = getBitMapFromUri()
            binding.imageEdit.setImageURI(uri)
            //binding.imageEdit.setImageBitmap(bitmap)
        }

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
                val contrast = 10 / 10F
                binding.imageEdit.setColorFilter(setBrightness(brightness))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
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

    private fun getBitMapFromUri():Bitmap{
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri);
        return bitmap
    }

    private fun createFileToSave(): File {
        val timeStamp = SimpleDateFormat("yyyyddMM_HHmmss").format(Date())
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(file.path + timeStamp)
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (item.itemId in listOf(R.id.bright,
                        R.id.filter,
                        R.id.add_icon,
                        R.id.crop,
                        R.id.change_color)) {
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
            R.id.crop -> binding.cropEditor.visibility = View.VISIBLE
            R.id.change_color -> binding.colorEditor.visibility = View.VISIBLE
            R.id.draw -> binding.drawEditor.visibility = View.VISIBLE
        }
    }

    fun onDrawButtonClick(view: View){
        setBarVisibility(R.id.draw)
    }

    fun onSaveImageButtonClick(view: View){

    }

}