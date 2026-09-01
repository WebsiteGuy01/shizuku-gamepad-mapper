package com.example.gamepadmapper.engine

import kotlin.math.abs
import kotlin.math.sign

 data class StickVector(val x: Float, val y: Float)

class JoystickProcessor(
    private val deadZone: Float = 0.12f,
    private val sensitivity: Float = 1.0f
) {
    fun process(rawX: Float, rawY: Float): StickVector {
        return StickVector(normalize(rawX), normalize(rawY))
    }

    private fun normalize(value: Float): Float {
        val clamped = value.coerceIn(-1f, 1f)
        val magnitude = abs(clamped)
        if (magnitude <= deadZone) return 0f
        val remapped = ((magnitude - deadZone) / (1f - deadZone))
            .coerceIn(0f, 1f)
        return (remapped * sensitivity).coerceIn(0f, 1f) * sign(clamped)
    }
}
