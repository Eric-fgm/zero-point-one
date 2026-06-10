package com.zeropointone.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeropointone.engine.PopulationSample
import kotlin.math.ln

/**
 * Line chart of per-level population over time. Uses a logarithmic y-axis because the trophic
 * pyramid spans orders of magnitude (hundreds of producers vs. single-digit apex predators) —
 * on a linear axis the upper levels would be an invisible flat line along the bottom.
 */
@Composable
fun PopulationChart(history: List<PopulationSample>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Population over time (log scale)", color = Color.White, fontSize = 12.sp)
            for (level in 1..5) Text("L$level", color = levelColor(level), fontSize = 11.sp)
        }
        Canvas(Modifier.fillMaxWidth().fillMaxSize()) {
            if (history.size < 2) return@Canvas

            val peak = history.maxOf { sample -> (1..5).maxOf { sample.counts[it] ?: 0 } }.coerceAtLeast(1)
            val logPeak = ln((peak + 1).toDouble())
            val w = size.width
            val h = size.height
            val lastIndex = history.size - 1

            fun yFor(count: Int): Float {
                val norm = (ln((count + 1).toDouble()) / logPeak).toFloat()
                return h - norm * h
            }

            for (level in 1..5) {
                val path = Path()
                history.forEachIndexed { i, sample ->
                    val x = w * i / lastIndex
                    val y = yFor(sample.counts[level] ?: 0)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = levelColor(level), style = Stroke(width = 2f))
            }
        }
    }
}
