package com.zeropointone.engine

import androidx.compose.runtime.mutableStateListOf
import com.zeropointone.utils.Hexagon
import com.zeropointone.utils.Position
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Represents the physical world of the simulation.
 * Uses a spatial hash map of Hexagons to make neighbor lookups efficient.
 * @param pixelWidth The number of pixels on x-axis
 * @param pixelHeight The number of pixels on y-axis
 */
class Terrain(val pixelWidth: Int, val pixelHeight: Int) {
    private val grid: Map<Hexagon, MutableList<Agent>> = buildMap {
        val horizontalSpacing = sqrt(3.0f) * Hexagon.SIZE
        val verticalSpacing = 1.5f * Hexagon.SIZE

        val cols = ceil(pixelWidth / horizontalSpacing).toInt()
        val rows = ceil(pixelHeight / verticalSpacing).toInt()

        for (r in 0..rows) {
            val qOffset = (r / 2)
            for (q in -qOffset until (cols - qOffset)) {
                put(Hexagon(q, r), mutableStateListOf())
            }
        }
    }

    fun place(agent: Agent) {
        val agents = this.grid[Hexagon.fromPosition(agent.position)]
        agents?.add(agent)
    }

    fun remove(agent: Agent) = this.grid[Hexagon.fromPosition(agent.position)]?.remove(agent) ?: false

    fun move(agent: Agent.Consumer, destination: Position) {
        if (this.remove(agent)) {
            agent.position = destination
            this.place(agent)
        }
    }

    fun getHexagons() = this.grid.keys.toList()

    fun getAgents() = this.grid.values.flatten()
}