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
}
