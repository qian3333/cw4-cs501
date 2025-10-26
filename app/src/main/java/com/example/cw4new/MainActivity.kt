package com.example.cw4new

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cw4new.ui.theme.Cw4newTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import android.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview


data class TemperatureReading(
    val value: Float,
    val timestamp: Long = System.currentTimeMillis()
)


data class TemperatureState(
    val readings: List<TemperatureReading> = emptyList(),
    val isRunning: Boolean = true
)


class TemperatureViewModel : ViewModel() {
    private val _state = MutableStateFlow(TemperatureState())
    // UI collects this immutable snapshot.
    val state: StateFlow<TemperatureState> = _state.asStateFlow()

    init {
        // Start the generator as soon as the VM spins up.
        generatingData()
    }

    private fun generatingData() {
        // Tick every two seconds and append the latest reading.
        viewModelScope.launch {
            while (true) {
                delay(2000)
                _state.update { currentState ->
                    if (currentState.isRunning) {
                        // Generate random temperature
                        val newReading = TemperatureReading(
                            value = 65 + Random.nextFloat() * 20
                        )

                        val newReadings = (currentState.readings + newReading).takeLast(20)
                        currentState.copy(readings = newReadings)
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    fun toggleRunning() {
        _state.update { it.copy(isRunning = !it.isRunning) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cw4newTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Dashboard(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Dashboard(
    modifier: Modifier = Modifier,
    viewModel: TemperatureViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    // Copy out for readability below.
    val readings = state.readings

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Temperature Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = { viewModel.toggleRunning() }) {
                Text(if (state.isRunning) "Pause" else "Resume")
            }
        }


        if (readings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Summary Statistics", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Simple inline stats for the snapshot.
                        val current = readings.last().value
                        val average = readings.map { it.value }.average()
                        val min = readings.minOf { it.value }
                        val max = readings.maxOf { it.value }

                        StatItem("Current", "%.1f°F".format(current))
                        StatItem("Average", "%.1f°F".format(average))
                        StatItem("Min", "%.1f°F".format(min))
                        StatItem("Max", "%.1f°F".format(max))
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Temperature Trend",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    if (readings.size >= 2) {
                        Chart(
                            readings = readings,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            "Need more data to display chart",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    }
                }
            }
        }


        Text(
            "Recent Readings (${readings.size}/20)",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(readings.reversed()) { reading ->
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val timeString = timeFormat.format(Date(reading.timestamp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time stamp left, value right keeps rows scannable.
                    Text(timeString)
                    Text("%.1f°F".format(reading.value))
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun Chart(
    readings: List<TemperatureReading>,
    modifier: Modifier = Modifier
) {
    // Use theme color once outside the draw block.
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20f

        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        val minTemp = 64f
        val maxTemp = 86f
        val tempRange = maxTemp - minTemp

        val xStep = chartWidth / (readings.size - 1)

        val path = Path()
        readings.forEachIndexed { index, reading ->

            val x = padding + index * xStep

            val y = padding + chartHeight - ((reading.value - minTemp) / tempRange * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3f)
        )


        readings.forEachIndexed { index, reading ->
            val x = padding + index * xStep
            val y = padding + chartHeight - ((reading.value - minTemp) / tempRange * chartHeight)

            drawCircle(
                color = primaryColor,
                radius = 4f,
                center = Offset(x, y)
            )
        }


        drawLine(
            start = Offset(padding, padding),
            end = Offset(padding, padding + chartHeight),
            color = Color.LightGray,
            strokeWidth = 1f
        )
        drawLine(
            start = Offset(padding, padding + chartHeight),
            end = Offset(padding + chartWidth, padding + chartHeight),
            color = Color.LightGray,
            strokeWidth = 1f
        )


        val textPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 28f
            isAntiAlias = true
        }

        listOf(65f, 75f, 85f).forEach { temp ->
            val y = padding + chartHeight - ((temp - minTemp) / tempRange * chartHeight)
            drawLine(
                start = Offset(padding - 5f, y),
                end = Offset(padding, y),
                color = Color.LightGray,
                strokeWidth = 1f
            )

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "${temp.toInt()}°",
                    0f,
                    y,
                    textPaint
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    Cw4newTheme {
        Dashboard()
    }
}