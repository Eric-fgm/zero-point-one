package com.zeropointone.engine

import com.zeropointone.enums.ConsumerLevel
import com.zeropointone.utils.Hexagon
import com.zeropointone.utils.Position
import com.zeropointone.utils.distanceTo
import com.zeropointone.utils.randomNearby
import com.zeropointone.utils.stepAwayFrom
import com.zeropointone.utils.stepTowards
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * An autonomous organism in the food chain. Every agent owns its energy and decides its
 * own behaviour each tick from purely local perception — the [Engine] only schedules them.
 */
sealed class Agent(var position: Position, var energy: Double) {
    val id: Long = nextId()

    /** Trophic level, 1 (producer) .. 5 (apex predator). */
    abstract val level: Int

    /** Advance this agent by one tick, queuing any births/deaths in [ctx]. */
    abstract fun update(ctx: StepContext)

    fun energyRatio(config: SimulationConfig): Float =
        (energy / config.params(level).maxEnergy).coerceIn(0.0, 1.0).toFloat()

    /** Level 1: autotroph. Photosynthesises and spreads up to a carrying capacity; never hunts. */
    class Producer(position: Position, energy: Double) : Agent(position, energy) {
        override val level: Int = 1

        override fun update(ctx: StepContext) {
            val p = ctx.config.params(level)
            energy = min(p.maxEnergy, energy + p.regrowRate)

            val belowCapacity = ctx.populationOf(level) < ctx.config.producerCarryingCapacity
            if (belowCapacity && energy >= p.reproductionThreshold && ctx.rng.nextDouble() < p.spawnChance) {
                energy -= p.reproductionCost
                val spread = Hexagon.SIZE.toDouble() * 1.5
                val childPos = position.randomNearby(ctx.rng, spread, ctx.terrain.pixelWidth, ctx.terrain.pixelHeight)
                ctx.spawn(Producer(childPos, p.initialEnergy))
            }
        }
    }

    /**
     * Levels 2-5: consumers. Each tick they pay a metabolic cost, then (in priority order)
     * flee the nearest predator, hunt/eat the nearest prey, or wander. Eating transfers
     * exactly [ENERGY_TRANSFER_YIELD] of the prey's energy — the 10% rule.
     */
    class Consumer(position: Position, val consumerLevel: ConsumerLevel, energy: Double) : Agent(position, energy) {
        override val level: Int = consumerLevel.trophicLevel

        override fun update(ctx: StepContext) {
            if (!ctx.isAlive(this)) return // already eaten earlier this tick
            val p = ctx.config.params(level)

            energy -= p.metabolicRate
            if (energy <= 0.0) {
                ctx.kill(this) // starvation
                return
            }

            val width = ctx.terrain.pixelWidth
            val height = ctx.terrain.pixelHeight
            val neighbours = ctx.index.within(position, p.perceptionRadius)

            val predator = neighbours
                .filter { it.level == level + 1 && ctx.isAlive(it) }
                .minByOrNull { position.distanceTo(it.position) }

            if (predator != null) {
                // Survival takes priority over feeding: evade.
                position = position.stepAwayFrom(predator.position, p.moveSpeed, width, height)
            } else {
                val prey = neighbours
                    .filter { it.level == level - 1 && ctx.isAlive(it) }
                    .minByOrNull { position.distanceTo(it.position) }

                when {
                    prey == null ->
                        position = position.randomNearby(ctx.rng, p.moveSpeed, width, height)

                    position.distanceTo(prey.position) <= CONSUME_DISTANCE -> {
                        // Consumption event: gain the configured yield (the 10% rule) of the prey's energy, prey dies.
                        energy = min(p.maxEnergy, energy + ctx.config.energyTransferYield * prey.energy)
                        ctx.kill(prey)
                    }

                    else ->
                        position = position.stepTowards(prey.position, p.moveSpeed, width, height)
                }
            }

            if (energy >= p.reproductionThreshold) {
                energy -= p.reproductionCost
                val childPos = position.randomNearby(ctx.rng, p.moveSpeed * 2, width, height)
                ctx.spawn(Consumer(childPos, consumerLevel, p.reproductionCost))
            }
        }
    }

    companion object {
        // The ten-percent law's yield now lives in SimulationConfig.energyTransferYield so it can be
        // varied as the headline experimental parameter (e.g. 5% / 10% / 20% / 30%).

        /** Pixel distance at which a predator is close enough to consume its prey. */
        const val CONSUME_DISTANCE = 14.0

        private val counter = AtomicLong(0L)
        private fun nextId(): Long = counter.getAndIncrement()
    }
}
