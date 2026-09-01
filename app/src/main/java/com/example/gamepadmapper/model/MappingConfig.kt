package com.example.gamepadmapper.model

enum class InjectionEngine {
    TOUCH,
    SHIZUKU
}

data class MappingConfig(
    val engine: InjectionEngine = InjectionEngine.TOUCH,
    val overlayEnabled: Boolean = true,
    val deadZone: Float = 0.12f,
    val sensitivity: Float = 1.0f
)
