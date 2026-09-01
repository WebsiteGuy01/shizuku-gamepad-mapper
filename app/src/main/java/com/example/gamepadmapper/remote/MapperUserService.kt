package com.example.gamepadmapper.remote

import android.os.SystemClock
import android.view.InputEvent
import android.view.MotionEvent
import com.example.gamepadmapper.IMapperUserService

/**
 * Entry point launched by Shizuku as a User Service. This class is not an
 * Android Service component; Shizuku instantiates it in the elevated process.
 */
class MapperUserService : IMapperUserService.Stub() {
    companion object {
        const val LEGACY_DESTROY_TRANSACTION = 16_777_115
        private const val AIDL_DESCRIPTOR = "com.example.gamepadmapper.IMapperUserService"
    }

    override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int): Boolean {
        if (code == LEGACY_DESTROY_TRANSACTION) {
            data.enforceInterface(AIDL_DESCRIPTOR)
            destroy()
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }

    private val inputManager: Any? by lazy {
        runCatching {
            val inputManagerClass = Class.forName("android.hardware.input.InputManager")
            inputManagerClass.getDeclaredMethod("getInstance").apply {
                isAccessible = true
            }.invoke(null)
        }.getOrNull()
    }

    private var activePointerDown = false
    private var downTime = 0L

    override fun injectDown(x: Float, y: Float) {
        downTime = SystemClock.uptimeMillis()
        activePointerDown = true
        dispatchMotion(MotionEvent.ACTION_DOWN, x, y)
    }

    override fun injectMove(x: Float, y: Float) {
        if (!activePointerDown) return
        dispatchMotion(MotionEvent.ACTION_MOVE, x, y)
    }

    override fun injectUp(x: Float, y: Float) {
        if (!activePointerDown) return
        dispatchMotion(MotionEvent.ACTION_UP, x, y)
        activePointerDown = false
    }

    override fun destroy() {
        runCatching {
            if (activePointerDown) {
                dispatchMotion(MotionEvent.ACTION_CANCEL, 0f, 0f)
            }
        }
        activePointerDown = false
        inputManager
        System.exit(0)
    }

    private fun dispatchMotion(action: Int, x: Float, y: Float) {
        val manager = inputManager ?: return
        val eventTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        try {
            val method = manager.javaClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType!!
            )
            method.isAccessible = true
            method.invoke(manager, event, 0 /* INJECT_INPUT_EVENT_MODE_ASYNC */)
        } finally {
            event.recycle()
        }
    }
}
