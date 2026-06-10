package com.zeropointone.utils

import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Screen-pixel position of an element.
 * @param x The x coordinate
 * @param y The y coordinate
 */
data class Position(val x: Int, val y: Int) {
    companion object {
        /** Generates a random position within the given pixel ranges. */
        fun generateRandom(xRange: IntRange, yRange: IntRange): Position =
            Position(xRange.random(), yRange.random())
    }
}

/** Euclidean pixel distance to another position. */
fun Position.distanceTo(other: Position): Double =
    hypot((x - other.x).toDouble(), (y - other.y).toDouble())

/** One movement step of [speed] pixels towards [target], clamped to the world bounds. */
fun Position.stepTowards(target: Position, speed: Double, width: Int, height: Int): Position {
    val dx = (target.x - x).toDouble()
    val dy = (target.y - y).toDouble()
    val d = hypot(dx, dy)
    if (d < 1e-9) return this
    return Position(
        (x + dx / d * speed).roundToInt().coerceIn(0, width),
        (y + dy / d * speed).roundToInt().coerceIn(0, height),
    )
}

/** One movement step of [speed] pixels directly away from [threat], clamped to the world bounds. */
fun Position.stepAwayFrom(threat: Position, speed: Double, width: Int, height: Int): Position {
    val dx = (x - threat.x).toDouble()
    val dy = (y - threat.y).toDouble()
    val d = hypot(dx, dy)
    if (d < 1e-9) return this
    return Position(
        (x + dx / d * speed).roundToInt().coerceIn(0, width),
        (y + dy / d * speed).roundToInt().coerceIn(0, height),
    )
}

/** A random position within +/- [spread] pixels of this one (used for wandering and offspring placement). */
fun Position.randomNearby(rng: Random, spread: Double, width: Int, height: Int): Position {
    if (spread <= 0.0) return this
    return Position(
        (x + rng.nextDouble(-spread, spread)).roundToInt().coerceIn(0, width),
        (y + rng.nextDouble(-spread, spread)).roundToInt().coerceIn(0, height),
    )
}
