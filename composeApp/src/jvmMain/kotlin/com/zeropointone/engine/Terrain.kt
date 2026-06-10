package com.zeropointone.engine

import com.zeropointone.utils.Hexagon
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The passive spatial environment. Holds the world's pixel bounds and the fixed set of
 * hexagonal cells used to draw the grid. Each tick it also builds a fresh [SpatialIndex]
 * of the current agents for efficient neighbour lookups.
 *
 * @param pixelWidth  number of pixels on the x-axis
 * @param pixelHeight number of pixels on the y-axis
 */
class Terrain(val pixelWidth: Int, val pixelHeight: Int) {

    /** Fixed hex cells covering the world, used only for rendering the background grid. */
    val hexagons: List<Hexagon> = buildList {
        val horizontalSpacing = sqrt(3.0f) * Hexagon.SIZE
        val verticalSpacing = 1.5f * Hexagon.SIZE
        val cols = ceil(pixelWidth / horizontalSpacing).toInt()
        val rows = ceil(pixelHeight / verticalSpacing).toInt()
        for (r in 0..rows) {
            val qOffset = r / 2
            for (q in -qOffset until (cols - qOffset)) add(Hexagon(q, r))
        }
    }

    /** Build a spatial hash of [agents] for this tick. Any pixel position is accepted. */
    fun index(agents: List<Agent>): SpatialIndex {
        val cells = HashMap<Hexagon, MutableList<Agent>>()
        for (agent in agents) {
            cells.getOrPut(Hexagon.fromPosition(agent.position)) { ArrayList() }.add(agent)
        }
        return SpatialIndex(cells)
    }
}
