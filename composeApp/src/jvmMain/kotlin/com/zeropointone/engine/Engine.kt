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

    /** Full per-tick population history (kept for CSV export). */
    private val history = ArrayList<PopulationSample>()

    /** Live playback speed in [0, 1]. May be changed at any time while the loop is running. */
    @Volatile var speed: Float = config.speed

    /** When true the loop holds the current state without advancing. Toggled live by the UI. */
    @Volatile var paused: Boolean = false

    private val _snapshot = MutableStateFlow(SimulationSnapshot.EMPTY)
    val snapshot: StateFlow<SimulationSnapshot> = _snapshot.asStateFlow()

    /** Start the live simulation: seed populations and run the tick loop on a background coroutine. */
    fun run() {
        if (job != null) return
        initialize()
        job = scope.launch {
            while (isActive) {
                if (paused) {
                    delay(60L)
                    continue
                }
                advance()
                delay(SimulationConfig.tickDelayMillis(speed))
            }
        }
    }

    fun destroy() {
        job?.cancel()
        job = null
        agents.clear()
        history.clear()
    }

    /** Seed the initial populations and publish tick 0. Call once before [advance] for headless runs. */
    fun initialize() {
        if (agents.isEmpty()) spawnInitial()
        tick = 0L
        history.clear()
        publish(0L)
    }

    /** Full per-tick population history as CSV text (header: tick,L1..L5,total). */
    fun populationHistoryCsv(): String {
        val sb = StringBuilder("tick,L1,L2,L3,L4,L5,total\n")
        for (sample in history) {
            val counts = (1..5).map { sample.counts[it] ?: 0 }
            sb.append(sample.tick)
            counts.forEach { sb.append(',').append(it) }
            sb.append(',').append(counts.sum()).append('\n')
        }
        return sb.toString()
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
        val counts = agents.groupingBy { it.level }.eachCount()
        history.add(PopulationSample(tick, counts))
        val from = (history.size - CHART_WINDOW).coerceAtLeast(0)
        _snapshot.value = SimulationSnapshot(
            tick = tick,
            agents = agents.map {
                AgentSnapshot(it.id, it.level, it.position.x, it.position.y, it.energyRatio(config))
            },
            populations = counts,
            history = ArrayList(history.subList(from, history.size)),
        )
    }

    private companion object {
        /** Number of most-recent ticks shown in the in-app chart. */
        const val CHART_WINDOW = 600
    }
}
