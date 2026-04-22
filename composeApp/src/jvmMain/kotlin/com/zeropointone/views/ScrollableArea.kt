package com.zeropointone.views

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScrollableArea(content: @Composable () -> Unit) {
    Box(
        Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
    ) {
        content()
    }
}