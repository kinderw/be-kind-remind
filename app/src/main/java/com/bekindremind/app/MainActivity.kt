package com.bekindremind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bekindremind.app.data.TaskMode
import com.bekindremind.app.data.TaskStatus
import com.bekindremind.app.data.TrafficModel
import com.bekindremind.app.data.TripTaskEntity
import com.bekindremind.app.permissions.ExactAlarmPermissionManager
import com.bekindremind.app.ui.ExactAlarmBlockerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var exactAlarmPermissionManager: ExactAlarmPermissionManager
    private var canScheduleExactAlarms by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        exactAlarmPermissionManager = ExactAlarmPermissionManager(this)
        canScheduleExactAlarms = exactAlarmPermissionManager.canScheduleExactAlarms()

        val appGraph = (application as BeKindRemindApp).appGraph

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (canScheduleExactAlarms) {
                        HomeScreen(
                            onCreateLeaveAtDemo = {
                                appGraph.schedulingEngine.scheduleTask(createDemoTask(TaskMode.LEAVE_AT))
                            },
                            onCreateArriveByDemo = {
                                appGraph.schedulingEngine.scheduleTask(createDemoTask(TaskMode.ARRIVE_BY))
                            }
                        )
                    } else {
                        ExactAlarmBlockerScreen(
                            onOpenSettings = {
                                startActivity(exactAlarmPermissionManager.createPermissionIntent())
                            },
                            onRefresh = {
                                canScheduleExactAlarms = exactAlarmPermissionManager.canScheduleExactAlarms()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::exactAlarmPermissionManager.isInitialized) {
            canScheduleExactAlarms = exactAlarmPermissionManager.canScheduleExactAlarms()
        }
    }

    private fun createDemoTask(mode: TaskMode): TripTaskEntity {
        val now = System.currentTimeMillis()
        val targetTime = now + 2 * 60 * 60 * 1000L
        return TripTaskEntity(
            mode = mode,
            title = if (mode == TaskMode.ARRIVE_BY) "Demo Arrive-By" else "Demo Leave-At",
            originLat = 37.7749,
            originLng = -122.4194,
            originLabel = "San Francisco",
            destinationPlaceId = null,
            destinationLat = 37.7849,
            destinationLng = -122.4094,
            timeUtc = targetTime,
            timeZoneId = java.util.TimeZone.getDefault().id,
            reminderOffsetsMinCsv = "60,30,15,5",
            bufferMin = if (mode == TaskMode.ARRIVE_BY) 10 else 0,
            trafficModel = TrafficModel.BEST_GUESS,
            lastKnownEtaMin = 25,
            status = TaskStatus.SCHEDULED,
            createdAtUtc = now,
            updatedAtUtc = now
        )
    }
}

@Composable
private fun HomeScreen(
    onCreateLeaveAtDemo: suspend () -> Unit,
    onCreateArriveByDemo: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by mutableStateOf("No demo task scheduled yet")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(text = stringResource(id = R.string.app_subtitle), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            scope.launch {
                runCatching { onCreateLeaveAtDemo() }
                    .onSuccess { status = "Leave-At demo scheduled" }
                    .onFailure { status = "Failed: ${it.message}" }
            }
        }) {
            Text("Schedule Leave-At Demo")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                runCatching { onCreateArriveByDemo() }
                    .onSuccess { status = "Arrive-By demo scheduled" }
                    .onFailure { status = "Failed: ${it.message}" }
            }
        }) {
            Text("Schedule Arrive-By Demo")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = status, style = MaterialTheme.typography.bodyMedium)
    }
}
