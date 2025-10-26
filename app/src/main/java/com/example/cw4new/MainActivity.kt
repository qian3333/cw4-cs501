package com.example.cw4new

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cw4new.ui.theme.Cw4newTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Row

data class LifecycleEvent(
    val eventName: String,
    val timestamp: Long,
    val color: Color
)


class LifeTrackerViewModel : ViewModel() {
    private val _events = MutableLiveData<List<LifecycleEvent>>(emptyList())
    val events: LiveData<List<LifecycleEvent>> = _events


    private val mutableLiveData = MutableLiveData(true)
    val showNotifications: LiveData<Boolean> = mutableLiveData


    fun toggleNotifications(enabled: Boolean) {
        mutableLiveData.value = enabled
    }

    fun addEvent(event: LifecycleEvent) {
        // Keep newest events at the top of the list.
        _events.value = listOf(event) + (_events.value ?: emptyList())
    }
}

class LifeCycleLogger(
    private val viewModel: LifeTrackerViewModel
) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        // Map lifecycle events to friendly labels and colors.
        val eventName = when (event) {
            Lifecycle.Event.ON_CREATE -> "ON_CREATE"
            Lifecycle.Event.ON_START -> "ON_START"
            Lifecycle.Event.ON_RESUME -> "ON_RESUME"
            Lifecycle.Event.ON_PAUSE -> "ON_PAUSE"
            Lifecycle.Event.ON_STOP -> "ON_STOP"
            Lifecycle.Event.ON_DESTROY -> "ON_DESTROY"
            Lifecycle.Event.ON_ANY -> "ON_ANY"
            else -> event.name
        }

        val color = when (event) {
            Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_START -> Color(0xFF4CAF50)
            Lifecycle.Event.ON_RESUME -> Color(0xFF2196F3)
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> Color(0xFFFFC107)
            Lifecycle.Event.ON_DESTROY -> Color(0xFFF44336)
            else -> Color(0xFF9E9E9E)
        }

        viewModel.addEvent(
            LifecycleEvent(
                eventName = eventName,
                timestamp = System.currentTimeMillis(),
                color = color
            )
        )
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var lifecycleLogger: LifeCycleLogger

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = androidx.lifecycle.ViewModelProvider(this)[LifeTrackerViewModel::class.java]
        lifecycleLogger = LifeCycleLogger(viewModel)


        // Begin logging as soon as the activity is created.
        lifecycle.addObserver(lifecycleLogger)

        enableEdgeToEdge()
        setContent {
            Cw4newTheme {
                val hostState = remember { SnackbarHostState() }
                val events by viewModel.events.observeAsState(emptyList())
                val notifications by viewModel.showNotifications.observeAsState(true)
                var dialog by remember { mutableStateOf(false) }

                LaunchedEffect(events.size) {
                    // Surface the latest event when notifications are enabled.
                    if (events.isNotEmpty() && notifications) {
                        hostState.showSnackbar(
                            message = "Lifecycle event: ${events.first().eventName}",
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("LifeTracker") },
                            actions = {
                                IconButton(onClick = { dialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState) }
                ) { innerPadding ->
                    if (events.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No lifecycle events yet",
                                color = Color.Gray,
                                fontSize = 18.sp
                            )
                        }
                    } else {

                        val currentState = events.first().eventName
                        Text(
                            text = "Current State: $currentState",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(top = 8.dp, start = 16.dp)
                        )


                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(top = 40.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(events) { event ->
                                val formattedTime = SimpleDateFormat(
                                    "HH:mm:ss.SSS",
                                    Locale.getDefault()
                                ).format(Date(event.timestamp))

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxSize()
                                ) {
                                    // Show each log line with the event color.
                                    Text(
                                        text = "$formattedTime - ${event.eventName}",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 8.dp)
                                            .padding(start = 16.dp),
                                        color = event.color
                                    )
                                }
                            }
                        }
                    }


                    if (dialog) {
                        Dialog(
                            showNotifications = notifications,
                            onToggleNotifications = { viewModel.toggleNotifications(it) },
                            onDismiss = { dialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(lifecycleLogger)
    }
}


@Composable
fun Dialog(
    showNotifications: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show transition notifications")
                    androidx.compose.material3.Switch(
                        checked = showNotifications,
                        onCheckedChange = onToggleNotifications
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TrackerPreview() {
    Cw4newTheme {
        val viewModel = LifeTrackerViewModel()
        // Seed predictable events so the preview has content.
        viewModel.addEvent(LifecycleEvent("ON_CREATE", System.currentTimeMillis(), Color.Green))
        viewModel.addEvent(LifecycleEvent("ON_START", System.currentTimeMillis() - 1000, Color(0xFF4CAF50)))
        viewModel.addEvent(LifecycleEvent("ON_RESUME", System.currentTimeMillis() - 2000, Color.Blue))

        val events by viewModel.events.observeAsState(emptyList())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("LifeTracker") },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Text(
                text = "Current State: ${events.firstOrNull()?.eventName ?: "None"}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(top = 8.dp, start = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    val formattedTime = SimpleDateFormat(
                        "HH:mm:ss.SSS",
                        Locale.getDefault()
                    ).format(Date(event.timestamp))

                    // Mirror the main list so the preview matches runtime UI.
                    Text(
                        text = "$formattedTime - ${event.eventName}",
                        modifier = Modifier.padding(16.dp),
                        color = event.color
                    )
                }
            }
        }
    }
}