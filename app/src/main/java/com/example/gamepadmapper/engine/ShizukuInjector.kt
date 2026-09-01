package com.example.gamepadmapper.engine

import android.view.MotionEvent

/** Main-process adapter for the generated IMapperUserService proxy. */
class ShizukuInjector(
    private val connection: ShizukuUserServiceConnection
) : TouchInjector {
    override fun inject(event: MotionEvent): Boolean {
        val remote = connection.service ?: return false
        return runCatching {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> remote.injectDown(event.x, event.y)
                MotionEvent.ACTION_MOVE -> remote.injectMove(event.x, event.y)
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> remote.injectUp(event.x, event.y)
            }
            true
        }.getOrDefault(false)
    }

    override fun destroy() {
        runCatching { connection.service?.destroy() }
        connection.unbind()
    }
}
