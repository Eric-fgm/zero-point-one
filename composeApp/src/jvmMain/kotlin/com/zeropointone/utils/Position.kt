package com.zeropointone.utils

import kotlin.random.Random

/**
 * Includes screen pixels position of an element.
 * @param x The x coordinate
 * @param y The y coordinate
 */
data class Position(val x: Int, val y: Int) {
    companion object {
        /**
         * Generates random position.
         */
        fun generateRandom(xRange: IntRange, yRange: IntRange): Position {
          return Position(xRange.random(), yRange.random())
        }
    }
}