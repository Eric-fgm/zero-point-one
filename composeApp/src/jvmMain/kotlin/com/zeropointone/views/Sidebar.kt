package com.zeropointone.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeropointone.engine.SimulationConfig

/** User-configurable options collected from the sidebar, turned into a [SimulationConfig] on Run. */
data class Options(
    val pixelWidth: Int,
    val pixelHeight: Int,
    val speed: Float,
    val seed: Long,
    val initialPopulations: Map<Int, Int>,
    /** null = derive automatically from the world area. */
    val carryingCapacity: Int?,
    /** Scales every consumer's metabolic rate (lower = easier survival). */
    val metabolicMultiplier: Float,
    /** Fraction of prey energy gained on consumption — the "10% rule" as a tunable. */
    val energyTransferYield: Double,
    val herding: Boolean,
    val gradientForaging: Boolean,
)

/** Builds a full simulation config from the chosen options, keeping the default species params. */
fun Options.toConfig(): SimulationConfig {
    val base = SimulationConfig.default(
        pixelWidth, pixelHeight, speed, seed, energyTransferYield, herding, gradientForaging,
    )
    return base.copy(
        initialPopulations = initialPopulations,
        producerCarryingCapacity = carryingCapacity ?: base.producerCarryingCapacity,
        params = base.params.mapValues { (level, p) ->
            if (level == 1) p else p.copy(metabolicRate = p.metabolicRate * metabolicMultiplier)
        },
    )
}

@Composable
fun Sidebar(
    isRunning: Boolean,
    isPaused: Boolean,
    exportStatus: String?,
    onSubmit: (Options) -> Unit,
    onReset: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTogglePause: () -> Unit,
    onExportCsv: () -> Unit,
) {
    var width by remember { mutableStateOf("800") }
    var height by remember { mutableStateOf("600") }
    var seed by remember { mutableStateOf("42") }
    var pop1 by remember { mutableStateOf("80") }
    var pop2 by remember { mutableStateOf("45") }
    var pop3 by remember { mutableStateOf("20") }
    var pop4 by remember { mutableStateOf("9") }
    var pop5 by remember { mutableStateOf("4") }
    var carrying by remember { mutableStateOf("") } // blank = auto
    var metabolism by remember { mutableStateOf(1f) }
    var transfer by remember { mutableStateOf(0.10f) } // the 10% rule, tunable
    var herding by remember { mutableStateOf(true) }
    var gradientForaging by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(0.3f) }

    Column(
        modifier = Modifier
            .width(264.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("World size")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Width", width, { width = it }, Modifier.weight(1f))
            NumberField("Height", height, { height = it }, Modifier.weight(1f))
        }
        NumberField("Seed", seed, { seed = it }, Modifier.fillMaxWidth())

        Divider(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Text("Initial populations")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberField("L1", pop1, { pop1 = it }, Modifier.weight(1f))
            NumberField("L2", pop2, { pop2 = it }, Modifier.weight(1f))
            NumberField("L3", pop3, { pop3 = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberField("L4", pop4, { pop4 = it }, Modifier.weight(1f))
            NumberField("L5", pop5, { pop5 = it }, Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
        NumberField("Grass capacity (blank = auto)", carrying, { carrying = it }, Modifier.fillMaxWidth())

        Divider(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Text("Energy transfer  ${(transfer * 100).toInt()}%  (the 10% rule)", fontSize = 13.sp)
        Slider(value = transfer, onValueChange = { transfer = it }, valueRange = 0.05f..0.40f)

        Text("Metabolism  ×${formatTwo(metabolism)}", fontSize = 13.sp)
        Slider(value = metabolism, onValueChange = { metabolism = it }, valueRange = 0.5f..1.5f)

        Divider(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Text("Behaviour", fontSize = 13.sp)
        ToggleRow("Herding", herding) { herding = it }
        ToggleRow("Gradient foraging", gradientForaging) { gradientForaging = it }

        Text("Speed", fontSize = 13.sp)
        Slider(
            value = speed,
            onValueChange = { speed = it; onSpeedChange(it) },
        )

        Divider(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSubmit(
                    Options(
                        pixelWidth = width.toIntOrNull() ?: 800,
                        pixelHeight = height.toIntOrNull() ?: 600,
                        speed = speed,
                        seed = seed.toLongOrNull() ?: 42L,
                        initialPopulations = mapOf(
                            1 to (pop1.toIntOrNull() ?: 0),
                            2 to (pop2.toIntOrNull() ?: 0),
                            3 to (pop3.toIntOrNull() ?: 0),
                            4 to (pop4.toIntOrNull() ?: 0),
                            5 to (pop5.toIntOrNull() ?: 0),
                        ),
                        carryingCapacity = carrying.toIntOrNull(),
                        metabolicMultiplier = metabolism,
                        energyTransferYield = transfer.toDouble(),
                        herding = herding,
                        gradientForaging = gradientForaging,
                    )
                )
            },
        ) {
            Text(if (isRunning) "Restart" else "Run")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTogglePause, enabled = isRunning, modifier = Modifier.weight(1f)) {
                Text(if (isPaused) "Resume" else "Pause")
            }
            Button(onClick = onReset, enabled = isRunning, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
        }
        Button(onClick = onExportCsv, enabled = isRunning, modifier = Modifier.fillMaxWidth()) {
            Text("Export CSV")
        }
        exportStatus?.let { Text(it, fontSize = 10.sp) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        modifier = modifier,
        value = value,
        onValueChange = { v -> if (v.all { it.isDigit() }) onChange(v) },
        label = { Text(label) },
        singleLine = true,
    )
}

private fun formatTwo(value: Float): String {
    val scaled = (value * 100).toInt()
    return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
}
