package com.zeropointone.experiments

import com.zeropointone.engine.Engine
import com.zeropointone.engine.SimulationConfig
import com.zeropointone.engine.Terrain
import java.io.File

/**
 * Headless parameter-sweep runner — no GUI, no coroutines, no graphics. It drives the simulation
 * purely through [Engine.initialize] + [Engine.advance], so thousands of runs can be executed and
 * aggregated into CSV files for the report's results section.
 *
 * Run with:  ./gradlew :composeApp:sweep
 *   optional: -PsweepArgs="<ticks> <seeds>"   e.g. -PsweepArgs="1500 20"
 *
 * Output: ./results/exp1_yield.csv, exp2_behaviours.csv, exp3_metabolism.csv
 */

private const val WIDTH = 800
private const val HEIGHT = 600

private data class RunResult(
    /** Last tick at which each level still had at least one individual. */
    val lastAlive: Map<Int, Int>,
    /** Whether each level was still present at the final tick. */
    val survived: Map<Int, Boolean>,
)

/** Run one simulation to [ticks] and report per-level persistence/survival. */
private fun runOnce(config: SimulationConfig, ticks: Int): RunResult {
    val engine = Engine(Terrain(WIDTH, HEIGHT), config)
    engine.initialize()
    val lastAlive = HashMap<Int, Int>().apply { for (level in 1..5) put(level, 0) }
    var finalPop = engine.snapshot.value.populations
    repeat(ticks) {
        val snap = engine.advance()
        for (level in 1..5) if ((snap.populations[level] ?: 0) > 0) lastAlive[level] = snap.tick.toInt()
        finalPop = snap.populations
    }
    engine.destroy()
    return RunResult(lastAlive, (1..5).associateWith { (finalPop[it] ?: 0) > 0 })
}

private fun mean(values: List<Double>): Double = if (values.isEmpty()) 0.0 else values.sum() / values.size

/** Sample standard deviation (n-1). Returns 0 for fewer than two samples. */
private fun std(values: List<Double>, m: Double): Double {
    if (values.size < 2) return 0.0
    return Math.sqrt(values.sumOf { (it - m) * (it - m) } / (values.size - 1))
}

private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

/** Aggregated metrics across seeds: survival fraction, and persistence mean ± std (per level). */
private data class Aggregated(
    val survival: Map<Int, Double>,
    val persistMean: Map<Int, Double>,
    val persistStd: Map<Int, Double>,
)

/** Aggregate runs into per-level survival fraction and persistence mean ± std over the seeds. */
private fun aggregate(results: List<RunResult>): Aggregated {
    val survival = (1..5).associateWith { lvl -> mean(results.map { if (it.survived[lvl] == true) 1.0 else 0.0 }) }
    val persistVals = (1..5).associateWith { lvl -> results.map { it.lastAlive[lvl]!!.toDouble() } }
    val persistMean = persistVals.mapValues { mean(it.value) }
    val persistStd = persistVals.mapValues { std(it.value, persistMean.getValue(it.key)) }
    return Aggregated(survival, persistMean, persistStd)
}

