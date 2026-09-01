package com.example.gamepadmapper.engine

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.example.gamepadmapper.IMapperUserService
import com.example.gamepadmapper.remote.MapperUserService
import rikka.shizuku.Shizuku

class ShizukuUserServiceConnection : ServiceConnection {
    @Volatile
    var service: IMapperUserService? = null
        private set

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service = binder?.let(IMapperUserService.Stub::asInterface)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }

    fun bind() {
        check(Shizuku.pingBinder()) { "Shizuku binder is unavailable" }
        val args = Shizuku.UserServiceArgs(
            ComponentName(
                "com.example.gamepadmapper",
                MapperUserService::class.java.name
            )
        )
            .daemon(false)
            .processNameSuffix("mapper")
            .debuggable(false)
            .version(1)
            .tag("gamepad-mapper")
        Shizuku.bindUserService(args, this)
    }

    fun unbind() {
        runCatching { Shizuku.unbindUserService(createArgs(), this, true) }
        service = null
    }

    private fun createArgs(): Shizuku.UserServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            "com.example.gamepadmapper",
            MapperUserService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("mapper")
        .debuggable(false)
        .version(1)
        .tag("gamepad-mapper")
}
