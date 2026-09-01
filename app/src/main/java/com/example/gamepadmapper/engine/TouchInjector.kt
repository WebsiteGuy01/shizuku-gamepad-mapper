package com.example.gamepadmapper.engine

import android.view.MotionEvent

interface TouchInjector {
    fun inject(event: MotionEvent): Boolean
    fun destroy()
}
