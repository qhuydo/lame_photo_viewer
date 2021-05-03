package com.hcmus.clc18se.photos.utils

import android.view.KeyEvent

interface OnDirectionKeyDown {
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
}