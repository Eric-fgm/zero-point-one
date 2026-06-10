package com.zeropointone.engine

import com.zeropointone.enums.ConsumerLevel
import com.zeropointone.utils.Hexagon
import com.zeropointone.utils.Position
import com.zeropointone.utils.Vec2
import com.zeropointone.utils.distanceTo
import com.zeropointone.utils.moved
import com.zeropointone.utils.randomNearby
import com.zeropointone.utils.toVec
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
     * Levels 2-5: consumers. Each tick they pay a metabolic cost, then act in priority order:
     *  1. if predators are near, **flee** (steer away from all of them);
     *  2. else if prey is in reach, **eat** it — transferring the configured yield (the 10% rule);
     *  3. else **steer**: a blend of hunting (toward prey, optionally density-weighted) and herding
     *     (cohesion + separation with same-level peers), falling back to a random wander.
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
            val behavior = ctx.config.behavior
            val neighbours = ctx.index.within(position, p.perceptionRadius)

            val predators = neighbours.filter { it.level == level + 1 && ctx.isAlive(it) }
            if (predators.isNotEmpty()) {
                // Survival first: steer away from all nearby predators (closer ones weigh more).
                position = position.moved(avoidanceOf(predators), p.moveSpeed, width, height)
            } else {
                val prey = neighbours.filter { it.level == level - 1 && ctx.isAlive(it) }
                val nearestPrey = prey.minByOrNull { position.distanceTo(it.position) }

                if (nearestPrey != null && position.distanceTo(nearestPrey.position) <= CONSUME_DISTANCE) {
                    // Consumption event: gain the configured yield (the 10% rule) of the prey's energy, prey dies.
                    energy = min(p.maxEnergy, energy + ctx.config.energyTransferYield * nearestPrey.energy)
                    ctx.kill(nearestPrey)
                } else {
                    var desired = Vec2.ZERO
                    if (nearestPrey != null) {
                        val hunt = if (behavior.gradientForaging) attractionTo(prey) else directionTo(nearestPrey)
                        desired += hunt * HUNT_WEIGHT
                    }
                    if (behavior.herding) {
                        val peers = neighbours.filter { it !== this && it.level == level && ctx.isAlive(it) }
                        if (peers.isNotEmpty()) {
                            desired += cohesionOf(peers) * behavior.cohesionWeight
                            desired += separationOf(peers, behavior.separationRadius) * behavior.separationWeight
                        }
                    }
                    position = if (desired.length() < 1e-9) {
                        position.randomNearby(ctx.rng, p.moveSpeed, width, height) // wander
                    } else {
                        position.moved(desired, p.moveSpeed, width, height)
                    }
                }
            }

            if (energy >= p.reproductionThreshold) {
                energy -= p.reproductionCost
                val childPos = position.randomNearby(ctx.rng, p.moveSpeed * 2, width, height)
                ctx.spawn(Consumer(childPos, consumerLevel, p.reproductionCost))
            }
        }

        /** Unit vector pointing from this agent toward [target]. */
        private fun directionTo(target: Agent): Vec2 =
            (target.position.toVec() - position.toVec()).normalizedOrZero()

        /** Density-weighted pull toward a group (inverse-distance), approximating a prey-density gradient. */
        private fun attractionTo(targets: List<Agent>): Vec2 {
            var acc = Vec2.ZERO
            for (t in targets) {
                val d = position.distanceTo(t.position)
                if (d < 1e-9) continue
                acc += (t.position.toVec() - position.toVec()) * (1.0 / (d * d)) // magnitude ~ 1/d
            }
            return acc.normalizedOrZero()
        }

        /** Inverse-distance weighted push away from a group of threats. */
        private fun avoidanceOf(threats: List<Agent>): Vec2 {
            var acc = Vec2.ZERO
            for (t in threats) {
                val d = position.distanceTo(t.position)
                acc += if (d < 1e-9) Vec2(1.0, 0.0)
                else (position.toVec() - t.position.toVec()) * (1.0 / (d * d))
            }
            return acc.normalizedOrZero()
        }

        /** Unit vector toward the centroid of [peers] (flock cohesion). */
        private fun cohesionOf(peers: List<Agent>): Vec2 {
            var cx = 0.0
            var cy = 0.0
            for (peer in peers) {
                cx += peer.position.x
                cy += peer.position.y
            }
            val centroid = Vec2(cx / peers.size, cy / peers.size)
            return (centroid - position.toVec()).normalizedOrZero()
        }

        /** Push away from peers closer than [radius], so herds spread out instead of collapsing to a point. */
        private fun separationOf(peers: List<Agent>, radius: Double): Vec2 {
            var acc = Vec2.ZERO
            for (peer in peers) {
                val d = position.distanceTo(peer.position)
                if (d < radius) {
                    acc += if (d < 1e-9) Vec2(1.0, 0.0)
                    else (position.toVec() - peer.position.toVec()) * (1.0 / (d * d))
                }
            }
            return acc.normalizedOrZero()
        }
    }

    companion object {
        // The ten-percent law's yield now lives in SimulationConfig.energyTransferYield so it can be
        // varied as the headline experimental parameter (e.g. 5% / 10% / 20% / 30%).

        /** Pixel distance at which a predator is close enough to consume its prey. */
        const val CONSUME_DISTANCE = 14.0

        /** Relative weight of the hunt steering vector vs. herding cohesion/separation. */
        private const val HUNT_WEIGHT = 1.0

        private val counter = AtomicLong(0L)
        private fun nextId(): Long = counter.getAndIncrement()
    }
}
