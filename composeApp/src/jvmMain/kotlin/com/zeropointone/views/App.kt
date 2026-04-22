package com.zeropointone.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zeropointone.engine.Engine
import com.zeropointone.engine.Terrain

@Composable
@Preview
fun App() {
    var engine: Engine? by remember { mutableStateOf(null) }

    MaterialTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Sidebar(
                onSubmit = { options ->
                    engine?.destroy()
                    engine = Engine(Terrain(options.pixelWidth, options.pixelHeight)).apply { run() }
                },
                onReset = {
                    engine?.destroy()
                    engine = null
                }
            )
            ScrollableArea {
                engine?.let { activeEngine ->
                    val terrain = activeEngine.terrain

                    val hexagons = terrain.getHexagons()
                    val agents by remember(activeEngine) {
                        derivedStateOf { terrain.getAgents() }
                    }

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.inversePrimary)
                            .size(width = terrain.pixelWidth.dp, height = terrain.pixelHeight.dp)
                    ) {
                        HexagonGrid(hexagons)
                        AgentsView(agents)
                    }
                }
            }
        }
    }
}