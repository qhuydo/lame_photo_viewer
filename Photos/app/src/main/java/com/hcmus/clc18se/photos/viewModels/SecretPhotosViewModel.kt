package com.hcmus.clc18se.photos.viewModels

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.*
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.utils.getMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File


class SecretPhotosViewModel(application: Application) : AndroidViewModel(application) {

    private var _mediaItems = MutableLiveData<List<MediaItem>?>()
    val mediaItems: LiveData<List<MediaItem>?>
        get() = _mediaItems

    private var _isUnlocked = MutableLiveData(false)
    val isUnlocked: LiveData<Boolean>
        get() = _isUnlocked


    private var _reloadDataRequest = MutableLiveData(false)
    val reloadDataRequest: LiveData<Boolean>
        get() = _reloadDataRequest

    private var _preventLock = MutableLiveData(false)
    val preventLock: LiveData<Boolean>
        get() = _preventLock

    private fun loadSecretImages() = viewModelScope.launch {

        withContext(Dispatchers.IO) {
            val begin = System.currentTimeMillis()

            val cw = ContextWrapper(getApplication<Application>().applicationContext)
            val contentResolver = getApplication<Application>().contentResolver
            val mimeTypeMap = MimeTypeMap.getSingleton()

            val directory = cw.getDir("images", Context.MODE_PRIVATE)

            val list = mutableListOf<MediaItem>()
            val files = directory.listFiles()
            files?.let {
                files.forEach { file ->
                    file?.let {
                        list += mediaItemFromPrivateFile(file, contentResolver)
                    }
                }
            }

            _mediaItems.postValue(list)

            Timber.d("${System.currentTimeMillis() - begin} ms")
        }
    }

    private fun mediaItemFromPrivateFile(
            file: File,
            contentResolver: ContentResolver
    ): MediaItem {
        val nameWithoutExtension = file.nameWithoutExtension

        val id = nameWithoutExtension.substringAfterLast("_").toLong()
        val name = nameWithoutExtension.substringBeforeLast("_")
        val path = file.path

        val uri = Uri.fromFile(file)

        val mime = contentResolver.getMimeType(uri)

        return MediaItem(id, name, uri, null, mime, null, path)
    }

    fun unlock() {
        _isUnlocked.postValue(true)
        loadSecretImages()
    }

    fun lock() {
        if (preventLock.value != true) {
            _isUnlocked.postValue(false)
            _mediaItems.postValue(emptyList())
        }
    }

    fun requestReloadingData() {
        _reloadDataRequest.postValue(true)
    }

    fun doneRequestingLoadData() {
        _reloadDataRequest.value = false
    }

    fun preventLock() {
        _preventLock.value = true
    }
}


@Suppress("UNCHECKED_CAST")
class SecretViewModelFactory(
        private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecretPhotosViewModel::class.java)) {
            return SecretPhotosViewModel(application) as T
        }
        throw Exception()
    }
}