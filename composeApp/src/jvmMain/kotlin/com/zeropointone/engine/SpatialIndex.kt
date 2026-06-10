package com.zeropointone.engine

import com.zeropointone.utils.Hexagon
import com.zeropointone.utils.Position
import com.zeropointone.utils.distanceTo
import kotlin.math.ceil

/**
 * A read-only spatial hash of agents, rebuilt fresh each tick by [Terrain.index].
 * Bins agents into hexagonal cells so perception queries only scan nearby cells
 * instead of the whole population.
 */
class SpatialIndex(private val cells: Map<Hexagon, List<Agent>>) {

    /** All agents within [radius] pixels of [origin] (includes the querying agent itself). */
    fun within(origin: Position, radius: Double): List<Agent> {
        if (radius <= 0.0) return emptyList()
        val center = Hexagon.fromPosition(origin)
        // 1.5 * SIZE is the vertical hex spacing — a safe lower bound on per-ring pixel coverage.
        val ring = ceil(radius / (1.5 * Hexagon.SIZE)).toInt() + 1
        val result = ArrayList<Agent>()
        for (dq in -ring..ring) {
            for (dr in -ring..ring) {
                val list = cells[Hexagon(center.q + dq, center.r + dr)] ?: continue
                for (agent in list) {
                    if (origin.distanceTo(agent.position) <= radius) result.add(agent)
                }
            }
        }
        return result
    }
}
