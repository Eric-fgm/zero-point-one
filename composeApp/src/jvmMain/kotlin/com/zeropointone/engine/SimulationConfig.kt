package com.zeropointone.engine

/**
 * Per-trophic-level biological parameters. The 10% energy-transfer yield itself is a
 * fixed thermodynamic law and lives in [Agent.ENERGY_TRANSFER_YIELD], not here.
 *
 * [regrowRate] and [spawnChance] are only used by producers (level 1).
 */
data class SpeciesParams(
    val maxEnergy: Double,
    val initialEnergy: Double,
    /** Energy lost per tick to baseline metabolism (0 for producers/autotrophs). */
    val metabolicRate: Double,
    /** Energy at/above which the agent reproduces. */
    val reproductionThreshold: Double,
    /** Energy paid by the parent on reproduction (becomes the child's starting energy). */
    val reproductionCost: Double,
    /** Pixel radius scanned for prey and predators. */
    val perceptionRadius: Double,
    /** Pixels moved per tick. */
    val moveSpeed: Double,
    /** Producers only: energy regained per tick (photosynthesis). */
    val regrowRate: Double = 0.0,
    /** Producers only: per-tick probability of spawning when above the reproduction threshold. */
    val spawnChance: Double = 0.0,
)

/**
 * Optional consumer steering behaviours, layered on top of the basic flee/hunt rules.
 * Both are toggleable so experiments can measure their effect (e.g. does herding raise the
 * survival probability of the apex predator?).
 */
data class BehaviorConfig(
    /** Prey/peers form herds: cohesion toward same-level neighbours, with separation to avoid stacking. */
    val herding: Boolean = true,
    /** Predators steer toward prey *density* (inverse-distance weighted) rather than the single nearest. */
    val gradientForaging: Boolean = true,
    val cohesionWeight: Double = 0.6,
    val separationWeight: Double = 0.9,
    val separationRadius: Double = 18.0,
)

/**
 * Full configuration for one simulation run. Centralising every tunable here keeps the
 * ecological logic decoupled from hard-coded numbers and makes parameter sweeps (M4) easy.
 */
data class SimulationConfig(
    val params: Map<Int, SpeciesParams>,
    val initialPopulations: Map<Int, Int>,
    val producerCarryingCapacity: Int,
    /** Fraction of prey energy a predator gains on consumption. The "10% rule"; default 0.10. */
    val energyTransferYield: Double,
    val seed: Long,
    val speed: Float,
    val behavior: BehaviorConfig = BehaviorConfig(),
) {
    fun params(level: Int): SpeciesParams =
        params[level] ?: error("No SpeciesParams configured for trophic level $level")

    companion object {
        /** Maps a speed slider value (0..1) to a per-tick delay: slow (200 ms) .. fast (20 ms). */
        fun tickDelayMillis(speed: Float): Long = ((1f - speed.coerceIn(0f, 1f)) * 180f + 20f).toLong()

        /**
         * Reasonable default parameters. These are a starting point, not tuned for any
         * particular outcome: under the hard 10% rule levels 4-5 are intentionally fragile.
         */
        fun default(
            width: Int,
            height: Int,
            speed: Float = 0f,
            seed: Long = 42L,
            energyTransferYield: Double = 0.10,
            herding: Boolean = true,
            gradientForaging: Boolean = true,
        ): SimulationConfig {
            val area = width.toLong() * height.toLong()
            val carryingCapacity = (area / 2500L).toInt().coerceIn(60, 500)
            return SimulationConfig(
                params = mapOf(
                    1 to SpeciesParams(
                        maxEnergy = 200.0, initialEnergy = 100.0, metabolicRate = 0.0,
                        reproductionThreshold = 140.0, reproductionCost = 70.0,
                        perceptionRadius = 0.0, moveSpeed = 0.0,
                        regrowRate = 3.0, spawnChance = 0.05,
                    ),
                    2 to SpeciesParams(
                        maxEnergy = 150.0, initialEnergy = 80.0, metabolicRate = 0.4,
                        reproductionThreshold = 110.0, reproductionCost = 55.0,
                        perceptionRadius = 80.0, moveSpeed = 6.0,
                    ),
                    3 to SpeciesParams(
                        maxEnergy = 300.0, initialEnergy = 150.0, metabolicRate = 0.5,
                        reproductionThreshold = 220.0, reproductionCost = 110.0,
                        perceptionRadius = 100.0, moveSpeed = 7.0,
                    ),
                    4 to SpeciesParams(
                        maxEnergy = 500.0, initialEnergy = 250.0, metabolicRate = 0.6,
                        reproductionThreshold = 380.0, reproductionCost = 190.0,
                        perceptionRadius = 120.0, moveSpeed = 8.0,
                    ),
                    5 to SpeciesParams(
                        maxEnergy = 800.0, initialEnergy = 400.0, metabolicRate = 0.7,
                        reproductionThreshold = 650.0, reproductionCost = 320.0,
                        perceptionRadius = 140.0, moveSpeed = 9.0,
                    ),
                ),
                initialPopulations = mapOf(1 to 80, 2 to 45, 3 to 20, 4 to 9, 5 to 4),
                producerCarryingCapacity = carryingCapacity,
                energyTransferYield = energyTransferYield,
                seed = seed,
                speed = speed,
                behavior = BehaviorConfig(herding = herding, gradientForaging = gradientForaging),
            )
        }
    }
}