fun main(args: Array<String>) {
    val ticks = args.getOrNull(0)?.toIntOrNull() ?: 1200
    val seedCount = args.getOrNull(1)?.toIntOrNull() ?: 12
    val seeds = (1L..seedCount.toLong()).toList()
    val outDir = File("results").apply { mkdirs() }
    val startedAt = System.currentTimeMillis()
    println("== Food-chain parameter sweep ==  world=${WIDTH}x$HEIGHT  ticks=$ticks  seeds=$seedCount")

    // Experiment 1 — survival vs energy-transfer yield (the headline experiment for the 10% rule).
    println("\n[exp1] survival vs energy-transfer yield")
    val yields = listOf(0.05, 0.08, 0.10, 0.15, 0.20, 0.25, 0.30)
    buildString {
        append("yield,L1_surv,L2_surv,L3_surv,L4_surv,L5_surv,L1_persist,L2_persist,L3_persist,L4_persist,L5_persist,L1_persist_std,L2_persist_std,L3_persist_std,L4_persist_std,L5_persist_std\n")
        for (y in yields) {
            val results = seeds.map { seed ->
                runOnce(SimulationConfig.default(WIDTH, HEIGHT, seed = seed, energyTransferYield = y), ticks)
            }
            val agg = aggregate(results)
            append(y)
            for (l in 1..5) append(',').append(round2(agg.survival[l]!!))
            for (l in 1..5) append(',').append(round2(agg.persistMean[l]!!))
            for (l in 1..5) append(',').append(round2(agg.persistStd[l]!!))
            append('\n')
            println("  yield=${"%.2f".format(y)}  L5 persist=${round2(agg.persistMean[5]!!)}±${round2(agg.persistStd[5]!!)}")
        }
    }.let { File(outDir, "exp1_yield.csv").writeText(it) }

    // Experiment 2 — effect of the optional behaviours (herding × gradient foraging).
    println("\n[exp2] survival vs behaviours (herding × gradient)")
    buildString {
        append("herding,gradient,L2_surv,L3_surv,L4_surv,L5_surv,L2_persist,L3_persist,L4_persist,L5_persist,L2_persist_std,L3_persist_std,L4_persist_std,L5_persist_std\n")
        for (herding in listOf(false, true)) for (gradient in listOf(false, true)) {
            val results = seeds.map { seed ->
                runOnce(SimulationConfig.default(WIDTH, HEIGHT, seed = seed, herding = herding, gradientForaging = gradient), ticks)
            }
            val agg = aggregate(results)
            append("$herding,$gradient")
            for (l in 2..5) append(',').append(round2(agg.survival[l]!!))
            for (l in 2..5) append(',').append(round2(agg.persistMean[l]!!))
            for (l in 2..5) append(',').append(round2(agg.persistStd[l]!!))
            append('\n')
            println("  herding=$herding gradient=$gradient  L4 persist=${round2(agg.persistMean[4]!!)}±${round2(agg.persistStd[4]!!)}")
        }
    }.let { File(outDir, "exp2_behaviours.csv").writeText(it) }

    // Experiment 3 — effect of a global metabolism multiplier on the upper levels.
    println("\n[exp3] survival vs metabolism multiplier")
    val metabolisms = listOf(0.6, 0.8, 1.0, 1.2, 1.4)
    buildString {
        append("metabolism,L2_surv,L3_surv,L4_surv,L5_surv,L2_persist,L3_persist,L4_persist,L5_persist,L2_persist_std,L3_persist_std,L4_persist_std,L5_persist_std\n")
        for (m in metabolisms) {
            val results = seeds.map { seed ->
                val base = SimulationConfig.default(WIDTH, HEIGHT, seed = seed)
                val scaled = base.copy(
                    params = base.params.mapValues { (lvl, p) ->
                        if (lvl == 1) p else p.copy(metabolicRate = p.metabolicRate * m)
                    },
                )
                runOnce(scaled, ticks)
            }
            val agg = aggregate(results)
            append(m)
            for (l in 2..5) append(',').append(round2(agg.survival[l]!!))
            for (l in 2..5) append(',').append(round2(agg.persistMean[l]!!))
            for (l in 2..5) append(',').append(round2(agg.persistStd[l]!!))
            append('\n')
            println("  metabolism=$m  L5 persist=${round2(agg.persistMean[5]!!)}±${round2(agg.persistStd[5]!!)}")
        }
    }.let { File(outDir, "exp3_metabolism.csv").writeText(it) }

    val secs = (System.currentTimeMillis() - startedAt) / 1000.0
    println("\nDone in ${"%.1f".format(secs)}s. CSVs written to ${outDir.absolutePath}")
}
