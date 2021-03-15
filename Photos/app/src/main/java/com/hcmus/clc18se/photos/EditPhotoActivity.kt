package com.hcmus.clc18se.photos

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import com.davemorrissey.labs.subscaleview.ImageSource
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.slider.Slider
import com.hcmus.clc18se.photos.databinding.ActivityEditPhotoBinding
import com.hcmus.clc18se.photos.fragments.PhotoViewPagerFragment
import timber.log.Timber
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
            binding.imageEdit.setImageURI(uri)
        }

        val bottomNavigation = binding.bottomEdit
        bottomNavigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        setBarVisibility(R.id.bright)
        savedInstanceState?.let {
            cur_item_id = it.getInt(bottomAppBarItemKey)
            setBarVisibility(cur_item_id)
        }
        val brightSlider = binding.brightEditor.findViewById<Slider>(R.id.slider_bright)
        brightSlider.addOnChangeListener { slider, oldValue:Float, fromUser ->
            val value = oldValue.toInt()
            for (x in 1..bitmap!!.height)
                for (y in 1..bitmap!!.width){
                    if (value > 0) {
                        val pixel = bitmap!!.getPixel(x, y)
                        val alpha = Color.alpha(pixel)
                        val red = if (Color.red(pixel) + value > 255) 255 else Color.red(pixel) + value
                        val green = if (Color.green(pixel) + value > 255) 255 else Color.green(pixel) + value
                        val blue = if (Color.blue(pixel) + value > 255) 255 else Color.blue(pixel) + value
                        bitmap!!.setPixel(x, y, Color.argb(alpha, red, green, blue))
                    }
                }
        }
    }

    private fun getBitMapFromUri():Bitmap{
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri);
        return bitmap
    }

    private fun createFileToSave(): File {
        val timeStamp = SimpleDateFormat("yyyyddMM_HHmmss").format(Date())
        var file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
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
        outState.putInt(bottomAppBarItemKey,cur_item_id)
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