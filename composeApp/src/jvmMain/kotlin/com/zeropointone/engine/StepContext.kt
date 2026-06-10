package com.zeropointone.engine

import kotlin.random.Random

/**
 * Everything an agent needs to act during a single tick. Births and deaths are queued
 * here and applied by the [Engine] only after every agent has acted, so the population
 * a given agent perceives stays stable for the whole tick.
 */
class StepContext(
    val index: SpatialIndex,
    val config: SimulationConfig,
    val terrain: Terrain,
    val rng: Random,
    private val populations: Map<Int, Int>,
    private val births: MutableList<Agent>,
    private val deaths: MutableSet<Agent>,
) {
    /** Queue a newborn agent to be added at the end of the tick. */
    fun spawn(agent: Agent) { births.add(agent) }

    /** Queue an agent for removal (starvation or being eaten) at the end of the tick. */
    fun kill(agent: Agent) { deaths.add(agent) }

    /** Whether an agent has not already been killed earlier this tick (prevents double-eating). */
    fun isAlive(agent: Agent): Boolean = agent !in deaths

    /** Population of a trophic level as counted at the start of the tick. */
    fun populationOf(level: Int): Int = populations[level] ?: 0
}
