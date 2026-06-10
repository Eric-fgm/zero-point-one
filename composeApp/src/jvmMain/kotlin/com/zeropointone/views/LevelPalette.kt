package com.zeropointone.views

import androidx.compose.ui.graphics.Color

/** Shared colour/label palette for the five trophic levels, used by the grid view and the chart. */
val LevelColors = mapOf(
    1 to Color(0xFF4CAF50), // producer  - green
    2 to Color(0xFFFFEB3B), // herbivore - yellow
    3 to Color(0xFFFF9800), // small carnivore - orange
    4 to Color(0xFFF44336), // large carnivore - red
    5 to Color(0xFF9C27B0), // apex predator - purple
)

val LevelLabels = mapOf(
    1 to "L1 Producers",
    2 to "L2 Herbivores",
    3 to "L3 Small carniv.",
    4 to "L4 Large carniv.",
    5 to "L5 Apex",
)

fun levelColor(level: Int): Color = LevelColors[level] ?: Color.Gray
