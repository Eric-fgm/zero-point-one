package com.zeropointone.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zeropointone.engine.Engine
import com.zeropointone.engine.Terrain
import java.io.File

@Composable
@Preview
fun App() {
    var engine: Engine? by remember { mutableStateOf(null) }
    var isPaused by remember { mutableStateOf(false) }
    var exportStatus: String? by remember { mutableStateOf(null) }

    MaterialTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Sidebar(
                isRunning = engine != null,
                isPaused = isPaused,
                exportStatus = exportStatus,
                onSubmit = { options ->
                    engine?.destroy()
                    isPaused = false
                    exportStatus = null
                    val terrain = Terrain(options.pixelWidth, options.pixelHeight)
                    engine = Engine(terrain, options.toConfig()).apply { run() }
                },
                onReset = {
                    engine?.destroy()
                    engine = null
                    isPaused = false
                    exportStatus = null
                },
                onSpeedChange = { engine?.speed = it },
                onTogglePause = {
                    isPaused = !isPaused
                    engine?.paused = isPaused
                },
                onExportCsv = {
                    engine?.let { eng ->
                        val file = File("foodchain_populations_tick${eng.snapshot.value.tick}.csv").absoluteFile
                        file.writeText(eng.populationHistoryCsv())
                        exportStatus = "Saved: ${file.path}"
                    }
                },
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                engine?.let { activeEngine ->
                    val terrain = activeEngine.terrain
                    val snapshot by activeEngine.snapshot.collectAsState()

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        ScrollableArea {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.inversePrimary)
                                    .size(width = terrain.pixelWidth.dp, height = terrain.pixelHeight.dp),
                            ) {
                                HexagonGrid(terrain.hexagons)
                                AgentsView(snapshot.agents)
                                PopulationOverlay(snapshot.populations, snapshot.tick)
                            }
                        }
                    }
                    PopulationChart(
                        history = snapshot.history,
                        modifier = Modifier.fillMaxWidth().height(170.dp).padding(8.dp),
                    )
                }
            }
        }
    }
}
