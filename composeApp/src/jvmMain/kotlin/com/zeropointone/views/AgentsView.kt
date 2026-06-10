package com.zeropointone.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeropointone.engine.AgentSnapshot

private val LevelColors = mapOf(
    1 to Color(0xFF4CAF50), // producer  - green
    2 to Color(0xFFFFEB3B), // herbivore - yellow
    3 to Color(0xFFFF9800), // small carnivore - orange
    4 to Color(0xFFF44336), // large carnivore - red
    5 to Color(0xFF9C27B0), // apex predator - purple
)

private val LevelLabels = mapOf(
    1 to "L1 Producers",
    2 to "L2 Herbivores",
    3 to "L3 Small carniv.",
    4 to "L4 Large carniv.",
    5 to "L5 Apex",
)

private fun levelColor(level: Int): Color = LevelColors[level] ?: Color.Gray
private fun levelSizeDp(level: Int): Int = 4 + level * 2

@Composable
fun AgentsView(agents: List<AgentSnapshot>) {
    Box(Modifier.fillMaxSize()) {
        agents.forEach { agent ->
            val diameter = levelSizeDp(agent.level)
            // Dimmer when low on energy, brighter when well-fed.
            val color = levelColor(agent.level).copy(alpha = 0.45f + 0.55f * agent.energyRatio)
            Box(
                Modifier
                    .offset(x = agent.x.dp - (diameter / 2).dp, y = agent.y.dp - (diameter / 2).dp)
                    .size(diameter.dp)
                    .background(color, shape = CircleShape)
            )
        }
    }
}

/** Compact live readout of per-level populations and the current tick. */
@Composable
fun PopulationOverlay(populations: Map<Int, Int>, tick: Long) {
    Column(
        Modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text("tick $tick", color = Color.White, fontSize = 11.sp)
        for (level in 1..5) {
            Text(
                text = "${LevelLabels[level]}: ${populations[level] ?: 0}",
                color = levelColor(level),
                fontSize = 12.sp,
            )
        }
    }
}
