package com.example.cw4new

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.cw4new.ui.theme.Cw4newTheme

sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
    object ToggleAutoMode : CounterAction()
    data class SetInterval(val interval: Int) : CounterAction()
}


enum class Screen {
    Counter,
    Settings
}


class CounterViewModel : ViewModel() {

    private val _count = MutableStateFlow(0)
    private val _isAutoMode = MutableStateFlow(false)
    private val _autoIncrementInterval = MutableStateFlow(3)

    val count: StateFlow<Int> = _count.asStateFlow()
    val isAutoMode: StateFlow<Boolean> = _isAutoMode.asStateFlow()
    val autoIncrementInterval: StateFlow<Int> = _autoIncrementInterval.asStateFlow()


    private var autoIncrementJob: Job? = null


    fun handleAction(action: CounterAction) {
        when (action) {
            CounterAction.Increment -> _count.value++
            CounterAction.Decrement -> _count.value--
            CounterAction.Reset -> _count.value = 0
            CounterAction.ToggleAutoMode -> {
                _isAutoMode.value = !_isAutoMode.value
                if (_isAutoMode.value) {
                    startAutoIncrement()
                } else {
                    stopAutoIncrement()
                }
            }
            is CounterAction.SetInterval -> {
                val newInterval = action.interval.coerceAtLeast(1)
                _autoIncrementInterval.value = newInterval

                if (_isAutoMode.value) {
                    startAutoIncrement()
                }
            }
        }
    }


    private fun startAutoIncrement() {
        autoIncrementJob?.cancel()
        autoIncrementJob = viewModelScope.launch {
            while (true) {
                delay(_autoIncrementInterval.value * 1000L)
                _count.value++
            }
        }
    }


    private fun stopAutoIncrement() {
        autoIncrementJob?.cancel()
        autoIncrementJob = null
    }


    override fun onCleared() {
        super.onCleared()
        stopAutoIncrement()
    }
}


@Composable
fun AppNavigation(
    viewModel: CounterViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Counter.name,
        modifier = modifier
    ) {
        composable(Screen.Counter.name) {
            CounterScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.name)
                }
            )
        }
        composable(Screen.Settings.name) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}


@Composable
fun CounterScreen(
    viewModel: CounterViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {

    val count by viewModel.count.collectAsStateWithLifecycle()
    val isAutoMode by viewModel.isAutoMode.collectAsStateWithLifecycle()
    val interval by viewModel.autoIncrementInterval.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Count: $count",
            fontSize = 48.sp,
            modifier = Modifier.padding(24.dp)
        )


        Text(
            text = "Auto mode: ${if (isAutoMode) "ON" else "OFF"}",
            fontSize = 18.sp,
            modifier = Modifier.padding(16.dp)
        )


        Text(
            text = "Interval: ${interval}s",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.handleAction(CounterAction.Decrement) },
                modifier = Modifier.size(80.dp, 80.dp)
            ) {
                Text(text = "-1", fontSize = 24.sp)
            }

            Button(
                onClick = { viewModel.handleAction(CounterAction.Reset) },
                modifier = Modifier.size(80.dp, 80.dp)
            ) {
                Text(text = "Reset", fontSize = 18.sp)
            }

            Button(
                onClick = { viewModel.handleAction(CounterAction.Increment) },
                modifier = Modifier.size(80.dp, 80.dp)
            ) {
                Text(text = "+1", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.size(32.dp))


        Button(
            onClick = { viewModel.handleAction(CounterAction.ToggleAutoMode) },
            modifier = Modifier.size(200.dp, 60.dp)
        ) {
            Text(
                text = if (isAutoMode) "Turn OFF Auto" else "Turn ON Auto",
                fontSize = 18.sp
            )
        }


        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}


@Composable
fun SettingsScreen(
    viewModel: CounterViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interval by viewModel.autoIncrementInterval.collectAsStateWithLifecycle()
    var intervalText by remember { mutableStateOf(TextFieldValue(interval.toString())) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            modifier = Modifier.padding(24.dp)
        )

        Text(
            text = "Auto-increment Interval (seconds):",
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                onClick = {
                    val newInterval = (intervalText.text.toIntOrNull() ?: 1) - 1
                    intervalText = TextFieldValue(newInterval.coerceAtLeast(1).toString())
                },
                modifier = Modifier.size(50.dp, 50.dp)
            ) {
                Text(text = "-")
            }

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = intervalText.text,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))


            Button(
                onClick = {
                    val newInterval = (intervalText.text.toIntOrNull() ?: 1) + 1
                    intervalText = TextFieldValue(newInterval.toString())
                },
                modifier = Modifier.size(50.dp, 50.dp)
            ) {
                Text(text = "+")
            }
        }

        Spacer(modifier = Modifier.size(32.dp))


        Button(
            onClick = {
                val newInterval = intervalText.text.toIntOrNull() ?: 3
                viewModel.handleAction(CounterAction.SetInterval(newInterval))
                onNavigateBack()
            },
            modifier = Modifier.size(150.dp, 50.dp)
        ) {
            Text(text = "Save", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.size(16.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.size(150.dp, 50.dp)
        ) {
            Text(text = "Back", fontSize = 18.sp)
        }
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cw4newTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: CounterViewModel = viewModel()

                    AppNavigation(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CounterPreview() {
    Cw4newTheme {
        val viewModel: CounterViewModel = viewModel()
        CounterScreen(viewModel = viewModel, onNavigateToSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    Cw4newTheme {
        val viewModel: CounterViewModel = viewModel()
        SettingsScreen(viewModel = viewModel, onNavigateBack = {})
    }
}