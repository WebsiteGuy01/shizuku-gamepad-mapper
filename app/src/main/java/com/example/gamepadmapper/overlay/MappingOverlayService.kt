package com.example.gamepadmapper.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.MotionEvent
import com.example.gamepadmapper.engine.ShizukuInjector
import com.example.gamepadmapper.engine.ShizukuUserServiceConnection

class MappingOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var shizukuConnection: ShizukuUserServiceConnection? = null
    private var shizukuInjector: ShizukuInjector? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        if (rikka.shizuku.Shizuku.pingBinder()) {
            shizukuConnection = ShizukuUserServiceConnection().also { connection ->
                runCatching {
                    connection.bind()
                    shizukuInjector = ShizukuInjector(connection)
                }
            }
        }

        windowManager = getSystemService(WindowManager::class.java)
        overlayView = FrameLayout(this).apply {
            alpha = 0.02f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val windowType = if (android.os.Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            1,
            1,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager?.addView(overlayView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    fun dispatchMappedEvent(event: MotionEvent): Boolean =
        shizukuInjector?.inject(event) == true

    override fun onDestroy() {
        shizukuInjector?.destroy()
        shizukuInjector = null
        shizukuConnection = null
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
