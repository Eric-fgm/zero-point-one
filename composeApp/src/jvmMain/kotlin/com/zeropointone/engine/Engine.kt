package com.zeropointone.engine

import com.zeropointone.enums.ConsumerLevel
import com.zeropointone.utils.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The discrete-time simulation scheduler. Owns the master list of agents and runs a single
 * tick loop on a background coroutine. Each tick: build a spatial index, let every agent act
 * (in shuffled order to avoid directional bias), then apply queued births and deaths
 * atomically. State is published to the UI as immutable [SimulationSnapshot]s via StateFlow.
 *
 * Replacing the prototype's one-coroutine-per-agent model with a central loop removes data
 * races on the shared world and makes the run deterministic for a given [SimulationConfig.seed].
 */
class Engine(val terrain: Terrain, private val config: SimulationConfig) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val rng = Random(config.seed)
    private val agents = ArrayList<Agent>()
    private var tick = 0L

    private val _snapshot = MutableStateFlow(SimulationSnapshot.EMPTY)
    val snapshot: StateFlow<SimulationSnapshot> = _snapshot.asStateFlow()

    /** Start the live simulation: seed populations and run the tick loop on a background coroutine. */
    fun run() {
        if (job != null) return
        initialize()
        job = scope.launch {
            val delayMillis = config.tickDelayMillis()
            while (isActive) {
                advance()
                delay(delayMillis)
            }
        }
    }

    fun destroy() {
        job?.cancel()
        job = null
        agents.clear()
    }

    /** Seed the initial populations and publish tick 0. Call once before [advance] for headless runs. */
    fun initialize() {
        if (agents.isEmpty()) spawnInitial()
        tick = 0L
        publish(0L)
    }

    /** Run exactly one tick and return the resulting snapshot. Usable headlessly (e.g. M4 batch runs). */
    fun advance(): SimulationSnapshot {
        tick++
        step()
        publish(tick)
        return _snapshot.value
    }

    private fun spawnInitial() {
        for (level in 1..5) {
            val count = config.initialPopulations[level] ?: 0
            val p = config.params(level)
            repeat(count) {
                val pos = Position.generateRandom(0..terrain.pixelWidth, 0..terrain.pixelHeight)
                agents += if (level == 1) {
                    Agent.Producer(pos, p.initialEnergy)
                } else {
                    Agent.Consumer(pos, ConsumerLevel.ofTrophicLevel(level)!!, p.initialEnergy)
                }
            }
        }
    }

    private fun step() {
        val index = terrain.index(agents)
        val populations = agents.groupingBy { it.level }.eachCount()
        val births = ArrayList<Agent>()
        val deaths = HashSet<Agent>()
        val ctx = StepContext(index, config, terrain, rng, populations, births, deaths)

        for (agent in agents.shuffled(rng)) agent.update(ctx)

        if (deaths.isNotEmpty()) agents.removeAll(deaths)
        if (births.isNotEmpty()) agents.addAll(births)
    }

    private fun publish(tick: Long) {
        _snapshot.value = SimulationSnapshot(
            tick = tick,
            agents = agents.map {
                AgentSnapshot(it.id, it.level, it.position.x, it.position.y, it.energyRatio(config))
            },
            populations = agents.groupingBy { it.level }.eachCount(),
        )
    }
}
