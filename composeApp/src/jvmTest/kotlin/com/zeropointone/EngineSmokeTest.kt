package com.zeropointone

import com.zeropointone.engine.Engine
import com.zeropointone.engine.SimulationConfig
import com.zeropointone.engine.Terrain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineSmokeTest {

    @Test
    fun seedsAllFiveTrophicLevels() {
        val config = SimulationConfig.default(width = 600, height = 400, speed = 1f, seed = 7L)
        val engine = Engine(Terrain(600, 400), config)

        engine.initialize()
        val start = engine.snapshot.value

        assertTrue(start.agents.isNotEmpty(), "expected agents after seeding")
        for (level in 1..5) {
            assertTrue((start.populations[level] ?: 0) > 0, "level $level should be seeded: ${start.populations}")
        }
        engine.destroy()
    }

    @Test
    fun simulationAdvancesDeterministicallyAndEvolves() {
        val config = SimulationConfig.default(width = 600, height = 400, speed = 1f, seed = 7L)
        val engine = Engine(Terrain(600, 400), config)

        engine.initialize()
        val start = engine.snapshot.value

        var last = start
        repeat(300) { last = engine.advance() } // must not throw

        assertEquals(300L, last.tick, "tick should advance one per call")
        assertTrue(last.populations != start.populations, "populations should fluctuate over time: $start -> $last")
        engine.destroy()
    }

    @Test
    fun recordsHistoryAndExportsCsv() {
        val config = SimulationConfig.default(width = 400, height = 300, speed = 1f, seed = 3L)
        val engine = Engine(Terrain(400, 300), config)

        engine.initialize()
        repeat(50) { engine.advance() }

        val snap = engine.snapshot.value
        assertEquals(50L, snap.tick)
        assertTrue(snap.history.isNotEmpty(), "snapshot should carry chart history")
        assertEquals(50L, snap.history.last().tick, "history should end at the current tick")

        val lines = engine.populationHistoryCsv().trim().lines()
        assertEquals("tick,L1,L2,L3,L4,L5,total", lines.first())
        assertEquals(52, lines.size, "header + ticks 0..50 = 52 lines")
        engine.destroy()
    }
}
