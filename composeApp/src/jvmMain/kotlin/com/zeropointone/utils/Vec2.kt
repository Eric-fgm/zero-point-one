package com.zeropointone.utils

import kotlin.math.hypot
import kotlin.math.roundToInt

/** A 2D vector in pixel space, used for blending agent steering behaviours. */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Vec2(x * scalar, y * scalar)

    fun length(): Double = hypot(x, y)

    fun normalizedOrZero(): Vec2 {
        val len = length()
        return if (len < 1e-9) ZERO else Vec2(x / len, y / len)
    }

    companion object {
        val ZERO = Vec2(0.0, 0.0)
    }
}

fun Position.toVec(): Vec2 = Vec2(x.toDouble(), y.toDouble())

/** Move along [direction] by [speed] pixels (the direction is normalised first), clamped to bounds. */
fun Position.moved(direction: Vec2, speed: Double, width: Int, height: Int): Position {
    val unit = direction.normalizedOrZero()
    if (unit == Vec2.ZERO) return this
    return Position(
        (x + unit.x * speed).roundToInt().coerceIn(0, width),
        (y + unit.y * speed).roundToInt().coerceIn(0, height),
    )
}
