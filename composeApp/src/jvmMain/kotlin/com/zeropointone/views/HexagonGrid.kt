package com.zeropointone.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zeropointone.utils.Hexagon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HexagonGrid(hexagons: List<Hexagon>) {
    Box(Modifier.fillMaxSize()) {
        hexagons.forEach { hexagon ->
            val center = Hexagon.toPosition(hexagon)
            val size = Hexagon.SIZE

            Box(
                modifier = Modifier
                    .size((size * 2).dp)
                    .offset(x=(center.x - size).dp, y=(center.y - size).dp)
                    .clip(HexagonShape)
                    .background(Color.DarkGray.copy(alpha = 0.1f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), HexagonShape)
            )
        }
    }
}

val HexagonShape = GenericShape  { size, _ ->
    val radius = size.width / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    for (i in 0..5) {
        val angleRad = Math.toRadians((60.0 * i - 30.0))
        val x = centerX + radius * cos(angleRad).toFloat()
        val y = centerY + radius * sin(angleRad).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}