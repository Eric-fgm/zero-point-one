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
        Text("tick $tick   •   total ${populations.values.sum()}", color = Color.White, fontSize = 11.sp)
        for (level in 1..5) {
            Text(
                text = "${LevelLabels[level]}: ${populations[level] ?: 0}",
                color = levelColor(level),
                fontSize = 12.sp,
            )
        }
    }
}
