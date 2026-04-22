package com.zeropointone.utils

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Hexagon(val q: Int, val r: Int) {
    companion object {
        const val SIZE = 20f

        fun toPosition(hexagon: Hexagon): Position {
            val x = this.SIZE * (sqrt(3.0) * hexagon.q + sqrt(3.0)/2.0 * hexagon.r)
            val y = this.SIZE * (3.0/2.0 * hexagon.r)
            return Position(x.roundToInt(), y.roundToInt())
        }

        /**
         * Converts screen pixels to the nearest Hexagon coordinate.
         * @param position The x,y coordinate (e.g., Wolf's position)
         */
        fun fromPosition(position: Position): Hexagon {
            val q = (sqrt(3.0f) / 3 * position.x - 1.0f / 3 * position.y) / this.SIZE
            val r = (2.0f / 3 * position.y) / this.SIZE

            return roundToHex(q, r)
        }

        private fun roundToHex(q: Float, r: Float): Hexagon {
            var rq = q.roundToInt()
            var rr = r.roundToInt()
            val rs = (-q - r).roundToInt()

            val qDiff = abs(rq - q)
            val rDiff = abs(rr - r)
            val sDiff = abs(rs - (-q - r))

            if (qDiff > rDiff && qDiff > sDiff) {
                rq = -rr - rs
            } else if (rDiff > sDiff) {
                rr = -rq - rs
            }

            return Hexagon(rq, rr)
        }
    }
}