package com.zeropointone.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Options(val pixelWidth: Int, val pixelHeight: Int, val speed: Float)

@Composable
fun Sidebar(onSubmit: (options: Options) -> Unit, onReset: () -> Unit) {
    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf(0f) }

    Column (Modifier.width(240.dp).padding(8.dp)) {
        Text(text = "Size")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                modifier = Modifier.weight(1f),
                value = width,
                onValueChange = { value -> if (value.all { it.isDigit() }) { width = value } },
                label = { Text("Width") }
            )
            TextField(
                modifier = Modifier.weight(1f),
                value = height,
                onValueChange = { value -> if (value.all { it.isDigit() }) { height = value } },
                label = { Text("Height") }
            )
        }
        Divider(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Text(text = "Speed")
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = speed,
            onValueChange = { speed = it}
        )
        Divider(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Text(text = "Controls")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSubmit(Options(width.toInt(), height.toInt(), speed)) }) {
                Text("Run")
            }
            Button( onClick = onReset) {
                Text("Reset")
            }
        }
    }
}