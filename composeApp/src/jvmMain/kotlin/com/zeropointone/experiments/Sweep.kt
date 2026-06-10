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
private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

/** Aggregate runs into per-level survival fraction and mean persistence tick. */
private fun aggregate(results: List<RunResult>): Pair<Map<Int, Double>, Map<Int, Double>> {
    val survival = (1..5).associateWith { lvl -> mean(results.map { if (it.survived[lvl] == true) 1.0 else 0.0 }) }
    val persistence = (1..5).associateWith { lvl -> mean(results.map { it.lastAlive[lvl]!!.toDouble() }) }
    return survival to persistence
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
        append("yield,L1_surv,L2_surv,L3_surv,L4_surv,L5_surv,L1_persist,L2_persist,L3_persist,L4_persist,L5_persist\n")
        for (y in yields) {
            val results = seeds.map { seed ->
                runOnce(SimulationConfig.default(WIDTH, HEIGHT, seed = seed, energyTransferYield = y), ticks)
            }
            val (surv, persist) = aggregate(results)
            append(y)
            for (l in 1..5) append(',').append(round2(surv[l]!!))
            for (l in 1..5) append(',').append(round2(persist[l]!!))
            append('\n')
            println("  yield=${"%.2f".format(y)}  L3/L4/L5 survival=${round2(surv[3]!!)}/${round2(surv[4]!!)}/${round2(surv[5]!!)}  L5 persist=${round2(persist[5]!!)}")
        }
    }.let { File(outDir, "exp1_yield.csv").writeText(it) }

    // Experiment 2 — effect of the optional behaviours (herding × gradient foraging).
    println("\n[exp2] survival vs behaviours (herding × gradient)")
    buildString {
        append("herding,gradient,L3_surv,L4_surv,L5_surv,L3_persist,L4_persist,L5_persist\n")
        for (herding in listOf(false, true)) for (gradient in listOf(false, true)) {
            val results = seeds.map { seed ->
                runOnce(SimulationConfig.default(WIDTH, HEIGHT, seed = seed, herding = herding, gradientForaging = gradient), ticks)
            }
            val (surv, persist) = aggregate(results)
            append("$herding,$gradient")
            for (l in 3..5) append(',').append(round2(surv[l]!!))
            for (l in 3..5) append(',').append(round2(persist[l]!!))
            append('\n')
            println("  herding=$herding gradient=$gradient  L4 persist=${round2(persist[4]!!)}  L5 persist=${round2(persist[5]!!)}")
        }
    }.let { File(outDir, "exp2_behaviours.csv").writeText(it) }

    // Experiment 3 — effect of a global metabolism multiplier on the upper levels.
    println("\n[exp3] survival vs metabolism multiplier")
    val metabolisms = listOf(0.6, 0.8, 1.0, 1.2, 1.4)
    buildString {
        append("metabolism,L3_surv,L4_surv,L5_surv,L3_persist,L4_persist,L5_persist\n")
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
            val (surv, persist) = aggregate(results)
            append(m)
            for (l in 3..5) append(',').append(round2(surv[l]!!))
            for (l in 3..5) append(',').append(round2(persist[l]!!))
            append('\n')
            println("  metabolism=$m  L4 persist=${round2(persist[4]!!)}  L5 persist=${round2(persist[5]!!)}")
        }
    }.let { File(outDir, "exp3_metabolism.csv").writeText(it) }

    val secs = (System.currentTimeMillis() - startedAt) / 1000.0
    println("\nDone in ${"%.1f".format(secs)}s. CSVs written to ${outDir.absolutePath}")
}
