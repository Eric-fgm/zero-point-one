package com.zeropointone.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeropointone.engine.Agent

@Composable
fun AgentsView(agents: List<Agent>) {
    Box(Modifier.fillMaxSize()) {
        agents.forEach { agent ->
            Box(
                Modifier
                    .offset(x = agent.position.x.dp - 3.dp, y = agent.position.y.dp - 3.dp)
                    .size(6.dp)
                    .background(Color.Red, shape = CircleShape)
            ) {
                Text(text="" + agent.position.x + ", " + agent.position.y, fontSize=8.sp)
            }
        }
    }
}