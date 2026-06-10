package com.zeropointone.engine

/** Immutable render data for a single agent at one tick. */
data class AgentSnapshot(
    val id: Long,
    val level: Int,
    val x: Int,
    val y: Int,
    /** Current energy as a fraction of the agent's max energy, in [0, 1]. */
    val energyRatio: Float,
)

/** Per-tick population counts, used to plot the population-over-time chart. */
data class PopulationSample(val tick: Long, val counts: Map<Int, Int>)

/**
 * Immutable snapshot of the whole simulation at one tick, published by the [Engine]
 * via StateFlow and consumed by the Compose UI. Decouples rendering from the
 * mutable simulation state running on the background thread.
 */
data class SimulationSnapshot(
    val tick: Long,
    val agents: List<AgentSnapshot>,
    /** Population count per trophic level (1..5). */
    val populations: Map<Int, Int>,
    /** Recent population history (a trailing window) for the in-app chart. */
    val history: List<PopulationSample> = emptyList(),
) {
    companion object {
        val EMPTY = SimulationSnapshot(0L, emptyList(), emptyMap())
    }
}
